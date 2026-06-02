package network.udp.communication;

import network.protocol.NetworkReceiver;
import utils.PacketKey;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class UdpReceiverAdapter implements NetworkReceiver {
    private static final byte[] END_PACKET = new byte[0];

    private final BlockingQueue<byte[]> receivedPackets = new LinkedBlockingQueue<>();
    private final UdpClientManager clientManager;
    private final UdpSenderAdapter sender;

    public UdpReceiverAdapter(UdpClientManager clientManager, UdpSenderAdapter sender) {
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

    public void addPacket(byte[] data, InetAddress address, int port) throws Exception {
        String key = PacketKey.createKey(data);
        InetSocketAddress client = new InetSocketAddress(address, port);
        byte[] response = clientManager.getResponse(key);

        if (response != null) {
            sender.send(response, client);
            return;
        }

        if (clientManager.addClient(key, client)) {
            receivedPackets.put(data);
        }
    }

    public void stop(int receiversCount) throws Exception {
        for (int i = 0; i < receiversCount; i++) {
            receivedPackets.put(END_PACKET);
        }
    }
}
