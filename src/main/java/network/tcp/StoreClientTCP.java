package network.tcp;

import cipher.Decoder;
import cipher.Encoder;
import data.Message;
import data.Package;
import utils.MessageType;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.Key;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class StoreClientTCP {
    private static final int RECONNECT_DELAY = 1000;

    private final InetAddress address;
    private final int port;
    private final Encoder encoder;
    private final Decoder decoder;
    private final AtomicBoolean isWorking = new AtomicBoolean(false);
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final BlockingDeque<Package> packagesToSend = new LinkedBlockingDeque<>();
    private final BlockingQueue<Package> responses = new LinkedBlockingQueue<>();

    private Socket socket;
    private TcpNetworkReceiver receiver;
    private TcpNetworkSender sender;
    private Thread worker;

    public StoreClientTCP(InetAddress address, int port, Encoder encoder, Decoder decoder) {
        this.address = address;
        this.port = port;
        this.encoder = encoder;
        this.decoder = decoder;
    }

    public void start() {
        if (isWorking.get()) {
            return;
        }

        isWorking.set(true);
        worker = new Thread(this::work);
        worker.start();
    }

    public void stop() {
        isWorking.set(false);
        closeConnection();

        if (worker != null) {
            worker.interrupt();
        }
    }

    public void awaitStop() throws InterruptedException {
        if (worker != null) {
            worker.join();
        }
    }

    public void sendPackage(Package pack) {
        packagesToSend.add(pack);
    }

    public Package receiveResponse() throws InterruptedException {
        return responses.take();
    }

    public Package receiveResponse(long timeoutMillis) throws InterruptedException {
        return responses.poll(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public boolean getIsConnected() {
        return isConnected.get();
    }

    public int getPackagesToSendCount() {
        return packagesToSend.size();
    }

    private void work() {
        System.out.println("StoreClientTCP started");
        while (isWorking.get()) {
            try {
                if (!isConnected.get() && !tryConnect()) {
                    sleepBeforeReconnect();
                    continue;
                }

                Package pack = packagesToSend.take();
                sendAndReadResponse(pack);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        closeConnection();
        System.out.println("StoreClientTCP finished");
    }

    private boolean tryConnect() {
        try {
            socket = new Socket(address, port);
            receiver = new TcpNetworkReceiver(socket.getInputStream());
            sender = new TcpNetworkSender(socket.getOutputStream());
            isConnected.set(true);
            System.out.println("Connected to server: " + socket);
            return true;
        } catch (IOException e) {
            isConnected.set(false);
            System.out.println("Server is not available. Reconnecting...");
            return false;
        }
    }

    private void sendAndReadResponse(Package pack) {
        try {
            byte[] request = encoder.encode(pack);
            sender.sendMessage(request, address);

            byte[] response = receiver.receiveMessage();
            if (response == null) {
                throw new IOException("Connection closed");
            }

            responses.put(decoder.decode(response));
        } catch (Exception e) {
            isConnected.set(false);
            packagesToSend.addFirst(pack);
            closeConnection();
            System.out.println("Client TCP error: " + e.getMessage());
            sleepBeforeReconnect();
        }
    }

    private void closeConnection() {
        isConnected.set(false);
        if (sender != null) {
            sender.close();
        }

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Client socket close error: " + e.getMessage());
        }
    }

    private void sleepBeforeReconnect() {
        try {
            Thread.sleep(RECONNECT_DELAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws Exception {
        byte[] keyBytes = "very_strong_key1".getBytes();
        Key secretKey = new SecretKeySpec(keyBytes, "AES");
        InetAddress address = InetAddress.getByName("localhost");
        AtomicLong packetId = new AtomicLong(1);

        StoreClientTCP client = new StoreClientTCP(address, StoreServerTCP.PORT,
                new Encoder(secretKey), new Decoder(secretKey));

        client.start();
        client.sendPackage(new Package((byte) 1, packetId.getAndIncrement(),
                new Message(MessageType.ADD_PRODUCT, 1, "apple;10")));
        client.sendPackage(new Package((byte) 1, packetId.getAndIncrement(),
                new Message(MessageType.GET_PRODUCT_COUNT, 1, "apple")));

        for (int i = 0; i < 2; i++) {
            Package response = client.receiveResponse();
            System.out.println("Response: " + response.getMessage().getMessage());
        }

        client.stop();
    }
}
