package netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;

import java.util.List;

public class PortUnificationServerHandler extends ByteToMessageDecoder {

    private final SslContext sslCtx;
    private final boolean detectSsl;
    private final boolean detectGzip;

    public PortUnificationServerHandler(SslContext sslCtx) {
        this(sslCtx, true, true);
    }

    private PortUnificationServerHandler(SslContext sslCtx, boolean detectSsl, boolean detectGzip) {
        System.out.println("===== [PortUni] 객체 실행 =====");
        this.sslCtx = sslCtx;
        this.detectSsl = detectSsl;
        this.detectGzip = detectGzip;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        System.out.println("===== [PortUni] decode() =====");

        System.out.println("ctx : " + ctx); // ChannelHandlerContext(PortUnificationServerHandler#0, [id: 0x36597824, L:/127.0.0.1:8080 - R:/127.0.0.1:4607])
        System.out.println("in : " + in);   // PooledUnsafeDirectByteBuf(ridx: 0, widx: 520, cap: 2048)
        System.out.println("out : " + out); // []

        // readerIndex() : ByteBuf 에서 데이터를 읽으면 ByteBuf 의 readerIndex 가 읽은 바이트 수만큼 증가한다
        // getUnsignedByte() : 지정한 인덱스의 부호 없는 바이트 값을 short 로 반환
        int ridx = in.readerIndex();
        int widx = in.writerIndex();
        int cap = in.capacity();
        int pro = in.getUnsignedByte(in.readerIndex());

        // TODO : https://perfectacle.github.io/2021/02/28/netty-byte-buf/ 공부하고 가자!
        // 클라에서 입력하는 문자가 달라도 아래 4개의 값은 동일함
        System.out.println("ridx:" + ridx); // 0
        System.out.println("widx:" + widx); // 520
        System.out.println("cap:" + cap);   // 2048
        System.out.println("pro:" + pro);   // 71

        if (in.readableBytes() < 5) {
            int a = in.getUnsignedByte(in.readerIndex());
            System.out.println("a : " + a); // X
            return;
        }

        if (isSsl(in)) {

            enableSsl(ctx);

        } else {

            final int magic1 = in.getUnsignedByte(in.readerIndex());
            final int magic2 = in.getUnsignedByte(in.readerIndex() + 1);
            System.out.println("magic1: " + magic1); // 71 -> G
            System.out.println("magic2: " + magic2); // 69 -> E

            if (isGzip(magic1, magic2)) {

                System.out.println("===== [PortUni] isGzip() =====");
                enableGzip(ctx);

            }
            else if (isHttp(magic1, magic2)) {

                System.out.println("===== [PortUni] isHttp() =====");

                StringBuilder builder = new StringBuilder();

                for (int i = 0; i < in.capacity(); i++) {
                    byte b = in.getByte(i);
                    // byte[] zzz[i]\ =  in.getByte(i);
                    // System.out.print((char) b);
                    //  String ggg = b.toString();
                    //  sb.setCharAt(i, (char) b);
                    builder.append((char) b);
                }

//                System.out.println("===== builder 출력 =====");
//                System.out.println(builder.toString());

                // @ 웹소켓 요청
                // └ GET / HTTP/1.1
                // └ Connection: Upgrade (필수)
                // └ Upgrade: websocket (필수)
                // └ Sec-WebSocket-Key: nvdiegWeLp09WPg06Jtlsg==
                //      -> 브라우저는 랜덤하게 생성한 키를 서버에 보낸다. 웹 서버는 이 키를 바탕으로 토큰을 생성한 후 브라우저에 돌려준다. 이런 과정으로 WebSocket 핸드쉐이킹이 이루어진다.
                if (builder.indexOf("Connection: Upgrade") > -1) { // 문자열이없는 경우 -1을 반환

                    System.out.println("===== [PortUni] isHttp() : 웹으로 접속 =====");

                    switchWeb(ctx);
                }
                else if (builder.indexOf("Connection: keep-alive") > -1) {

                    System.out.println("===== [PortUni] isHttp() : 모바일로 접속 =====");

                    switchMobile(ctx);

                } else {
                    System.out.println("===== [PortUni] isHttp() : 알수없음 =====");
                }

            }
            else if (isFactorial(magic1)) {

                System.out.println("===== [PortUni] isFactorial() =====");

                switchToFactorial(ctx);

            }
            else {
                System.out.println("===== [PortUni] 알수없음 =====");
            }
        }
    }

    private boolean isSsl(ByteBuf buf) {
        if (detectSsl) {
            return SslHandler.isEncrypted(buf);
        }
        return false;
    }

    private boolean isGzip(int magic1, int magic2) {
        if (detectGzip) {
            return magic1 == 31 && magic2 == 139;
        }
        return false;
    }

    private static boolean isHttp(int magic1, int magic2) {
        return
                magic1 == 'G' && magic2 == 'E' || // GET
                magic1 == 'P' && magic2 == 'O' || // POST
                magic1 == 'P' && magic2 == 'U' || // PUT
                magic1 == 'H' && magic2 == 'E' || // HEAD
                magic1 == 'O' && magic2 == 'P' || // OPTIONS
                magic1 == 'P' && magic2 == 'A' || // PATCH
                magic1 == 'D' && magic2 == 'E' || // DELETE
                magic1 == 'T' && magic2 == 'R' || // TRACE
                magic1 == 'C' && magic2 == 'O';   // CONNECT
    }

    private static boolean isFactorial(int magic1) {
        return magic1 == 'F';
    }

    private void enableSsl(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast("ssl", sslCtx.newHandler(ctx.alloc()));
        p.addLast("unificationA", new PortUnificationServerHandler(sslCtx, false, detectGzip));
        p.remove(this);
    }

    private void enableGzip(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast("gzipdeflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
        p.addLast("gzipinflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
        p.addLast("unificationB", new PortUnificationServerHandler(sslCtx, detectSsl, false));
        p.remove(this);
    }
    private void switchToFactorial(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast("decoder", new BigIntegerDecoder());
        p.addLast("encoder", new NumberEncoder());
        p.addLast("handler", new FactorialServerHandler());
        p.remove(this);
    }

    // web 사용자가 들어왔을때
    private void switchWeb(ChannelHandlerContext ctx) {
        ChannelPipeline pipeline = ctx.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new WebSocketServerCompressionHandler());
        pipeline.addLast(new ChatServerHandler());
        pipeline.addLast(new HttpClientCodec());

        pipeline.remove(this);
    }

    // mobile 사용자가 들어왔을때
    private void switchMobile(ChannelHandlerContext ctx) {
        ChannelPipeline pipeline = ctx.pipeline();
        pipeline.addLast(new ByteToMessageDecoder() {

            @Override
            protected void decode(ChannelHandlerContext ctx, ByteBuf in, java.util.List<Object> out) throws Exception {

                System.out.println("out" + out);

                out.add(in.readBytes(in.readableBytes()));

                StringBuilder builder2 = new StringBuilder();
                for (int i = 0; i < in.capacity(); i++) {
                    byte b = in.getByte(i);
                    // byte[] zzz[i]\ =  in.getByte(i);
                    // System.out.print((char) b);
                    //  String ggg = b.toString();
                    //  sb.setCharAt(i, (char) b);
                    builder2.append((char) b);
                }

            }
        });

        pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
        pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));

        //파이프라인에 이벤트 핸들러 등록
        pipeline.addLast(new ChatServerHandler());
        pipeline.addLast(new WebSocketHandler());
        pipeline.remove(this);
    }


}