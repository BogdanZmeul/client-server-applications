package network.udp.client;

import cipher.Decoder;
import cipher.Encoder;
import data.Message;
import data.Package;
import network.udp.server.StoreServerUDP;
import utils.MessageType;

import javax.crypto.spec.SecretKeySpec;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.security.Key;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class StoreClientUDP {
    private static final int BUFFER_SIZE = 65507;
    private static final int TIMEOUT = 1000;

    private final InetAddress address;
    private final int port;
    private final Encoder encoder;
    private final Decoder decoder;
    private final DatagramSocket socket;

    public StoreClientUDP(InetAddress address, int port, Encoder encoder, Decoder decoder) throws Exception {
        this.address = address;
        this.port = port;
        this.encoder = encoder;
        this.decoder = decoder;
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(TIMEOUT);
    }

    public Package sendPackage(Package pack) throws Exception {
        byte[] request = encoder.encode(pack);
        DatagramPacket requestPacket = new DatagramPacket(request, request.length, address, port);

        while (true) {
            try {
                socket.send(requestPacket);

                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(responsePacket);

                byte[] response = Arrays.copyOf(responsePacket.getData(), responsePacket.getLength());

                return decoder.decode(response);
            } catch (SocketTimeoutException e) {
                System.out.println("UDP response timeout. Repeat request...");
            }
        }
    }

    public void close() {
        socket.close();
    }

    public static void main(String[] args) throws Exception {
        byte[] keyBytes = "very_strong_key1".getBytes();
        Key secretKey = new SecretKeySpec(keyBytes, "AES");
        InetAddress address = InetAddress.getByName("localhost");
        AtomicLong packetId = new AtomicLong(1);

        StoreClientUDP client = new StoreClientUDP(address, StoreServerUDP.PORT,
                new Encoder(secretKey), new Decoder(secretKey));

        Package response = client.sendPackage(new Package((byte) 1, packetId.getAndIncrement(),
                new Message(MessageType.ADD_PRODUCT, 1, "apple;10")));

        System.out.println("Response: " + response.getMessage().getMessage());
        client.close();
    }
}
