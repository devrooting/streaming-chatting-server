package netty;

import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class WebSocketHandler extends ChannelInboundHandlerAdapter {

    private static final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    //채팅방 해쉬맵
    private static final Map<String, Chatroom> chattingRooms = new HashMap<>();

    //채팅방 스트리머PK 와 일치하는 channel 을 찾아주는 해쉬맵
    private static final Map<Channel, String> match = new HashMap<>();

    //사용자가 어느 채팅방에 있는지 알려주는 해쉬맵
    private static final Map<Channel, String> client_chatroom = new HashMap<>();

    static final String JDBC_DRIVER = "org.mariadb.jdbc.Driver";
    static final String DB_URL = "jdbc:mariadb://localhost:3306/DB명";
    static final String USERNAME = "계정이름";
    static final String PASSWORD = "비밀번호";

    Connection conn = null;
    Statement stmt = null;

    ChannelFuture cf;
    EventLoopGroup group;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("===== [WebSocketHandler] handlerAdded() =====");
    }


    // 채널 입출력 준비 완료
    // 연결 직후 한번 수행하는 작업에 유용
    // 채널 입출력 준비 완료 사용자가 들어왔을때.
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("===== [WebSocketHandler] channelActive() =====");
        System.out.println("User Access!");

        //채널 입출력 준비 완료 사용자가 들어왔을때.
        Channel ch = ctx.channel();
        for (Channel channel : channelGroup) {
            channel.write("[SERVER] -" + ch.remoteAddress() + "들어왔음!\n");
        }
        channelGroup.add(ch);

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("===== [WebSocketHandler] channelInactive() =====");

        //사용자가 나갔을 때 기존 사용자에게 알림.
        Channel ch = ctx.channel();
        for (Channel channel : channelGroup) {
            channel.write("[SERVER] - " + ch.remoteAddress() + " 나갔음!\n");
        }
        channelGroup.remove(ch);
    }


    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("===== [WebSocketHandler] handlerRemoved() =====");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("===== [WebSocketHandler] channelRead() =====");
