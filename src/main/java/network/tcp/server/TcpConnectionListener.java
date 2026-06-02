package network.tcp.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class TcpConnectionListener extends Thread {
    private final ServerSocket serverSocket;
    private final AtomicBoolean isRunning;
    private final StoreServerContext context;

    public TcpConnectionListener(ServerSocket serverSocket, AtomicBoolean isRunning,
                                 StoreServerContext context) {
        this.serverSocket = serverSocket;
        this.isRunning = isRunning;
        this.context = context;
    }

    @Override
    public void run() {
        System.out.println("StoreServerTCP started");
        try {
            while (isRunning.get()) {
                Socket socket = serverSocket.accept();
                TcpClientConnection clientConnection = createClientConnection(socket);

                context.getClientSockets().add(socket);
                context.getClientThreads().add(clientConnection);
                clientConnection.start();
            }
        } catch (IOException e) {
            if (isRunning.get()) {
                System.out.println("StoreServerTCP error: " + e.getMessage());
            }
        } finally {
            isRunning.set(false);
            closeServerSocket();
            System.out.println("StoreServerTCP finished");
        }
    }

    private TcpClientConnection createClientConnection(Socket socket) {
        return new TcpClientConnection(socket, context);
    }

    private void closeServerSocket() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.out.println("Server socket close error: " + e.getMessage());
        }
    }
}
