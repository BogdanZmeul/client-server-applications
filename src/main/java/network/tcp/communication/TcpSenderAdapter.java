package network.tcp.communication;

import network.protocol.NetworkSender;
import utils.PacketKey;

import java.net.InetAddress;
import java.net.Socket;

public class TcpSenderAdapter implements NetworkSender {
    private final TcpClientManager clientManager;

    public TcpSenderAdapter(TcpClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public void sendMessage(byte[] data, InetAddress target) {
        String key = PacketKey.createKey(data);
        clientManager.addResponse(key, data);

        Socket socket = clientManager.removeClient(key);
        send(data, socket);
    }

    public void send(byte[] data, Socket socket) {
        if (socket == null || socket.isClosed() || data == null) {
            return;
        }

        try {
            synchronized (socket) {
                socket.getOutputStream().write(data);
                socket.getOutputStream().flush();
            }
        } catch (Exception e) {
            System.out.println("TCP send error: " + e.getMessage());
        }
    }
}
