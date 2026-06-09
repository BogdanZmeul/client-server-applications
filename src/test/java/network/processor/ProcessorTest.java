package network.processor;

import data.Message;
import data.Package;
import db.service.SqliteProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import service.StoreService;
import utils.MessageType;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProcessorTest {
    @TempDir
    Path tempDir;

    private SqliteProductService productDb;
    private StoreService storeService;
    private Processor processor;

    @BeforeEach
    void setUp() {
        productDb = new SqliteProductService(tempDir.resolve("test.db").toString());
        storeService = new StoreService(productDb);
        processor = new Processor(storeService);
    }

    @AfterEach
    void close() {
        productDb.close();
    }

    @Test
    void shouldAddAndTakeProductCorrectly() {
        int productId = createProduct("apple", 0, 0);
        processor.process(new Package((byte) 1, 1, new Message(MessageType.ADD_PRODUCT, 1, productId + ";100")));
        processor.process(new Package((byte) 1, 2, new Message(MessageType.TAKE_PRODUCT, 1, productId + ";30")));

        Package actual = processor.process(new Package((byte) 1, 3, new Message(MessageType.GET_PRODUCT_COUNT, 1, String.valueOf(productId))));

        assertEquals(70, storeService.getProductQuantity(productId));
        assertEquals("Ok:70", actual.getMessage().getMessage());
    }

    @Test
    void shouldAddGroupAndProductToGroup() {
        int productId = createProduct("apple", 0, 0);
        int groupId = createGroup("fruits");
        processor.process(new Package((byte) 1, 2, new Message(MessageType.ADD_PRODUCT_TO_GROUP, 1, groupId + ";" + productId)));

        assertTrue(storeService.isProductInGroup(groupId, productId));
    }

    @Test
    void shouldSetPrice() {
        int productId = createProduct("apple", 0, 0);

        Package actual = processor.process(new Package((byte) 1, 1, new Message(MessageType.SET_PRICE, 1, productId + ";35.5")));

        assertEquals("Ok", actual.getMessage().getMessage());
        assertEquals(35.5, storeService.getProductPrice(productId));
    }

    @Test
    void shouldReturnErrorWhenNotEnoughProduct() {
        int productId = createProduct("apple", 0, 0);

        Package actual = processor.process(new Package((byte) 1, 1, new Message(MessageType.TAKE_PRODUCT, 1, productId + ";10")));

        assertEquals("Error:Product not found or not enough quantity", actual.getMessage().getMessage());
        assertEquals(0, storeService.getProductQuantity(productId));
    }

    @Test
    void shouldReturnErrorWhenCommandHasWrongArgumentsCount() {
        Package actual = processor.process(new Package((byte) 1, 1, new Message(MessageType.ADD_PRODUCT, 1, "apple")));

        assertEquals("Error:Invalid command", actual.getMessage().getMessage());
    }

    @Test
    void shouldReturnErrorWhenProductCountIsNegative() {
        int productId = createProduct("apple", 0, 0);

        Package actual = processor.process(new Package((byte) 1, 1, new Message(MessageType.ADD_PRODUCT, 1, productId + ";-1")));

        assertEquals("Error:Count cannot be negative", actual.getMessage().getMessage());
        assertEquals(0, storeService.getProductQuantity(productId));
    }

    @Test
    void shouldReturnErrorWhenPriceIsNegative() {
        int productId = createProduct("apple", 0, 0);

        Package actual = processor.process(new Package((byte) 1, 1, new Message(MessageType.SET_PRICE, 1, productId + ";-10")));

        assertEquals("Error:Price cannot be negative", actual.getMessage().getMessage());
        assertEquals(0, storeService.getProductPrice(productId));
    }

    @Test
    void shouldReturnErrorWhenCommandTypeIsUnknown() {
        Package actual = processor.process(new Package((byte) 1, 1, new Message(100, 1, "apple")));

        assertEquals("Error:Unknown command", actual.getMessage().getMessage());
    }

    @Test
    void shouldProcessAddMessagesInManyThreads() throws Exception {
        int productId = createProduct("apple", 0, 0);
        int threadsCount = 10;
        int messagesCount = 100;
        Thread[] threads = new Thread[threadsCount];

        for (int i = 0; i < threadsCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < messagesCount; j++) {
                    processor.process(new Package((byte) 1, j, new Message(MessageType.ADD_PRODUCT, 1, productId + ";1")));
                }
            });

            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(threadsCount * messagesCount, storeService.getProductQuantity(productId));
    }

    @Test
    void shouldProcessAddAndTakeMessagesInManyThreads() throws Exception {
        int productId = createProduct("apple", 0, 0);
        processor.process(new Package((byte) 1, 1, new Message(MessageType.ADD_PRODUCT, 1, productId + ";1000")));

        int addThreadsCount = 5;
        int takeThreadsCount = 5;
        int messagesCount = 100;
        Thread[] addThreads = new Thread[addThreadsCount];
        Thread[] takeThreads = new Thread[takeThreadsCount];

        for (int i = 0; i < addThreadsCount; i++) {
            addThreads[i] = new Thread(() -> {
                for (int j = 0; j < messagesCount; j++) {
                    processor.process(new Package((byte) 1, j, new Message(MessageType.ADD_PRODUCT, 1, productId + ";2")));
                }
            });

            addThreads[i].start();
        }

        for (int i = 0; i < takeThreadsCount; i++) {
            takeThreads[i] = new Thread(() -> {
                for (int j = 0; j < messagesCount; j++) {
                    processor.process(new Package((byte) 1, j, new Message(MessageType.TAKE_PRODUCT, 1, productId + ";1")));
                }
            });

            takeThreads[i].start();
        }

        for (Thread thread : addThreads) {
            thread.join();
        }

        for (Thread thread : takeThreads) {
            thread.join();
        }

        assertEquals(1500, storeService.getProductQuantity(productId));
    }

    private int createProduct(String name, int count, double price) {
        Package answer = processor.process(new Package((byte) 1, 1, new Message(MessageType.CREATE_PRODUCT, 1,
                name + ";" + count + ";" + price)));
        return getId(answer);
    }

    private int createGroup(String name) {
        Package answer = processor.process(new Package((byte) 1, 1, new Message(MessageType.CREATE_GROUP, 1, name)));
        return getId(answer);
    }

    private int getId(Package answer) {
        assertTrue(answer.getMessage().getMessage().startsWith("Ok:"));
        return Integer.parseInt(answer.getMessage().getMessage().substring(3));
    }
}
