package network.security;

import cipher.Decoder;
import data.Package;

import java.util.concurrent.BlockingQueue;

public class Decriptor implements Runnable {
    private final Decoder decoder;
    private final BlockingQueue<byte[]> input;
    private final BlockingQueue<Package> output;
    private final byte[] endPacket;

    public Decriptor(Decoder decoder, BlockingQueue<byte[]> input,
                     BlockingQueue<Package> output, byte[] endPacket) {
        this.decoder = decoder;
        this.input = input;
        this.output = output;
        this.endPacket = endPacket;
    }

    @Override
    public void run() {
        System.out.println("Decriptor started");
        try {
            while (true) {
                byte[] data = input.take();
                if (data == endPacket) {
                    break;
                }

                decrypt(data);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            System.out.println("Decriptor finished");
        }
    }

    public void decrypt(byte[] message) {
        try {
            output.put(decoder.decode(message));
        } catch (Exception e) {
            System.out.println("Decriptor error: " + e.getMessage());
        }
    }
}
