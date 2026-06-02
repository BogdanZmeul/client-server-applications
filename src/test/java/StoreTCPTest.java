import cipher.Decoder;
import cipher.Encoder;
import data.Message;
import data.Package;
import network.storage.ProductStorage;
import network.tcp.client.StoreClientTCP;
import network.tcp.server.StoreServerTCP;
import org.junit.jupiter.api.Test;
import utils.MessageType;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.security.Key;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class StoreTCPTest {
    private static final int PORT = StoreServerTCP.PORT;

    @Test
    void shouldSendMessageToServerAndReceiveResponse() throws Exception {
        Key secretKey = createKey();
        ProductStorage productStorage = new ProductStorage();
        StoreServerTCP server = new StoreServerTCP(PORT, secretKey, productStorage,
                1, 1, 1, 1, 1);
        StoreClientTCP client = null;

        try {
            server.start();
            client = createClient(secretKey);
            client.start();

            client.sendPackage(createPackage(1, MessageType.ADD_PRODUCT, "apple;7"));
            client.sendPackage(createPackage(2, MessageType.GET_PRODUCT_COUNT, "apple"));

            Package firstResponse = client.receiveResponse(3000);
            Package secondResponse = client.receiveResponse(3000);

            assertNotNull(firstResponse);
            assertNotNull(secondResponse);
            assertEquals("Ok", firstResponse.getMessage().getMessage());
            assertEquals("Ok:7", secondResponse.getMessage().getMessage());
        } finally {
            if (client != null) {
                client.stop();
                client.awaitStop();
            }
            server.stop();
            server.awaitStop();
        }
    }

    @Test
    void shouldProcessAllCommandTypesOverTcp() throws Exception {
        Key secretKey = createKey();
        ProductStorage productStorage = new ProductStorage();
        StoreServerTCP server = new StoreServerTCP(PORT, secretKey, productStorage,
                2, 2, 2, 2, 2);
        StoreClientTCP client = null;

        try {
            server.start();
            client = createClient(secretKey);
            client.start();

            client.sendPackage(createPackage(1, MessageType.ADD_GROUP, "fruits"));
            client.sendPackage(createPackage(2, MessageType.ADD_PRODUCT_TO_GROUP, "fruits;apple"));
            client.sendPackage(createPackage(3, MessageType.ADD_PRODUCT, "apple;10"));
            client.sendPackage(createPackage(4, MessageType.TAKE_PRODUCT, "apple;4"));
            client.sendPackage(createPackage(5, MessageType.SET_PRICE, "apple;12.5"));
            client.sendPackage(createPackage(6, MessageType.GET_PRODUCT_COUNT, "apple"));

            assertEquals("Ok", client.receiveResponse(3000).getMessage().getMessage());
            assertEquals("Ok", client.receiveResponse(3000).getMessage().getMessage());
            assertEquals("Ok", client.receiveResponse(3000).getMessage().getMessage());
            assertEquals("Ok", client.receiveResponse(3000).getMessage().getMessage());
            assertEquals("Ok", client.receiveResponse(3000).getMessage().getMessage());
            assertEquals("Ok:6", client.receiveResponse(3000).getMessage().getMessage());

            assertTrue(productStorage.isProductInGroup("fruits", "apple"));
            assertEquals(6, productStorage.getProductCount("apple"));
            assertEquals(12.5, productStorage.getPrice("apple"));
        } finally {
            if (client != null) {
                client.stop();
                client.awaitStop();
            }
            server.stop();
            server.awaitStop();
        }
    }

    @Test
    void shouldReturnErrorResponseOverTcp() throws Exception {
        Key secretKey = createKey();
        ProductStorage productStorage = new ProductStorage();
        StoreServerTCP server = new StoreServerTCP(PORT, secretKey, productStorage,
                1, 1, 1, 1, 1);
        StoreClientTCP client = null;

        try {
            server.start();
            client = createClient(secretKey);
            client.start();

            client.sendPackage(createPackage(1, MessageType.TAKE_PRODUCT, "apple;3"));

            Package response = client.receiveResponse(3000);

            assertNotNull(response);
            assertEquals("Error:Not enough product", response.getMessage().getMessage());
            assertEquals(0, productStorage.getProductCount("apple"));
        } finally {
            if (client != null) {
                client.stop();
                client.awaitStop();
            }
            server.stop();
            server.awaitStop();
        }
    }

    @Test
    void shouldProcessMessagesFromManyTcpClients() throws Exception {
        Key secretKey = createKey();
        ProductStorage productStorage = new ProductStorage();
        StoreServerTCP server = new StoreServerTCP(PORT, secretKey, productStorage,
                2, 2, 4, 3, 5);
        StoreClientTCP[] clients = new StoreClientTCP[100];
        int messagesCount = 20;
        AtomicLong packetId = new AtomicLong(1);

        try {
            server.start();

            for (int i = 0; i < clients.length; i++) {
                clients[i] = createClient(secretKey);
                clients[i].start();
            }

            for (StoreClientTCP client : clients) {
                for (int i = 0; i < messagesCount; i++) {
                    client.sendPackage(createPackage(packetId.getAndIncrement(),
                            MessageType.ADD_PRODUCT, "apple;1"));
                }
            }

            for (StoreClientTCP client : clients) {
                for (int i = 0; i < messagesCount; i++) {
                    Package response = client.receiveResponse(5000);

                    assertNotNull(response);
                    assertEquals("Ok", response.getMessage().getMessage());
                }
            }

            assertEquals(clients.length * messagesCount, productStorage.getProductCount("apple"));
        } finally {
            for (StoreClientTCP client : clients) {
                if (client != null) {
                    client.stop();
                    client.awaitStop();
                }
            }
            server.stop();
            server.awaitStop();
        }
    }

    @Test
    void shouldWaitAndSendPackageWhenServerStartsLater() throws Exception {
        Key secretKey = createKey();
        ProductStorage productStorage = new ProductStorage();
        StoreClientTCP client = createClient(secretKey);
        StoreServerTCP server = new StoreServerTCP(PORT, secretKey, productStorage,
                1, 1, 1, 1, 1);

        try {
            client.start();
            client.sendPackage(createPackage(1, MessageType.ADD_PRODUCT, "apple;3"));

            Thread.sleep(2000);

            assertFalse(client.getIsConnected());
            assertEquals(1, client.getPackagesToSendCount());

            server.start();

            Package response = client.receiveResponse(5000);

            assertNotNull(response);
            assertEquals("Ok", response.getMessage().getMessage());
            assertEquals(3, productStorage.getProductCount("apple"));
        } finally {
            client.stop();
            client.awaitStop();
            server.stop();
            server.awaitStop();
        }
    }

    private Key createKey() {
        byte[] keyBytes = "very_strong_key1".getBytes();
        return new SecretKeySpec(keyBytes, "AES");
    }

    private StoreClientTCP createClient(Key secretKey) throws Exception {
        return new StoreClientTCP(InetAddress.getByName("localhost"), StoreTCPTest.PORT,
                new Encoder(secretKey), new Decoder(secretKey));
    }

    private Package createPackage(long packetId, int type, String text) {
        return new Package((byte) 1, packetId, new Message(type, 1, text));
    }
}
