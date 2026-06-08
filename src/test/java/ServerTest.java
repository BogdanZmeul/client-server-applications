import cipher.Decoder;
import cipher.Encoder;
import data.Message;
import data.Package;
import db.SqliteProductService;
import network.Server;
import network.processor.Processor;
import network.protocol.NetworkReceiver;
import network.protocol.NetworkSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import service.StoreService;
import utils.MessageType;

import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.nio.file.Path;
import java.security.Key;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldProcessMessagesWithManyThreadsAndWorkers() throws Exception {
        byte[] keyBytes = "very_strong_key1".getBytes();
        Key secretKey = new SecretKeySpec(keyBytes, "AES");
        Encoder requestEncoder = new Encoder(secretKey);
        Decoder responseDecoder = new Decoder(secretKey);
        QueueNetworkReceiver receiver = new QueueNetworkReceiver();
        TestNetworkSender sender = new TestNetworkSender();
        AtomicLong packetId = new AtomicLong(1);
        int threadsCount = 8;
        int messagesCount = 25;
        Thread[] threads = new Thread[threadsCount];

        for (int i = 0; i < threadsCount; i++) {
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < messagesCount; j++) {
                        Package pack = new Package((byte) 1, packetId.getAndIncrement(),
                                new Message(MessageType.ADD_PRODUCT, 1, "apple;1"));
                        receiver.add(requestEncoder.encode(pack));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        try (SqliteProductService productDb = new SqliteProductService(tempDir.resolve("server1.db").toString())) {
            StoreService storeService = new StoreService(productDb);
            Processor processor = new Processor(storeService);
            createProduct(processor, "apple", 0, 0);
            Server server = new Server(receiver, new Decoder(secretKey), processor, new Encoder(secretKey),
                    sender, 2, 2, 4, 3, 5);

            server.start();
            server.awaitStop();

            assertEquals(threadsCount * messagesCount, storeService.getProductQuantity("apple"));
            assertEquals(threadsCount * messagesCount, sender.getSentData().size());

            for (byte[] data : sender.getSentData()) {
                Package answer = responseDecoder.decode(data);
                assertEquals("Ok", answer.getMessage().getMessage());
            }
        }
    }

    @Test
    void shouldProcessDifferentMessagesAndErrorsInOrder() throws Exception {
        byte[] keyBytes = "very_strong_key1".getBytes();
        Key secretKey = new SecretKeySpec(keyBytes, "AES");
        Encoder requestEncoder = new Encoder(secretKey);
        Decoder responseDecoder = new Decoder(secretKey);
        QueueNetworkReceiver receiver = new QueueNetworkReceiver();
        TestNetworkSender sender = new TestNetworkSender();
        AtomicLong packetId = new AtomicLong(1);

        addMessage(receiver, requestEncoder, packetId, MessageType.ADD_PRODUCT, "apple;50");
        addMessage(receiver, requestEncoder, packetId, MessageType.TAKE_PRODUCT, "apple;20");
        addMessage(receiver, requestEncoder, packetId, MessageType.ADD_GROUP, "fruits");
        addMessage(receiver, requestEncoder, packetId, MessageType.ADD_PRODUCT_TO_GROUP, "fruits;apple");
        addMessage(receiver, requestEncoder, packetId, MessageType.SET_PRICE, "apple;12.5");
        addMessage(receiver, requestEncoder, packetId, MessageType.GET_PRODUCT_COUNT, "apple");
        addMessage(receiver, requestEncoder, packetId, MessageType.TAKE_PRODUCT, "apple;100");
        addMessage(receiver, requestEncoder, packetId, MessageType.ADD_PRODUCT, "apple");

        try (SqliteProductService productDb = new SqliteProductService(tempDir.resolve("server2.db").toString())) {
            StoreService storeService = new StoreService(productDb);
            Processor processor = new Processor(storeService);
            createProduct(processor, "apple", 0, 0);
            Server server = new Server(receiver, new Decoder(secretKey), processor, new Encoder(secretKey),
                    sender, 1, 1, 1, 1, 1);

            server.start();
            server.awaitStop();

            int okResponses = 0;
            int errorResponses = 0;

            for (byte[] data : sender.getSentData()) {
                Package answer = responseDecoder.decode(data);
                String message = answer.getMessage().getMessage();

                if (message.startsWith("Ok")) {
                    okResponses++;
                }
                if (message.startsWith("Error")) {
                    errorResponses++;
                }
            }

            assertEquals(8, sender.getSentData().size());
            assertEquals(30, storeService.getProductQuantity("apple"));
            assertTrue(storeService.isProductInGroup("fruits", "apple"));
            assertEquals(12.5, storeService.getProductPrice("apple"));
            assertEquals(6, okResponses);
            assertEquals(2, errorResponses);
        }
    }

    @Test
    void shouldProcessHeavyLoadWithManyWorkers() throws Exception {
        byte[] keyBytes = "very_strong_key1".getBytes();
        Key secretKey = new SecretKeySpec(keyBytes, "AES");
        Encoder requestEncoder = new Encoder(secretKey);
        Decoder responseDecoder = new Decoder(secretKey);
        QueueNetworkReceiver receiver = new QueueNetworkReceiver();
        TestNetworkSender sender = new TestNetworkSender();
        AtomicLong packetId = new AtomicLong(1);
        int threadsCount = 12;
        int messagesCount = 100;
        Thread[] threads = new Thread[threadsCount];

        for (int i = 0; i < threadsCount; i++) {
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < messagesCount; j++) {
                        addMessage(receiver, requestEncoder, packetId, MessageType.ADD_PRODUCT, "apple;1");
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        try (SqliteProductService productDb = new SqliteProductService(tempDir.resolve("server3.db").toString())) {
            StoreService storeService = new StoreService(productDb);
            Processor processor = new Processor(storeService);
            createProduct(processor, "apple", 0, 0);
            Server server = new Server(receiver, new Decoder(secretKey), processor, new Encoder(secretKey),
                    sender, 3, 4, 6, 4, 5);

            server.start();
            server.awaitStop();

            assertEquals(threadsCount * messagesCount, storeService.getProductQuantity("apple"));
            assertEquals(threadsCount * messagesCount, sender.getSentData().size());

            for (byte[] data : sender.getSentData()) {
                Package answer = responseDecoder.decode(data);
                assertEquals("Ok", answer.getMessage().getMessage());
            }
        }
    }

    private static void addMessage(QueueNetworkReceiver receiver, Encoder encoder, AtomicLong packetId,
                                   int type, String text) throws Exception {
        Package pack = new Package((byte) 1, packetId.getAndIncrement(), new Message(type, 1, text));
        receiver.add(encoder.encode(pack));
    }

    private static void createProduct(Processor processor, String name, int count, double price) {
        processor.process(new Package((byte) 1, 1, new Message(MessageType.CREATE_PRODUCT, 1,
                name + ";" + count + ";" + price)));
    }

    private static class QueueNetworkReceiver implements NetworkReceiver {
        private final BlockingQueue<byte[]> data = new LinkedBlockingQueue<>();

        public void add(byte[] bytes) {
            data.add(bytes);
        }

        @Override
        public byte[] receiveMessage() {
            return data.poll();
        }
    }

    private static class TestNetworkSender implements NetworkSender {
        private final BlockingQueue<byte[]> sentData = new LinkedBlockingQueue<>();

        @Override
        public void sendMessage(byte[] data, InetAddress target) {
            sentData.add(data);
        }

        public BlockingQueue<byte[]> getSentData() {
            return sentData;
        }
    }
}
