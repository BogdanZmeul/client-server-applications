package network.communication.receiver;

import network.protocol.NetworkReceiver;
import java.util.concurrent.BlockingQueue;

public class Receiver implements Runnable {
    private final NetworkReceiver networkReceiver;
    private final BlockingQueue<byte[]> output;

    public Receiver(NetworkReceiver networkReceiver, BlockingQueue<byte[]> output) {
        this.networkReceiver = networkReceiver;
        this.output = output;
    }

    @Override
    public void run() {
        System.out.println("Receiver started");
        try {
            receiveMessage();
        } finally {
            System.out.println("Receiver finished");
        }
    }

    public void receiveMessage() {
        try {
            while (true) {
                byte[] data = networkReceiver.receiveMessage();
                if (data == null) {
                    break;
                }

                output.put(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
