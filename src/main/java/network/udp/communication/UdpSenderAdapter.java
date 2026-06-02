package network.udp.communication;

import network.protocol.NetworkSender;
import utils.PacketKey;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class UdpSenderAdapter implements NetworkSender {
    private final DatagramSocket socket;
    private final UdpClientManager clientManager;

    public UdpSenderAdapter(DatagramSocket socket, UdpClientManager clientManager) {
        this.socket = socket;
        this.clientManager = clientManager;
    }

    @Override
    public void sendMessage(byte[] data, InetAddress target) throws Exception {
        String key = PacketKey.createKey(data);
        clientManager.addResponse(key, data);

        InetSocketAddress client = clientManager.removeClient(key);
        if (client != null) {
            send(data, client);
        }
    }

    public void send(byte[] data, InetSocketAddress client) throws Exception {
        DatagramPacket packet = new DatagramPacket(data, data.length,
                client.getAddress(), client.getPort());
        socket.send(packet);
    }
}
