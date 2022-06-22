package apps.smartfwd.src.main.java.task;



import apps.smartfwd.src.main.java.task.base.AbstractStoppableTask;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class SocketServerTask extends AbstractStoppableTask {
    public interface Handler{
        void handle(String payload);
    }
    Handler handler;
    String ip;
    int port;
    ServerSocketChannel channel=null;

    public SocketServerTask(String ip, int port, Handler handler){
        this.ip = ip;
        this.port=port;
        this.handler=handler;
    }
    @Override
    public void run() {
        Selector selector = null;
        ByteBuffer buffer = ByteBuffer.allocate(2048);
        try {
            channel = ServerSocketChannel.open();
            channel.bind(new InetSocketAddress(this.ip, this.port));
            channel.configureBlocking(false);
            selector = Selector.open();
            channel.register(selector, SelectionKey.OP_ACCEPT);
            while (selector.select() > 0) {
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey next = iterator.next();
                    iterator.remove();
                    if (next.isAcceptable()) {
                        SocketChannel accept = channel.accept();
                        accept.configureBlocking(false);
                        accept.register(selector, SelectionKey.OP_READ);
                    } else if (next.isReadable()) {
                        SocketChannel channel = (SocketChannel) next.channel();
                        int len = 0;
                        StringBuilder stringBuilder = new StringBuilder();
                        while ((len = channel.read(buffer)) >= 0) {
                            buffer.flip();
                            String res = new String(buffer.array(), 0, len);
                            buffer.clear();
                            stringBuilder.append(res);
                        }
                        String payload = stringBuilder.toString();
                        handler.handle(payload);
                        channel.close();
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (selector != null) {
                try {
                    selector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public void stop(){
        if(null==channel) return;
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
