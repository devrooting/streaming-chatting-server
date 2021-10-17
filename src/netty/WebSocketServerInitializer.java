package netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;

public class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {

    private static final String WEBSOCKET_PATH = "/websocket";
    private SslContext sslCtx;


    public WebSocketServerInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }


    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        System.out.println("===== [WebSocketServerInitializer] initChannel() =====");

        ChannelPipeline pipeline = ch.pipeline();

        if (sslCtx != null) {
            System.out.println("===== [WebSocketServerInitializer] sslCtx != null =====");
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        }

        pipeline.addLast(new PortUnificationServerHandler(sslCtx));


    }


}
