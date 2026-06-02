import cipher.Decoder;
import cipher.Encoder;
import data.Message;
import data.Package;
import network.storage.ProductStorage;
import network.udp.client.StoreClientUDP;
import network.udp.server.StoreServerUDP;
import org.junit.jupiter.api.Test;
import utils.MessageType;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.security.Key;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class StoreUDPTest {
    private static final int PORT = StoreServerUDP.PORT;

    @Test
    void shouldSendMessageToUdpServerAndReceiveResponse() throws Exception {
        Key secretKey = createKey();
        ProductStorage productStorage = new ProductStorage();
        StoreServerUDP server = new StoreServerUDP(PORT, secretKey, productStorage);
        StoreClientUDP client = null;

        try {
            server.start();
            client = createClient(secretKey);

            Package response = client.sendPackage(createPackage(1, MessageType.ADD_PRODUCT, "apple;7"));
            Package countResponse = client.sendPackage(createPackage(2, MessageType.GET_PRODUCT_COUNT, "apple"));

            assertEquals("Ok", response.getMessage().getMessage());
            assertEquals("Ok:7", countResponse.getMessage().getMessage());
            assertEquals(7, productStorage.getProductCount("apple"));
        } finally {
            if (client != null) {
                client.close();
            }
            server.stop();
            server.awaitStop();
        }
    }

    @Test
    void shouldProcessAllCommandTypesOverUdp() throws Exception {
        Key secretKey = createKey();
        ProductStorage productStorage = new ProductStorage();
        StoreServerUDP server = new StoreServerUDP(PORT, secretKey, productStorage);
        StoreClientUDP client = null;

        try {
            server.start();
            client = createClient(secretKey);

            Package groupResponse = client.sendPackage(createPackage(1, MessageType.ADD_GROUP, "fruits"));
            Package productToGroupResponse = client.sendPackage(createPackage(2,
                    MessageType.ADD_PRODUCT_TO_GROUP, "fruits;apple"));
            Package addResponse = client.sendPackage(createPackage(3, MessageType.ADD_PRODUCT, "apple;10"));
            Package takeResponse = client.sendPackage(createPackage(4, MessageType.TAKE_PRODUCT, "apple;4"));
            Package priceResponse = client.sendPackage(createPackage(5, MessageType.SET_PRICE, "apple;12.5"));
            Package countResponse = client.sendPackage(createPackage(6, MessageType.GET_PRODUCT_COUNT, "apple"));

            assertEquals("Ok", groupResponse.getMessage().getMessage());
            assertEquals("Ok", productToGroupResponse.getMessage().getMessage());
            assertEquals("Ok", addResponse.getMessage().getMessage());
            assertEquals("Ok", takeResponse.getMessage().getMessage());
            assertEquals("Ok", priceResponse.getMessage().getMessage());
            assertEquals("Ok:6", countResponse.getMessage().getMessage());

            assertTrue(productStorage.isProductInGroup("fruits", "apple"));
            assertEquals(6, productStorage.getProductCount("apple"));
            assertEquals(12.5, productStorage.getPrice("apple"));
        } finally {
            if (client != null) {
                client.close();
            }
            server.stop();
            server.awaitStop();
        }
    }

    @Test
    void shouldReturnErrorResponseOverUdp() throws Exception {
        Key secretKey = createKey();
        ProductStorage productStorage = new ProductStorage();
        StoreServerUDP server = new StoreServerUDP(PORT, secretKey, productStorage);
        StoreClientUDP client = null;

        try {
            server.start();
            client = createClient(secretKey);

            Package response = client.sendPackage(createPackage(1, MessageType.TAKE_PRODUCT, "apple;3"));

            assertEquals("Error:Not enough product", response.getMessage().getMessage());
            assertEquals(0, productStorage.getProductCount("apple"));
        } finally {
            if (client != null) {
                client.close();
            }
            server.stop();
            server.awaitStop();
        }
    }

    @Test
    void shouldProcessMessagesFromManyUdpClients() throws Exception {
        Key secretKey = createKey();
        ProductStorage productStorage = new ProductStorage();
        StoreServerUDP server = new StoreServerUDP(PORT, secretKey, productStorage);
        int clientsCount = 10;
        int messagesCount = 10;
        Thread[] threads = new Thread[clientsCount];
        Exception[] errors = new Exception[clientsCount];
        AtomicLong packetId = new AtomicLong(1);

        try {
            server.start();

            for (int i = 0; i < clientsCount; i++) {
                int index = i;
                threads[i] = new Thread(() -> {
                    StoreClientUDP client = null;
                    try {
                        client = createClient(secretKey);
                        for (int j = 0; j < messagesCount; j++) {
                            Package response = client.sendPackage(createPackage(packetId.getAndIncrement(),
                                    MessageType.ADD_PRODUCT, "apple;1"));

                            assertEquals("Ok", response.getMessage().getMessage());
                        }
                    } catch (Exception e) {
                        errors[index] = e;
                    } finally {
                        if (client != null) {
                            client.close();
                        }
                    }
                });

                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            for (Exception error : errors) {
                assertNull(error);
            }

            assertEquals(clientsCount * messagesCount, productStorage.getProductCount("apple"));
        } finally {
            server.stop();
            server.awaitStop();
        }
    }

    @Test
    void shouldNotProcessSameUdpPackageTwice() throws Exception {
        Key secretKey = createKey();
        ProductStorage productStorage = new ProductStorage();
        StoreServerUDP server = new StoreServerUDP(PORT, secretKey, productStorage);
        StoreClientUDP client = null;

        try {
            server.start();
            client = createClient(secretKey);

            Package pack = createPackage(1, MessageType.ADD_PRODUCT, "apple;5");
            Package firstResponse = client.sendPackage(pack);
            Package secondResponse = client.sendPackage(pack);

            assertEquals("Ok", firstResponse.getMessage().getMessage());
            assertEquals("Ok", secondResponse.getMessage().getMessage());
            assertEquals(5, productStorage.getProductCount("apple"));
        } finally {
            if (client != null) {
                client.close();
            }
            server.stop();
            server.awaitStop();
        }
    }

    @Test
    void shouldRepeatUdpRequestWhenServerStartsLater() throws Exception {
        Key secretKey = createKey();
        ProductStorage productStorage = new ProductStorage();
        StoreClientUDP client = createClient(secretKey);
        StoreServerUDP server = new StoreServerUDP(PORT, secretKey, productStorage);
        Package[] response = new Package[1];
        Exception[] error = new Exception[1];

        Thread clientThread = new Thread(() -> {
            try {
                response[0] = client.sendPackage(createPackage(1, MessageType.ADD_PRODUCT, "apple;4"));
            } catch (Exception e) {
                error[0] = e;
            }
        });

        try {
            clientThread.start();
            Thread.sleep(1500);

            server.start();

            clientThread.join(7000);

            assertFalse(clientThread.isAlive());
            assertNull(error[0]);
            assertNotNull(response[0]);
            assertEquals("Ok", response[0].getMessage().getMessage());
            assertEquals(4, productStorage.getProductCount("apple"));
        } finally {
            client.close();
            server.stop();
            server.awaitStop();
        }
    }

    private Key createKey() {
        byte[] keyBytes = "very_strong_key1".getBytes();
        return new SecretKeySpec(keyBytes, "AES");
    }

    private StoreClientUDP createClient(Key secretKey) throws Exception {
        return new StoreClientUDP(InetAddress.getByName("localhost"), PORT,
                new Encoder(secretKey), new Decoder(secretKey));
    }

    private Package createPackage(long packetId, int type, String text) {
        return new Package((byte) 1, packetId, new Message(type, 1, text));
    }
}
