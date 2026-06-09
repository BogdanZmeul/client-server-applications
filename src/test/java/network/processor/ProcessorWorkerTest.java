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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

class ProcessorWorkerTest {
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
    void shouldProcessorWorkerProcessMessage() {
        int productId = createProduct("apple", 0, 0);
        Package endPackage = new Package();
        BlockingQueue<Package> input = new LinkedBlockingQueue<>();
        BlockingQueue<Package> output = new LinkedBlockingQueue<>();

        input.add(new Package((byte) 1, 1, new Message(MessageType.ADD_PRODUCT, 1, productId + ";10")));
        input.add(endPackage);

        ProcessorWorker processorWorker = new ProcessorWorker(processor, input, output, endPackage);
        processorWorker.run();

        Package actual = output.poll();

        assertEquals(10, storeService.getProductQuantity(productId));
        assertNotNull(actual);
        assertEquals("Ok", actual.getMessage().getMessage());
    }

    @Test
    void shouldProcessorWorkerStopWhenFirstPackageIsEndPackage() {
        Package endPackage = new Package();
        BlockingQueue<Package> input = new LinkedBlockingQueue<>();
        BlockingQueue<Package> output = new LinkedBlockingQueue<>();

        input.add(endPackage);

        ProcessorWorker processorWorker = new ProcessorWorker(processor, input, output, endPackage);
        processorWorker.run();

        assertTrue(output.isEmpty());
    }

    @Test
    void shouldProcessorWorkerProcessSeveralPackages() {
        int productId = createProduct("apple", 0, 0);
        Package endPackage = new Package();
        BlockingQueue<Package> input = new LinkedBlockingQueue<>();
        BlockingQueue<Package> output = new LinkedBlockingQueue<>();

        input.add(new Package((byte) 1, 1, new Message(MessageType.ADD_PRODUCT, 1, productId + ";10")));
        input.add(new Package((byte) 1, 2, new Message(MessageType.ADD_PRODUCT, 1, productId + ";5")));
        input.add(new Package((byte) 1, 3, new Message(MessageType.TAKE_PRODUCT, 1, productId + ";3")));
        input.add(endPackage);

        ProcessorWorker processorWorker = new ProcessorWorker(processor, input, output, endPackage);
        processorWorker.run();

        assertEquals(12, storeService.getProductQuantity(productId));
        assertEquals(3, output.size());
    }

    @Test
    void shouldProcessorWorkerCreateErrorResponse() {
        int productId = createProduct("apple", 0, 0);
        Package endPackage = new Package();
        BlockingQueue<Package> input = new LinkedBlockingQueue<>();
        BlockingQueue<Package> output = new LinkedBlockingQueue<>();

        input.add(new Package((byte) 1, 1, new Message(MessageType.TAKE_PRODUCT, 1, productId + ";10")));
        input.add(endPackage);

        ProcessorWorker processorWorker = new ProcessorWorker(processor, input, output, endPackage);
        processorWorker.run();

        Package actual = output.poll();

        assertNotNull(actual);
        assertEquals("Error:Product not found or not enough quantity", actual.getMessage().getMessage());
    }

    private int createProduct(String name, int count, double price) {
        Package answer = processor.process(new Package((byte) 1, 1, new Message(MessageType.CREATE_PRODUCT, 1,
                name + ";" + count + ";" + price)));
        assertTrue(answer.getMessage().getMessage().startsWith("Ok:"));
        return Integer.parseInt(answer.getMessage().getMessage().substring(3));
    }
}
