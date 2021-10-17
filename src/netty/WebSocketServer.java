package netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public final class WebSocketServer {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", SSL ? "8443" : "8080"));

    public static void main(String[] args) throws Exception {

        System.out.println("Hi, Im WebSocketServer!");
        System.out.println("PORT: "+PORT);
        System.out.println("SSL: "+SSL);

        final SslContext sslCtx;

//        File crt = new File("../../../../../인증서경로/인증서파일명.crt"); // 인증서 파일
//        File key = new File("../../../../../개인키경로/개인키명.pem"); // 개인키 파일
//
//        System.out.println("파일의 존재 여부 crt: " + crt.exists());
//        System.out.println("파일의 존재 여부 key: " + key.exists());
//
//
//        sslCtx = SslContextBuilder.forServer(cert, key).build();

        if (SSL) {
            System.out.println("===== [WebSocketServer] SSL 존재O =====");
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            System.out.println("===== [WebSocketServer] SSL 존재X =====");
            sslCtx = null;
        }

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {

            ServerBootstrap bootstrap = new ServerBootstrap();

            // @ bootstrap 에 쓰레드 등록. 첫번째 인자는 부모, 두번째 인자는 자식
            // └ 부모 : 외부에서 들어오는 클라이언트 연결을 받고
            // └ 자식 : 연결된 클라이언트 소켓을 바탕으로 데이터 입출력 및 이벤트 처리를 담당한다
            bootstrap.group(bossGroup, workerGroup) // group() : 이벤트 루프를 설정
                    // 부모 쓰레드가 사용할 네트워크 입출력 모드 설정
                    .channel(NioServerSocketChannel.class) // channel() : 소켓 입출력 모드 설정
                    .handler(new LoggingHandler(LogLevel.INFO)) // handler() : 서버 소켓 채널의 이벤트 핸들러 설정, 부모 쓰레드에서 발생한 이벤트만 처리
                    // 자식 쓰레드의 초기화 방법 설정
                    .childHandler(new WebSocketServerInitializer(sslCtx)); // childHandler() : 클라이언트 소켓 채널의 이벤트 핸들러 설정, 자식 쓰레드에서 발생한 이벤트만 처리

            Channel ch = bootstrap.bind(PORT).sync().channel();

            System.out.println("Open your web browser and navigate to " + (SSL ? "https" : "http") + "://127.0.0.1:" + PORT + '/');

            ch.closeFuture().sync();

        }
        catch (InterruptedException e){
            System.out.println("===== [WebSocketServer] e =====");
            throw new RuntimeException(e);
        }
        finally {
            System.out.println("===== [WebSocketServer] f =====");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}