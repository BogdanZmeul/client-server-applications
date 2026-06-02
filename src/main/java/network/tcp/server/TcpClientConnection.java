package network.tcp.server;

import network.tcp.communication.TcpSocketReceiver;

import java.net.Socket;

public class TcpClientConnection extends Thread {
    private final Socket socket;
    private final StoreServerContext context;

    public TcpClientConnection(Socket socket, StoreServerContext context) {
        this.socket = socket;
        this.context = context;
    }

    @Override
    public void run() {
        System.out.println("Open client connection: " + socket);
        try (socket) {
            readClientPackets();
        } catch (Exception e) {
            System.out.println("Client connection error: " + e.getMessage());
        } finally {
            context.getClientSockets().remove(socket);
            context.getClientThreads().remove(this);
            System.out.println("Close client connection: " + socket);
        }
    }

    private void readClientPackets() throws Exception {
        TcpSocketReceiver receiver = new TcpSocketReceiver(socket.getInputStream());
        while (true) {
            byte[] data = receiver.receiveMessage();
            if (data == null) {
                break;
            }

            context.getReceiverAdapter().addPacket(data, socket);
        }
    }
}
