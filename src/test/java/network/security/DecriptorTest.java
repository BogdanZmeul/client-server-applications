package network.security;

import cipher.Decoder;
import cipher.Encoder;
import data.Message;
import data.Package;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.MessageType;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

class DecriptorTest {

    private Encoder encoder;
    private Decoder decoder;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = "very_strong_key1".getBytes();
        Key secretKey = new SecretKeySpec(keyBytes, "AES");

        encoder = new Encoder(secretKey);
        decoder = new Decoder(secretKey);
    }

    @Test
    void shouldDecriptorDecodeMessage() throws Exception {
        byte[] endPacket = new byte[0];
        BlockingQueue<byte[]> input = new LinkedBlockingQueue<>();
        BlockingQueue<Package> output = new LinkedBlockingQueue<>();
        Package pack = new Package((byte) 1, 10, new Message(MessageType.ADD_PRODUCT, 1, "1;5"));

        input.add(encoder.encode(pack));
        input.add(endPacket);

        Decriptor decriptor = new Decriptor(decoder, input, output, endPacket);
        decriptor.run();

        Package actual = output.poll();

        assertNotNull(actual);
        assertEquals(10, actual.getbPktId());
        assertEquals(MessageType.ADD_PRODUCT, actual.getMessage().getcType());
        assertEquals("1;5", actual.getMessage().getMessage());
    }

    @Test
    void shouldDecriptorStopWhenFirstPacketIsEndPacket() {
        byte[] endPacket = new byte[0];
        BlockingQueue<byte[]> input = new LinkedBlockingQueue<>();
        BlockingQueue<Package> output = new LinkedBlockingQueue<>();

        input.add(endPacket);

        Decriptor decriptor = new Decriptor(decoder, input, output, endPacket);
        decriptor.run();

        assertTrue(output.isEmpty());
    }

    @Test
    void shouldDecriptorDecodeSeveralMessages() throws Exception {
        byte[] endPacket = new byte[0];
        BlockingQueue<byte[]> input = new LinkedBlockingQueue<>();
        BlockingQueue<Package> output = new LinkedBlockingQueue<>();
        Package firstPackage = new Package((byte) 1, 1, new Message(MessageType.ADD_PRODUCT, 1, "1;5"));
        Package secondPackage = new Package((byte) 1, 2, new Message(MessageType.ADD_GROUP, 1, "fruits"));

        input.add(encoder.encode(firstPackage));
        input.add(encoder.encode(secondPackage));
        input.add(endPacket);

        Decriptor decriptor = new Decriptor(decoder, input, output, endPacket);
        decriptor.run();

        assertEquals(2, output.size());

        Package firstActual = output.poll();
        Package secondActual = output.poll();

        assertNotNull(firstActual);
        assertNotNull(secondActual);
        assertEquals("1;5", firstActual.getMessage().getMessage());
        assertEquals("fruits", secondActual.getMessage().getMessage());
    }
}