//        System.out.println("===== msg 출력 =====");
//        System.out.println(msg);

        // 웹 일때
        if (msg instanceof WebSocketFrame) {

            System.out.println("===== [WebSocketHandler] channelRead() : WebSocketFrame - 웹 =====");

            if (msg instanceof BinaryWebSocketFrame) {
                System.out.println("===== [WebSocketHandler] channelRead() : WebSocketFrame - BinaryWebSocketFrame Received =====");
                System.out.println(((BinaryWebSocketFrame) msg).content());

            }
            else if (msg instanceof TextWebSocketFrame) {
                System.out.println("===== [WebSocketHandler] channelRead() : WebSocketFrame - TextWebSocketFrame Received =====");

                String message = null;

                message = String.valueOf(((TextWebSocketFrame) msg).text());
                System.out.println("[CLIENT] 받은 메시지 : " + message);

                Channel ch = ctx.channel();

                // 받은 메세지를 구분값으로 split 한다
                String[] parts = message.split("/%%%/");

                String room_pk = parts[1];      //방 PK
                String type = parts[2];         //메세지 TYPE
                String user_pk = parts[3];      //유저 PK
                String streamer_pk = parts[4];  //스트리머 PK
                String user_nickname = null;    //유저 닉네임
                String user_photo_path = null;  //유저 프로필이미지경로

                System.out.println("type : " + type);
                System.out.println("user_pk : " + user_pk);
                System.out.println("streamer_pk : " + streamer_pk);

                // DB SELECT - 로그인 사용자의 닉네임과 프로필이미지경로를 조회해서 변수에 넣어준다
                try {

                    // 드라이버 로딩 및 연결
                    Class.forName(JDBC_DRIVER);
                    conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
                    stmt = conn.createStatement();

                    String sql = "SELECT user_nickname, user_photo_path FROM USERS WHERE user_PK=" + user_pk;

                    // 비로그인 사용자일 때
                    if (user_pk.trim().length() == 0 || user_pk.equals("undefined")) {
                        System.out.println("비로그인 사용자 IN");
                    }
                    // 로그인 사용자일 때 - 로그인 사용자의 닉네임과 프로필이미지경로 조회
                    else {
                        System.out.println("로그인 사용자 IN");

                        // 쿼리 실행
                        ResultSet rs = stmt.executeQuery(sql);

                        rs.first();

                        user_nickname = rs.getString("user_nickname");
                        user_photo_path = rs.getString("user_photo_path");

                        System.out.println("로그인 사용자 닉네임 : " + user_nickname);

                        rs.close();

                        stmt.close();
                        conn.close();
                    }

                }
                catch (SQLException se1) {
                    se1.printStackTrace();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                finally {

                    try {
                        if (stmt != null)
                            stmt.close();
                    }
                    catch (SQLException se2) {
                    }

                    try {
                        if (conn != null)
                            conn.close();
                    }
                    catch (SQLException se) {
                        se.printStackTrace();
                    }
                }

                // @ 스트리머가 스트리밍 방을 생성했을 때 처리
                //  └ 스트리머 객체 ChatClient 생성
                //  └ 채팅방 객체 ChatRoom 생성
                //  └ 채팅방 DB 에 "방이 생성되었습니다" 데이터 INSERT
                if (type.equals("CREATE_ROOM")) {

                    System.out.println("유저PK : "+user_pk+"##스트리머 PK : "+streamer_pk+"##전체방개수 : "+chattingRooms.size());

                    // (방송을 시작한) 스트리머의 객체 생성 및 데이터 입력
                    ChatClient client = new ChatClient();
                    client.setUser_id(user_pk);
                    client.setNickname(user_nickname);
                    client.setUser_channel(ch);

                    // 채팅방 객체 생성 및 데이터 입력
                    Chatroom chatroom = new Chatroom();
                    chatroom.setStreamer_channel(ch);
                    chatroom.setStreamer_id(user_pk);
                    chatroom.setStreamer_nickname(user_nickname);

                    // 스트리머의 방송 시작시간 세팅
                    long startTime = System.currentTimeMillis();
                    chatroom.setStreamer_start_time(startTime);

                    // 스트리머 객체를 채팅방 해쉬맵에 넣는다 (스트리머 또한 채팅방의 시청자로 처리)
                    HashMap<String, ChatClient> clients = chatroom.getClients();
                    clients.put(user_pk, client);
                    //스트리밍 방 hashmap 스트리머의 객체를 넣는다
                    chatroom.setClients(clients);

                    //채팅방 전체 데이터
                    chattingRooms.put(user_pk, chatroom);

                    // Iterator<String> iteratorK = chattingRooms.get(streamer_pk).getClients().keySet().iterator();
                    // while (iteratorK.hasNext()) {
                    //     String key = iteratorK.next();
                    //     ChatClient value = chattingRooms.get(streamer_pk).getClients().get(key);
                    //     System.out.println("[key]:" + key + ", [value]:" + value.getNickname() + "user_nickname:" + user_nickname);

                    //     value.getUser_channel().writeAndFlush("CREATE_STREAMING_ROOM/%%%/");
                    //     value.getUser_channel().writeAndFlush(new TextWebSocketFrame("CREATE_STREAMING_ROOM/%%%/"));

                    // }


                    // DB INSERT - 채팅방에 "방이 생성되었습니다" 라는 데이터를 추가한다
                    try {

                        Class.forName(JDBC_DRIVER);
                        conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);

                        // 현재 날짜 및 시간
                        Date date = new Date();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                        String register_time = simpleDateFormat.format(date);
                        String create_message = "방이 생성되었습니다.";

                        String sql = "INSERT INTO STREAMINGROOMS_CHATTING (room_id, user_id, user_nickname, type, message, register_date) values (" + "'" + room_pk + "'," + "'" + user_pk + "'," + "'" + user_nickname + "'," + "'" + type + "'," + "'" + create_message + "'," + "'" + register_time + "'" + ")";
                        int r = stmt.executeUpdate(sql);

                        System.out.println("[DB] STREAMINGROOMS_CHATTING INSERT row : " + r);

                        stmt.close();
                        conn.close();

                    } catch (SQLException se1) {
                        se1.printStackTrace();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        try {
                            if (stmt != null)
                                stmt.close();
                        } catch (SQLException se2) {
                            se2.printStackTrace();
                        }
                        try {
                            if (conn != null)
                                conn.close();
                        } catch (SQLException se) {
                            se.printStackTrace();
                        }
                    }
                }

                // @ 스트리머가 방송을 종료했을 때 처리
                else if (type.equals("REMOVE_ROOM")) {

                    ///매치에서 지워준다
                    match.remove(ch);

                    // 방 번호
                    String room_id = parts[5];

                    //시청자들에게 스트리머가 나갔다는 메세지를 보냄
                    Iterator<String> iteratorK = chattingRooms.get(user_pk).getClients().keySet().iterator();

                    while (iteratorK.hasNext()) {

                        String key = iteratorK.next();
                        ChatClient value = chattingRooms.get(user_pk).getClients().get(key);
                        System.out.println("[key]:" + key + ", [value]:" + value.getNickname());

                        value.getUser_channel().writeAndFlush("REMOVE_ROOM" + "\n");
                        value.getUser_channel().writeAndFlush(new TextWebSocketFrame("REMOVE_ROOM/%%%/" + user_nickname + "/%%%/" + user_pk + "/%%%/" + user_photo_path));
                    }


                    //채팅방 해쉬맵에서도 지워준다
                    chattingRooms.remove(user_pk);
                    System.out.println("방장(스트리머) 나감");

                    System.out.println("유저PK : "+user_pk+"##스트리머 PK : "+streamer_pk+"##전체방개수 : "+chattingRooms.size());

                    // DB UPDATE & INSERT - 스트리밍방의 상태를 "off" 처리 해주고, 채팅방에 "방이 제거되었습니다" 라는 데이터를 추가한다
                    try {

                        Class.forName(JDBC_DRIVER);
                        conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);

                        // 현재 날짜 및 시간
                        Date date = new Date();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                        // 방송 종료 시간
                        String register_time_leave = simpleDateFormat.format(date);

                        stmt = conn.createStatement();

                        // 방 상태를 off 로 변경한다
                        String room_state = "off";

                        String sql = "UPDATE STREAMINGROOMS SET room_state = " + "'" + room_state + "'" + ",chatting_start_time = " + "'" + ",chatting_end_time = " + "'" + register_time_leave + "'" + ",broadcasting_hours = " + "'" + " WHERE streamer_id = " + "'" + streamer_pk + "'" + " AND room_PK = " + "'" + room_id + "'";
                        int r = stmt.executeUpdate(sql);

                        System.out.println("[DB] STREAMINGROOMS UPDATE row : " + r);

                        String msg_remove = "방 제거되었습니다.";

                        sql = "INSERT INTO STREAMINGROOMS_CHATTING (room_id, user_id, user_nickname, type, message, register_date) values (" + "'" + room_pk + "'," + "'" + user_pk + "'," + "'" + user_nickname + "'," + "'" + type + "'," + "'" + msg_remove + "'," + "'" + register_time_leave + "'" + ")";
                        int rs = stmt.executeUpdate(sql);

                        System.out.println("[DB] STREAMINGROOMS_CHATTING INSERT row : " + rs);

                        stmt.close();
                        conn.close();

                    } catch (SQLException se1) {
                        se1.printStackTrace();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        try {
                            if (stmt != null)
                                stmt.close();
                        } catch (SQLException se2) {
                            se2.printStackTrace();
                        }
                        try {
                            if (conn != null)
                                conn.close();
                        } catch (SQLException se) {
                            se.printStackTrace();
                        }
                    }
                }

                // @ 시청자가 방에 들어갔을 때 처리
                else if (type.equals("USER_IN")) {

                    if (user_pk.trim().length() == 0 || user_pk.equals("undefined")) {

                        System.out.println("[USER_ATTEND] 비로그인 사용자 들어옴");
                        System.out.println("유저PK : "+user_pk+"##스트리머 PK : "+streamer_pk+"##전체방개수 : "+chattingRooms.size());

                    } else {
                        System.out.println("[USER_ATTEND] 로그인 사용자 들어옴");
                        System.out.println("유저PK : "+user_pk+"##스트리머 PK : "+streamer_pk+"##전체방개수 : "+chattingRooms.size());

                        //시청자의 객체 생성 및 데이터 입력
                        ChatClient client = new ChatClient();
                        client.setUser_id(user_pk);
                        client.setNickname(user_nickname);
                        client.setUser_channel(ch);

                        //시청자가 들어간 스트리밍 채팅방 시청자 데이터가 들어간 해쉬맵
                        HashMap<String, ChatClient> clients = chattingRooms.get(streamer_pk).getClients();

                        //해당 시청자의 객체를 넣어준다
                        clients.put(user_pk, client);

                        chattingRooms.get(streamer_pk).setClients(clients);

                        //채팅방 인원리스트를 불러온다
                        Iterator<String> iteratorK = chattingRooms.get(streamer_pk).getClients().keySet().iterator();

                        //채팅방에 속해있는 모든사람들에게 메세지를 보내준다
                        while (iteratorK.hasNext()) {
                            String key = iteratorK.next();
                            ChatClient value = chattingRooms.get(streamer_pk).getClients().get(key);
                            System.out.println("[key]:" + key + ", [value]:" + value.getNickname() + "user_nickname:" + user_nickname);

                            value.getUser_channel().writeAndFlush("USER_IN/%%%/" + user_nickname + "\n");

                            value.getUser_channel().writeAndFlush(new TextWebSocketFrame("USER_IN/%%%/" + user_nickname + "/%%%/" + user_pk + "/%%%/" + user_photo_path));

                        }

                        // DB UPDATE - 채팅방 시청자수 +1 증가시킨다
                        try {

                            Class.forName(JDBC_DRIVER);
                            conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);

                            stmt = conn.createStatement();

                            String sql = "UPDATE STREAMINGROOMS SET room_viewer_number = room_viewer_number + 1 WHERE room_state= " + "'" + "on" + "'" + " AND streamer_id =" + streamer_pk;
                            ResultSet rs = stmt.executeQuery(sql);

                            rs.first();

                            // 현재 날짜 및 시간
                            Date date = new Date();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                            String register_time = sdf.format(date);

                            // 스트리밍 방에서 어떤 시청자가 들어왔는지 확인할 수 있게 하기 위해서, 해당 방에 들어간 시청자를 테이블에 추가한다
                            sql = "insert into STREAMINGROOMS_VIEWER (room_id,streamer_id, user_id, register_time) values (" + "'" + room_pk + "'," + "'" + streamer_pk + "'," + "'" + user_pk + "'," + "'" + register_time + "'" + ")";
                            int r = stmt.executeUpdate(sql);

                            System.out.println("[DB] STREAMINGROOMS_VIEWER INSERT row : " + r);

                            String attend_mesage = user_nickname + "님이 입장했습니다.";

                            // DB에 유저가 입장했다고 알려준다.
                            sql = "INSERT INTO STREAMINGROOMS_CHATTING (room_id, user_id, user_nickname,type,message,register_date) values (" + "'" + room_pk + "'," + "'" + user_pk + "'," + "'" + user_nickname + "'," + "'" + type + "'," + "'" + attend_mesage + "'," + "'" + register_time + "'" + ")";
                            int rsa = stmt.executeUpdate(sql);

                            System.out.println("[DB] STREAMINGROOMS_CHATTING INSERT row : " + r);

                            stmt.close();
                            conn.close();

                        } catch (SQLException se1) {
                            se1.printStackTrace();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        } finally {
                            try {
                                if (stmt != null)
                                    stmt.close();
                            } catch (SQLException se2) {
                                se2.printStackTrace();
                            }
                            try {
                                if (conn != null)
                                    conn.close();
                            } catch (SQLException se) {
                                se.printStackTrace();
                            }
                        }

                    }

                }

                // @ 시청자가 방에서 메세지를 입력했을 때 처리
                else if (type.equals("USER_SEND_MESSAGE")) {

                    String content = parts[5];

                    System.out.println("유저PK : "+user_pk+"##스트리머 PK : "+streamer_pk+"##전체방개수 : "+chattingRooms.size()+"##유저메세지 : "+content);

                    // 채팅방 인원리스트를 불러온다
                    System.out.println("채팅방 인원: " + chattingRooms.get(streamer_pk));
                    System.out.println("채팅방 인원: " + chattingRooms.get(streamer_pk).getClients());
                    System.out.println("채팅방 인원: " + chattingRooms.get(streamer_pk).getClients().keySet());
                    System.out.println("채팅방 인원: " + chattingRooms.get(streamer_pk).getClients().keySet().iterator());

                    Iterator<String> iteratorK = chattingRooms.get(streamer_pk).getClients().keySet().iterator();

                    // 채팅방에 속해있는 모든사람들에게 메세지를 보내준다
                    while (iteratorK.hasNext()) {
                        String key = iteratorK.next();
                        ChatClient value = chattingRooms.get(streamer_pk).getClients().get(key);
                        System.out.println("[key]:" + key + ", [value]:" + value.getNickname() + "user_nickname:" + user_nickname);
                        if (value.getUser_id().equals(user_pk)) {
                            // 자기 자신이 친말을 자기자신에게 보내지 않는다
                        } else {
                            value.getUser_channel().writeAndFlush("USER_SEND_MESSAGE/%%%/" + user_nickname + "/%%%/" + content + "\n");
                            value.getUser_channel().writeAndFlush(new TextWebSocketFrame("USER_SEND_MESSAGE/%%%/" + user_nickname + "/%%%/" + content + "/%%%/" + user_pk + "/%%%/" + user_photo_path));
                        }
                    }

                    // DB INSERT - 채팅방에 유저가 입력한 데이터를 추가한다
                    try {
                        Class.forName(JDBC_DRIVER);
                        conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
                        stmt = conn.createStatement();

                        //현재 날짜 및 시간
                        Date date = new Date();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                        String register_time = sdf.format(date);

                        // DB에 유저가 입력한 메시지를 저장한다
                        String sql = "INSERT INTO STREAMINGROOMS_CHATTING (room_id, user_id, user_nickname, type, message, register_date) values (" + "'" + room_pk + "'," + "'" + user_pk + "'," + "'" + user_nickname + "'," + "'" + type + "'," + "'" + content + "'," + "'" + register_time + "'" + ")";
                        int r = stmt.executeUpdate(sql);

                        System.out.println("[DB] STREAMINGROOMS_CHATTING INSERT row : " + r);

                        stmt.close();
                        conn.close();

                    } catch (SQLException se1) {
                        se1.printStackTrace();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        try {
                            if (stmt != null)
                                stmt.close();
                        } catch (SQLException se2) {
                            se2.printStackTrace();
                        }
                        try {
                            if (conn != null)
                                conn.close();
                        } catch (SQLException se) {
                            se.printStackTrace();
                        }
                    }
                }

                // @ 시청자가 방에서 나갔을때
                else if (type.equals("USER_OUT")) {

                    System.out.println("유저PK : "+user_pk+"##스트리머 PK : "+streamer_pk+"##전체방개수 : "+chattingRooms.size());

                    // 시청자가 들어간 스트리밍 채팅방 시청자 데이터가 들어간 해쉬맵
                    HashMap<String, ChatClient> clients = chattingRooms.get(streamer_pk).getClients();

                    clients.remove(user_pk);
                    chattingRooms.get(streamer_pk).setClients(clients);

                    // 채팅방 인원리스트를 불러온다
                    Iterator<String> iteratorK = chattingRooms.get(streamer_pk).getClients().keySet().iterator();

                    //채팅방에 속해있는 모든사람들에게 메세지를 보내준다
                    while (iteratorK.hasNext()) {
                        String key = iteratorK.next();
                        ChatClient value = chattingRooms.get(streamer_pk).getClients().get(key);
                        System.out.println("[key]:" + key + ", [value]:" + value.getNickname() + "user_nickname:" + user_nickname);

                        value.getUser_channel().writeAndFlush("USER_OUT/%%%/" + user_nickname + "\n");
                        value.getUser_channel().writeAndFlush(new TextWebSocketFrame("USER_OUT/%%%/" + user_nickname + "/%%%/" + user_pk + "/%%%/" + user_photo_path));

                    }

                    //사용자의 세션을 종료해준다
                    ctx.close();

                    // DB UPDATE - 채팅방 시청자수 -1 증가시킨다
                    try {

                        Class.forName(JDBC_DRIVER);
                        conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
                        stmt = conn.createStatement();

                        String sql = "update STREAMINGROOMS set room_viewer_number = room_viewer_number - 1 where room_state= " + "'" + "on" + "'" + " and streamer_id =" + streamer_pk;
                        ResultSet rs = stmt.executeQuery(sql);

                        System.out.println("[DB] STREAMINGROOMS UPDATE row : " + rs);

                        //현재 날짜 및 시간
                        Date d = new Date();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                        String register_time = sdf.format(d);

                        String user_out = user_nickname + "님이 퇴장했습니다.";

                        sql = "INSERT INTO STREAMINGROOMS_CHATTING (room_id, user_id, user_nickname,type,message,register_date) values (" + "'" + room_pk + "'," + "'" + user_pk + "'," + "'" + user_nickname + "'," + "'" + type + "'," + "'" + user_out + "'," + "'" + register_time + "'" + ")";
                        int r = stmt.executeUpdate(sql);

                        System.out.println("[DB] STREAMINGROOMS_CHATTING INSERT row : " + r);

                        stmt.close();
                        conn.close();

                    } catch (SQLException se1) {
                        se1.printStackTrace();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        try {
                            if (stmt != null)
                                stmt.close();
                        } catch (SQLException se2) {
                        }
                        try {
                            if (conn != null)
                                conn.close();
                        } catch (SQLException se) {
                            se.printStackTrace();
                        }
                    }

                    //나중에 채팅방 유저의 channel로 pk을 찾기 위해서 해쉬맵에 등록해 놓는다
                    match.put(ch, user_pk);

                    //사용자가 어느 채팅방에 들어 있는지 알기 위해서 넣어준다
                    client_chatroom.put(ch, streamer_pk);

                }

                else if (msg instanceof PingWebSocketFrame) {
                    System.out.println("PingWebSocketFrame Received : ");
                    System.out.println(((PingWebSocketFrame) msg).content());
                }
                else if (msg instanceof PongWebSocketFrame) {
                    System.out.println("PongWebSocketFrame Received : ");
                    System.out.println(((PongWebSocketFrame) msg).content());
                }
                else if (msg instanceof CloseWebSocketFrame) {
                    System.out.println("CloseWebSocketFrame Received : ");
                    System.out.println("ReasonText :" + ((CloseWebSocketFrame) msg).reasonText());
                    System.out.println("StatusCode : " + ((CloseWebSocketFrame) msg).statusCode());
                }
                else {
                    System.out.println("Unsupported WebSocketFrame");

                }

            }
            // 모바일 일때
            else {

                System.out.println("===== [WebSocketHandler] channelRead() : WebSocketFrame ? =====");
                // 추후 코드 추가 예정

            }
        }
        // 모바일 일때
        else {
            System.out.println("===== [WebSocketHandler] channelRead() : WebSocketFrame - 모바일 =====");
            // 추후 코드 추가 예정
        }
    }

    // 데이터 수신이 완료되었음
    // 소켓 채널에 더 이상 읽을 데이터가 없을때 발생
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("===== [WebSocketHandler] channelReadComplete() =====");
        ctx.flush();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("===== [WebSocketHandler] exceptionCaught() =====");
        cause.printStackTrace();
        // ctx.close();
    }

    public void close() {
        cf.channel().close();
        group.shutdownGracefully();
    }


}