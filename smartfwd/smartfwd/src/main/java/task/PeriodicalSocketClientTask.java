package apps.smartfwd.src.main.java.task;


import apps.smartfwd.src.main.java.task.base.PeriodicalTask;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class PeriodicalSocketClientTask extends PeriodicalTask {
    public interface ResponseHandler {
        void handle(String payload);
    }
    public interface RequestGenerator {
        String payload();
    }
    String ip;
    int port;
    ResponseHandler handler;
    RequestGenerator payloadGenerator;
    public PeriodicalSocketClientTask(String ip, int port, RequestGenerator payloadGenerator, ResponseHandler handler){
        this.ip=ip;
        this.port=port;
        this.handler=handler;
        this.payloadGenerator=payloadGenerator;
        this.worker=()->{
            try {
                SocketChannel socketChannel = SocketChannel.open();
                //for docker test only
                //todo delete this
                InetAddress address = InetAddress.getByName(this.ip);
                socketChannel.connect(new InetSocketAddress(this.ip,this.port));
                ByteBuffer byteBuffer = ByteBuffer.allocate(512 * 1024);
                String payload=this.payloadGenerator.payload();
                if(null==payload) return;
                byteBuffer.put(payload.getBytes());
                byteBuffer.flip();
                socketChannel.write(byteBuffer);
                while (byteBuffer.hasRemaining()) {
                    socketChannel.write(byteBuffer);
                }
                byteBuffer.clear();
                //接收数据
                int len = 0;
                StringBuilder stringBuilder = new StringBuilder();
                while ((len = socketChannel.read(byteBuffer)) >= 0) {
                    byteBuffer.flip();
                    String res = new String(byteBuffer.array(), 0, len);
                    byteBuffer.clear();
                    stringBuilder.append(res);
                }
                String resp = stringBuilder.toString();
                socketChannel.close();
                this.handler.handle(resp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }
}
