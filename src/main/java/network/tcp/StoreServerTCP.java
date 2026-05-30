package network.tcp;

import cipher.Decoder;
import cipher.Encoder;
import network.Server;
import network.processor.Processor;
import network.storage.ProductStorage;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class StoreServerTCP {
    public static final int PORT = 8080;

    private final int port;
    private final Key secretKey;
    private final ProductStorage productStorage;
    private final int receiversCount;
    private final int decriptorsCount;
    private final int processorsCount;
    private final int encriptorsCount;
    private final int sendersCount;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final BlockingQueue<Socket> clientSockets = new LinkedBlockingQueue<>();
    private final BlockingQueue<Thread> clientThreads = new LinkedBlockingQueue<>();

    private ServerSocket serverSocket;
    private Thread serverThread;

    public StoreServerTCP(int port, Key secretKey, ProductStorage productStorage,
                          int receiversCount, int decriptorsCount, int processorsCount,
                          int encriptorsCount, int sendersCount) {
        this.port = port;
        this.secretKey = secretKey;
        this.productStorage = productStorage;
        this.receiversCount = receiversCount;
        this.decriptorsCount = decriptorsCount;
        this.processorsCount = processorsCount;
        this.encriptorsCount = encriptorsCount;
        this.sendersCount = sendersCount;
    }

    public void start() throws IOException {
        if (isRunning.get()) {
            return;
        }

        isRunning.set(true);
        serverSocket = new ServerSocket(port);
        serverThread = new Thread(this::acceptClients);
        serverThread.start();
    }

    public void stop() {
        isRunning.set(false);
        closeServerSocket();

        for (Socket socket : clientSockets) {
            closeSocket(socket);
        }
    }

    public void awaitStop() throws InterruptedException {
        if (serverThread != null) {
            serverThread.join();
        }

        for (Thread thread : clientThreads) {
            thread.join();
        }
    }

    private void acceptClients() {
        System.out.println("StoreServerTCP started");
        try {
            while (isRunning.get()) {
                Socket socket = serverSocket.accept();
                clientSockets.add(socket);
                ClientConnection clientConnection = new ClientConnection(socket);
                clientThreads.add(clientConnection);
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

    private class ClientConnection extends Thread {
        private final Socket socket;

        public ClientConnection(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println("Open client connection: " + socket);
            try (socket) {
                Server server = getServer();
                server.awaitStop();
            } catch (Exception e) {
                System.out.println("Client connection error: " + e.getMessage());
            } finally {
                clientSockets.remove(socket);
                clientThreads.remove(this);
                System.out.println("Close client connection: " + socket);
            }
        }

        private Server getServer() throws IOException {
            TcpNetworkReceiver receiver = new TcpNetworkReceiver(socket.getInputStream());
            TcpNetworkSender sender = new TcpNetworkSender(socket.getOutputStream());
            Processor processor = new Processor(productStorage);
            Server server = new Server(receiver, new Decoder(secretKey), processor,
                    new Encoder(secretKey), sender, socket.getInetAddress(),
                    receiversCount, decriptorsCount, processorsCount,
                    encriptorsCount, sendersCount);

            server.start();
            return server;
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
