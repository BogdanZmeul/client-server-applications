package network.tcp.server;

import network.storage.ProductStorage;
import network.tcp.communication.context.StoreServerContext;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.util.concurrent.atomic.AtomicBoolean;

public class StoreServerTCP {
    public static final int PORT = 8080;

    private final int port;
    private final StoreServerContext context;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private ServerSocket serverSocket;
    private StoreServerThread serverThread;

    public StoreServerTCP(int port, Key secretKey, ProductStorage productStorage,
                          int receiversCount, int decriptorsCount, int processorsCount,
                          int encriptorsCount, int sendersCount) {
        this.port = port;
        this.context = new StoreServerContext(secretKey, productStorage, receiversCount,
                decriptorsCount, processorsCount, encriptorsCount, sendersCount);
    }

    public void start() throws IOException {
        if (isRunning.get()) {
            return;
        }

        isRunning.set(true);
        serverSocket = new ServerSocket(port);
        serverThread = new StoreServerThread(serverSocket, isRunning, context);
        serverThread.start();
    }

    public void stop() {
        isRunning.set(false);
        closeServerSocket();

        for (Socket socket : context.getClientSockets()) {
            closeSocket(socket);
        }
    }

    public void awaitStop() throws InterruptedException {
        if (serverThread != null) {
            serverThread.join();
        }

        for (Thread thread : context.getClientThreads()) {
            thread.join();
        }
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Server socket close error: " + e.getMessage());
        }
    }

    private void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Client socket close error: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        byte[] keyBytes = "very_strong_key1".getBytes();
        Key secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
        ProductStorage productStorage = new ProductStorage();
        StoreServerTCP server = new StoreServerTCP(PORT, secretKey, productStorage,
                2, 2, 4, 3, 5);

        server.start();
    }
}
