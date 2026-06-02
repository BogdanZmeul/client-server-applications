package network.tcp.client;

import cipher.Decoder;
import cipher.Encoder;
import data.Message;
import data.Package;
import network.tcp.server.StoreServerTCP;
import utils.MessageType;

import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.security.Key;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class StoreClientTCP {
    private final StoreClientContext context;

    private StoreClientThread clientThread;

    public StoreClientTCP(InetAddress address, int port, Encoder encoder, Decoder decoder) {
        this.context = new StoreClientContext(address, port, encoder, decoder);
    }

    public void start() {
        if (context.getIsWorking().get()) {
            return;
        }

        context.getIsWorking().set(true);
        clientThread = new StoreClientThread(context);
        clientThread.start();
    }

    public void stop() {
        context.getIsWorking().set(false);

        if (clientThread != null) {
            clientThread.closeConnection();
            clientThread.interrupt();
        }
    }

    public void awaitStop() throws InterruptedException {
        if (clientThread != null) {
            clientThread.join();
        }
    }

    public void sendPackage(Package pack) {
        context.getPackagesToSend().add(pack);
    }

    public Package receiveResponse() throws InterruptedException {
        return context.getResponses().take();
    }

    public Package receiveResponse(long timeoutMillis) throws InterruptedException {
        return context.getResponses().poll(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public boolean getIsConnected() {
        return context.getIsConnected().get();
    }

    public int getPackagesToSendCount() {
        return context.getPackagesToSend().size();
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
