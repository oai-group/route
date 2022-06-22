package apps.smartfwd.src.main.java.task;

import apps.smartfwd.src.main.java.task.base.AbstractStoppableTask;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SocketClientTask extends AbstractStoppableTask {
    public interface ResponseHandler {
        void handle(String response);
    }
    String payload;
    ResponseHandler responseHandler;
    String ip;
    int port;
    public SocketClientTask(String payload, ResponseHandler responseHandler, String ip, int port){
        this.payload=payload;
        this.responseHandler = responseHandler;
        this.ip=ip;
        this.port=port;

    }
    @Override
    public void run() {
        try {
            SocketChannel socketChannel = SocketChannel.open();
            //for docker test only
            //todo delete this
            InetAddress address = InetAddress.getByName(this.ip);
            socketChannel.connect(new InetSocketAddress(this.ip,this.port));
            ByteBuffer byteBuffer = ByteBuffer.allocate(512 * 1024);
            byteBuffer.put(this.payload.getBytes());
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
            String payload = stringBuilder.toString();
            socketChannel.close();
            responseHandler.handle(payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
