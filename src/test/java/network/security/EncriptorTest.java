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

class EncriptorTest {

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
    void shouldEncriptorEncodeMessage() throws Exception {
        Package endPackage = new Package();
        BlockingQueue<Package> input = new LinkedBlockingQueue<>();
        BlockingQueue<byte[]> output = new LinkedBlockingQueue<>();
        Package pack = new Package((byte) 1, 5, new Message(MessageType.ADD_GROUP, 1, "fruits"));

        input.add(pack);
        input.add(endPackage);

        Encriptor encriptor = new Encriptor(encoder, input, output, endPackage);
        encriptor.run();

        byte[] actual = output.poll();

        assertNotNull(actual);
        Package decodedPackage = decoder.decode(actual);
        assertEquals(5, decodedPackage.getbPktId());
        assertEquals(MessageType.ADD_GROUP, decodedPackage.getMessage().getcType());
        assertEquals("fruits", decodedPackage.getMessage().getMessage());
    }

    @Test
    void shouldEncriptorStopWhenFirstPackageIsEndPackage() {
        Package endPackage = new Package();
        BlockingQueue<Package> input = new LinkedBlockingQueue<>();
        BlockingQueue<byte[]> output = new LinkedBlockingQueue<>();

        input.add(endPackage);

        Encriptor encriptor = new Encriptor(encoder, input, output, endPackage);
        encriptor.run();

        assertTrue(output.isEmpty());
    }

    @Test
    void shouldEncriptorEncodeSeveralMessages() throws Exception {
        Package endPackage = new Package();
        BlockingQueue<Package> input = new LinkedBlockingQueue<>();
        BlockingQueue<byte[]> output = new LinkedBlockingQueue<>();
        Package firstPackage = new Package((byte) 1, 1, new Message(MessageType.ADD_PRODUCT, 1, "apple;10"));
        Package secondPackage = new Package((byte) 1, 2, new Message(MessageType.SET_PRICE, 1, "apple;12.5"));

        input.add(firstPackage);
        input.add(secondPackage);
        input.add(endPackage);

        Encriptor encriptor = new Encriptor(encoder, input, output, endPackage);
        encriptor.run();

        Package firstActual = decoder.decode(output.poll());
        Package secondActual = decoder.decode(output.poll());

        assertEquals(1, firstActual.getbPktId());
        assertEquals(2, secondActual.getbPktId());
        assertEquals("apple;10", firstActual.getMessage().getMessage());
        assertEquals("apple;12.5", secondActual.getMessage().getMessage());
    }
}
