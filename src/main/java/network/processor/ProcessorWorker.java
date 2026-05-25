package network.processor;

import data.Package;

import java.util.concurrent.BlockingQueue;

public class ProcessorWorker implements Runnable {
    private final Processor processor;
    private final BlockingQueue<Package> input;
    private final BlockingQueue<Package> output;
    private final Package endPackage;

    public ProcessorWorker(Processor processor, BlockingQueue<Package> input,
                           BlockingQueue<Package> output, Package endPackage) {
        this.processor = processor;
        this.input = input;
        this.output = output;
        this.endPackage = endPackage;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Package pack = input.take();
                if (pack == endPackage) {
                    break;
                }

                output.put(processor.process(pack));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
