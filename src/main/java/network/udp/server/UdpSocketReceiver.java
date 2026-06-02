package network.udp.server;

import network.udp.communication.UdpReceiverAdapter;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class UdpSocketReceiver extends Thread {
    private static final int BUFFER_SIZE = 65507;

    private final DatagramSocket socket;
    private final AtomicBoolean isRunning;
    private final UdpReceiverAdapter receiverAdapter;

    public UdpSocketReceiver(DatagramSocket socket, AtomicBoolean isRunning,
                             UdpReceiverAdapter receiverAdapter) {
        this.socket = socket;
        this.isRunning = isRunning;
        this.receiverAdapter = receiverAdapter;
    }

    @Override
    public void run() {
        System.out.println("StoreServerUDP started");
        try {
            while (isRunning.get()) {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);

                byte[] data = Arrays.copyOf(request.getData(), request.getLength());
                receiverAdapter.addPacket(data, request.getAddress(), request.getPort());
            }
        } catch (Exception e) {
            if (isRunning.get()) {
                System.out.println("StoreServerUDP error: " + e.getMessage());
            }
        } finally {
            isRunning.set(false);
            socket.close();
            System.out.println("StoreServerUDP finished");
        }
    }
}
