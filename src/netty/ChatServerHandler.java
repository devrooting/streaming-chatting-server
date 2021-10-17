package netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

public class ChatServerHandler extends ChannelInboundHandlerAdapter {

    WebSocketServerHandshaker handshaker;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("===== [ChatServerHandler] handlerAdded() =====");
    }


    // 채널 입출력 준비 완료
    // 연결 직후 한번 수행하는 작업에 유용
    // 채널 입출력 준비 완료 사용자가 들어왔을때.
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("===== [ChatServerHandler] channelActive() =====");
    }


    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("===== [ChatServerHandler] handlerRemoved() =====");
    }


    // 데이터 수신이 완료되었음
    // 소켓 채널에 더 이상 읽을 데이터가 없을때 발생
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("===== [ChatServerHandler] channelReadComplete() =====");
    }

    // 데이터가 수신되었음
    // ByteBuf 객체로 전달됨
    // 메시지가 들어올 때마다 호출되는 함수. 여기서는 수신한 데이터를 모두 처리하기위해 재정의
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        System.out.println("===== [ChatServerHandler] channelRead() =====");
//        System.out.println("===== msg 출력 =====");
//        System.out.println(msg);

        if (msg instanceof HttpRequest) {
            //객체 instanceof 클래스
            // 웹 접속
            System.out.println("===== [ChatServerHandler] channelRead() : HttpRequest O =====");

            HttpRequest httpRequest = (HttpRequest) msg;

            HttpHeaders headers = httpRequest.headers();

            System.out.println("Connection : " + headers.get("Connection"));
            System.out.println("Upgrade : " + headers.get("Upgrade"));
            System.out.println("User-Agent : " + headers.get("User-Agent"));

            if ((headers.get("Connection").indexOf("keep-alive") > -1)) { // 문자열이없는 경우 -1을 반환
                // 모바일
                System.out.println("===== [ChatServerHandler] channelRead() : HttpRequest - 모바일 =====");
            }
            else {
                // 웹 (웹소켓)
                System.out.println("===== [ChatServerHandler] channelRead() : HttpRequest - 웹 =====");

                if ("Upgrade".equalsIgnoreCase(headers.get(HttpHeaderNames.CONNECTION))
                        && "WebSocket".equalsIgnoreCase(headers.get(HttpHeaderNames.UPGRADE))) {

                    ctx.pipeline().replace(this, "websocketHandler", new WebSocketHandler());
                    System.out.println("===== [ChatServerHandler] channelRead() : HttpRequest - 웹 >> 파이프라인에 WebSocketHandler 추가 =====");
                    //System.out.println("[ChatServerHandler] Opened Channel : " + ctx.channel());
                    System.out.println("===== [ChatServerHandler] Handshake start =====");
                    handleHandshake(ctx, httpRequest);
                    System.out.println("===== [ChatServerHandler] Handshake end =====");
                }

            }

        }
        else {
            // 모바일 접속?
            System.out.println("===== [ChatServerHandler] channelRead() : HttpRequest X =====");
            ctx.fireChannelRead(msg); // 두번째 이벤트 핸들러의 메서드에 수행하고 싶을때 추가하는 코드
        }


    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("===== [ChatServerHandler] exceptionCaught() =====");
        cause.printStackTrace();
        ctx.close();
    }

    protected void handleHandshake(ChannelHandlerContext ctx, HttpRequest req) {
        WebSocketServerHandshakerFactory wsFactory =
                new WebSocketServerHandshakerFactory(getWebSocketURL(req), null, true);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }

    protected String getWebSocketURL(HttpRequest req) {
        String url = "wss://" + req.headers().get("Host") + req.getUri();
        System.out.println("===== [ChatServerHandler] 웹소켓 URL : " + url + " =====");
        return url;
    }

}
