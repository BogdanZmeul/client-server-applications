package network.tcp.communication;

import network.protocol.NetworkSender;

import java.io.OutputStream;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class TcpNetworkSender implements NetworkSender {
    private final OutputStream output;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public TcpNetworkSender(OutputStream output) {
        this.output = output;
    }

    @Override
    public synchronized void sendMessage(byte[] data, InetAddress target) throws Exception {
        if (closed.get()) {
            return;
        }

        output.write(data);
        output.flush();
    }

    public void close() {
        closed.set(true);
    }
}
