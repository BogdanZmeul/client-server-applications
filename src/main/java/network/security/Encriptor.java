package network.security;

import cipher.Encoder;
import data.Package;
import java.util.concurrent.BlockingQueue;

public class Encriptor implements Runnable {
    private final Encoder encoder;
    private final BlockingQueue<Package> input;
    private final BlockingQueue<byte[]> output;
    private final Package endPackage;

    public Encriptor(Encoder encoder, BlockingQueue<Package> input,
                     BlockingQueue<byte[]> output, Package endPackage) {
        this.encoder = encoder;
        this.input = input;
        this.output = output;
        this.endPackage = endPackage;
    }

    @Override
    public void run() {
        System.out.println("Encriptor started");
        try {
            while (true) {
                Package pack = input.take();
                if (pack == endPackage) {
                    break;
                }

                try {
                    output.put(encrypt(pack));
                } catch (Exception e) {
                    System.out.println("Encriptor error: " + e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            System.out.println("Encriptor finished");
        }
    }

    public byte[] encrypt(Package message) throws Exception {
        return encoder.encode(message);
    }
}
