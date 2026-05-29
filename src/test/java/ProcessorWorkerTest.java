import data.Message;
import data.Package;
import network.processor.Processor;
import network.processor.ProcessorWorker;
import network.storage.ProductStorage;
import org.junit.jupiter.api.Test;
import utils.MessageType;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

class ProcessorWorkerTest {

    @Test
    void shouldProcessorWorkerProcessMessage() {
        Package endPackage = new Package();
        BlockingQueue<Package> input = new LinkedBlockingQueue<>();
        BlockingQueue<Package> output = new LinkedBlockingQueue<>();
        ProductStorage productStorage = new ProductStorage();
        Processor processor = new Processor(productStorage);

        input.add(new Package((byte) 1, 1, new Message(MessageType.ADD_PRODUCT, 1, "apple;10")));
        input.add(endPackage);

        ProcessorWorker processorWorker = new ProcessorWorker(processor, input, output, endPackage);
        processorWorker.run();

        Package actual = output.poll();

        assertEquals(10, productStorage.getProductCount("apple"));
        assertNotNull(actual);
        assertEquals("Ok", actual.getMessage().getMessage());
    }

    @Test
    void shouldProcessorWorkerStopWhenFirstPackageIsEndPackage() {
        Package endPackage = new Package();
        BlockingQueue<Package> input = new LinkedBlockingQueue<>();
        BlockingQueue<Package> output = new LinkedBlockingQueue<>();
        ProductStorage productStorage = new ProductStorage();
        Processor processor = new Processor(productStorage);

        input.add(endPackage);

        ProcessorWorker processorWorker = new ProcessorWorker(processor, input, output, endPackage);
        processorWorker.run();

        assertTrue(output.isEmpty());
    }

    @Test
    void shouldProcessorWorkerProcessSeveralPackages() {
        Package endPackage = new Package();
        BlockingQueue<Package> input = new LinkedBlockingQueue<>();
        BlockingQueue<Package> output = new LinkedBlockingQueue<>();
        ProductStorage productStorage = new ProductStorage();
        Processor processor = new Processor(productStorage);

        input.add(new Package((byte) 1, 1, new Message(MessageType.ADD_PRODUCT, 1, "apple;10")));
        input.add(new Package((byte) 1, 2, new Message(MessageType.ADD_PRODUCT, 1, "apple;5")));
        input.add(new Package((byte) 1, 3, new Message(MessageType.TAKE_PRODUCT, 1, "apple;3")));
        input.add(endPackage);

        ProcessorWorker processorWorker = new ProcessorWorker(processor, input, output, endPackage);
        processorWorker.run();

        assertEquals(12, productStorage.getProductCount("apple"));
        assertEquals(3, output.size());
    }

    @Test
    void shouldProcessorWorkerCreateErrorResponse() {
        Package endPackage = new Package();
        BlockingQueue<Package> input = new LinkedBlockingQueue<>();
        BlockingQueue<Package> output = new LinkedBlockingQueue<>();
        ProductStorage productStorage = new ProductStorage();
        Processor processor = new Processor(productStorage);

        input.add(new Package((byte) 1, 1, new Message(MessageType.TAKE_PRODUCT, 1, "apple;10")));
        input.add(endPackage);

        ProcessorWorker processorWorker = new ProcessorWorker(processor, input, output, endPackage);
        processorWorker.run();

        Package actual = output.poll();

        assertNotNull(actual);
        assertEquals("Error:Not enough product", actual.getMessage().getMessage());
    }
}
