import network.storage.ProductStorage;
import data.Message;
import data.Package;
import network.processor.Processor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.MessageType;

import static org.junit.jupiter.api.Assertions.*;

class ProcessorTest {

    private ProductStorage productStorage;
    private Processor processor;

    @BeforeEach
    void setUp() {
        productStorage = new ProductStorage();
        processor = new Processor(productStorage);
    }

    @Test
    void shouldAddAndTakeProductCorrectly() {
        processor.process(new Package((byte) 1, 1, new Message(MessageType.ADD_PRODUCT, 1, "apple;100")));
        processor.process(new Package((byte) 1, 2, new Message(MessageType.TAKE_PRODUCT, 1, "apple;30")));

        Package actual = processor.process(new Package((byte) 1, 3, new Message(MessageType.GET_PRODUCT_COUNT, 1, "apple")));

        assertEquals(70, productStorage.getProductCount("apple"));
        assertEquals("Ok:70", actual.getMessage().getMessage());
    }

    @Test
    void shouldAddGroupAndProductToGroup() {
        processor.process(new Package((byte) 1, 1, new Message(MessageType.ADD_GROUP, 1, "fruits")));
        processor.process(new Package((byte) 1, 2, new Message(MessageType.ADD_PRODUCT_TO_GROUP, 1, "fruits;apple")));

        assertTrue(productStorage.isProductInGroup("fruits", "apple"));
    }

    @Test
    void shouldSetPrice() {
        Package actual = processor.process(new Package((byte) 1, 1, new Message(MessageType.SET_PRICE, 1, "apple;35.5")));

        assertEquals("Ok", actual.getMessage().getMessage());
        assertEquals(35.5, productStorage.getPrice("apple"));
    }

    @Test
    void shouldReturnErrorWhenNotEnoughProduct() {
        Package actual = processor.process(new Package((byte) 1, 1, new Message(MessageType.TAKE_PRODUCT, 1, "apple;10")));

        assertEquals("Error:Not enough product", actual.getMessage().getMessage());
        assertEquals(0, productStorage.getProductCount("apple"));
    }

    @Test
    void shouldReturnErrorWhenCommandHasWrongArgumentsCount() {
        Package actual = processor.process(new Package((byte) 1, 1, new Message(MessageType.ADD_PRODUCT, 1, "apple")));

        assertEquals("Error:Invalid command", actual.getMessage().getMessage());
    }

    @Test
    void shouldReturnErrorWhenProductCountIsNegative() {
        Package actual = processor.process(new Package((byte) 1, 1, new Message(MessageType.ADD_PRODUCT, 1, "apple;-1")));

        assertEquals("Error:Count cannot be negative", actual.getMessage().getMessage());
        assertEquals(0, productStorage.getProductCount("apple"));
    }

    @Test
    void shouldReturnErrorWhenPriceIsNegative() {
        Package actual = processor.process(new Package((byte) 1, 1, new Message(MessageType.SET_PRICE, 1, "apple;-10")));

        assertEquals("Error:Price cannot be negative", actual.getMessage().getMessage());
        assertEquals(0, productStorage.getPrice("apple"));
    }

    @Test
    void shouldReturnErrorWhenCommandTypeIsUnknown() {
        Package actual = processor.process(new Package((byte) 1, 1, new Message(100, 1, "apple")));

        assertEquals("Error:Unknown command", actual.getMessage().getMessage());
    }

    @Test
    void shouldProcessAddMessagesInManyThreads() throws Exception {
        int threadsCount = 10;
        int messagesCount = 100;
        Thread[] threads = new Thread[threadsCount];

        for (int i = 0; i < threadsCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < messagesCount; j++) {
                    processor.process(new Package((byte) 1, j, new Message(MessageType.ADD_PRODUCT, 1, "apple;1")));
                }
            });

            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(threadsCount * messagesCount, productStorage.getProductCount("apple"));
    }

    @Test
    void shouldProcessAddAndTakeMessagesInManyThreads() throws Exception {
        processor.process(new Package((byte) 1, 1, new Message(MessageType.ADD_PRODUCT, 1, "apple;1000")));

        int addThreadsCount = 5;
        int takeThreadsCount = 5;
        int messagesCount = 100;
        Thread[] addThreads = new Thread[addThreadsCount];
        Thread[] takeThreads = new Thread[takeThreadsCount];

        for (int i = 0; i < addThreadsCount; i++) {
            addThreads[i] = new Thread(() -> {
                for (int j = 0; j < messagesCount; j++) {
                    processor.process(new Package((byte) 1, j, new Message(MessageType.ADD_PRODUCT, 1, "apple;2")));
                }
            });

            addThreads[i].start();
        }

        for (int i = 0; i < takeThreadsCount; i++) {
            takeThreads[i] = new Thread(() -> {
                for (int j = 0; j < messagesCount; j++) {
                    processor.process(new Package((byte) 1, j, new Message(MessageType.TAKE_PRODUCT, 1, "apple;1")));
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

        assertEquals(1500, productStorage.getProductCount("apple"));
    }
}
