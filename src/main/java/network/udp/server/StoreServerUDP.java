package network.udp.server;

import cipher.Decoder;
import cipher.Encoder;
import network.Server;
import network.processor.Processor;
import network.storage.ProductStorage;
import network.udp.communication.UdpClientManager;
import network.udp.communication.UdpReceiverAdapter;
import network.udp.communication.UdpSenderAdapter;

import java.io.IOException;
import java.net.DatagramSocket;
import java.security.Key;
import java.util.concurrent.atomic.AtomicBoolean;

public class StoreServerUDP {
    public static final int PORT = 9001;

    private final int port;
    private final Key secretKey;
    private final ProductStorage productStorage;
    private final int receiversCount;
    private final int decriptorsCount;
    private final int processorsCount;
    private final int encriptorsCount;
    private final int sendersCount;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private DatagramSocket socket;
    private UdpReceiverAdapter receiverAdapter;
    private UdpSenderAdapter senderAdapter;
    private Server server;
    private UdpSocketReceiver socketReceiver;

    public StoreServerUDP(int port, Key secretKey, ProductStorage productStorage) {
        this(port, secretKey, productStorage, 2, 2, 4, 3, 5);
    }

    public StoreServerUDP(int port, Key secretKey, ProductStorage productStorage,
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
        socket = new DatagramSocket(port);
        UdpClientManager clientManager = new UdpClientManager();
        senderAdapter = new UdpSenderAdapter(socket, clientManager);
        receiverAdapter = new UdpReceiverAdapter(clientManager, senderAdapter);
        server = createServer();
        server.start();

        socketReceiver = new UdpSocketReceiver(socket, isRunning, receiverAdapter);
        socketReceiver.start();
    }

    public void stop() {
        isRunning.set(false);
        stopServerNetwork();
        if (socket != null) {
            socket.close();
        }
    }

    public void awaitStop() throws InterruptedException {
        if (socketReceiver != null) {
            socketReceiver.join();
        }
        if (server != null) {
            server.awaitStop();
        }
    }

    public static void main(String[] args) throws Exception {
        byte[] keyBytes = "very_strong_key1".getBytes();
        Key secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
        ProductStorage productStorage = new ProductStorage();
        StoreServerUDP server = new StoreServerUDP(PORT, secretKey, productStorage);

        server.start();
    }

    private Server createServer() {
        Processor processor = new Processor(productStorage);
        return new Server(receiverAdapter, new Decoder(secretKey), processor, new Encoder(secretKey), senderAdapter,
                receiversCount, decriptorsCount, processorsCount, encriptorsCount, sendersCount);
    }

    private void stopServerNetwork() {
        try {
            if (receiverAdapter != null) {
                receiverAdapter.stop(receiversCount);
            }
        } catch (Exception e) {
            System.out.println("UDP server network stop error: " + e.getMessage());
        }
    }
}
