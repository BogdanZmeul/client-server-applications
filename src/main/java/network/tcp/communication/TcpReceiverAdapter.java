package network.tcp.communication;

import network.protocol.NetworkReceiver;
import utils.PacketKey;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TcpReceiverAdapter implements NetworkReceiver {
    private static final byte[] END_PACKET = new byte[0];

    private final BlockingQueue<byte[]> receivedPackets = new LinkedBlockingQueue<>();
    private final TcpClientManager clientManager;
    private final TcpSenderAdapter sender;

    public TcpReceiverAdapter(TcpClientManager clientManager, TcpSenderAdapter sender) {
        this.clientManager = clientManager;
        this.sender = sender;
    }

    @Override
    public byte[] receiveMessage() throws Exception {
        byte[] data = receivedPackets.take();
        if (data == END_PACKET) {
            return null;
        }

        return data;
    }

    public void addPacket(byte[] data, Socket socket) throws Exception {
        String key = PacketKey.createKey(data);
        byte[] response = clientManager.getResponse(key);

        if (response != null) {
            sender.send(response, socket);
            return;
        }

        if (clientManager.addClient(key, socket)) {
            receivedPackets.put(data);
        }
    }

    public void stop(int receiversCount) throws Exception {
        for (int i = 0; i < receiversCount; i++) {
            receivedPackets.put(END_PACKET);
        }
    }
}
