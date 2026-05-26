package network.communication.sender;

import network.protocol.NetworkSender;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;

public class Sender implements Runnable {
    private final NetworkSender networkSender;
    private final BlockingQueue<byte[]> input;
    private final byte[] endPacket;
    private final InetAddress target;

    public Sender(NetworkSender networkSender, BlockingQueue<byte[]> input, byte[] endPacket,
                  InetAddress target) {
        this.networkSender = networkSender;
        this.input = input;
        this.endPacket = endPacket;
        this.target = target;
    }

    @Override
    public void run() {
        System.out.println("Sender started");
        try {
            while (true) {
                byte[] data = input.take();
                if (data == endPacket) {
                    break;
                }

                sendMessage(data, target);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.out.println("Sender error: " + e.getMessage());
        } finally {
            System.out.println("Sender finished");
        }
    }

    public void sendMessage(byte[] message, InetAddress target) throws Exception {
        networkSender.sendMessage(message, target);
    }
}
