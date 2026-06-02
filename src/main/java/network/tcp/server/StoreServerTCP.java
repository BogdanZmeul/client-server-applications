package network.tcp.server;

import cipher.Decoder;
import cipher.Encoder;
import network.Server;
import network.processor.Processor;
import network.storage.ProductStorage;

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
    private TcpConnectionListener socketListener;
    private Server server;

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
        server = createServer();
        server.start();

        serverSocket = new ServerSocket(port);
        socketListener = new TcpConnectionListener(serverSocket, isRunning, context);
        socketListener.start();
    }

    public void stop() {
        isRunning.set(false);
        stopServerNetwork();
        closeServerSocket();

        for (Socket socket : context.getClientSockets()) {
            closeSocket(socket);
        }
    }

    public void awaitStop() throws InterruptedException {
        if (socketListener != null) {
            socketListener.join();
        }

        for (Thread thread : context.getClientThreads()) {
            thread.join();
        }

        if (server != null) {
            server.awaitStop();
        }
    }

    private Server createServer() {
        Processor processor = new Processor(context.getProductStorage());
        return new Server(context.getReceiverAdapter(), new Decoder(context.getSecretKey()), processor,
                new Encoder(context.getSecretKey()), context.getSenderAdapter(),
                context.getReceiversCount(), context.getDecriptorsCount(),
                context.getProcessorsCount(), context.getEncriptorsCount(),
                context.getSendersCount());
    }

    private void stopServerNetwork() {
        try {
            context.getReceiverAdapter().stop(context.getReceiversCount());
        } catch (Exception e) {
            System.out.println("TCP server network stop error: " + e.getMessage());
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
