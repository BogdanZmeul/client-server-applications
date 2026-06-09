package network.communication.receiver;

import cipher.Decoder;
import cipher.Encoder;
import data.Package;
import network.communication.receiver.FakeNetworkReceiver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

import static org.junit.jupiter.api.Assertions.*;

class FakeNetworkReceiverTest {

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
    void shouldFakeNetworkReceiverGenerateLimitedMessages() throws Exception {
        FakeNetworkReceiver receiver = new FakeNetworkReceiver(encoder, 100);

        for (int i = 0; i < 100; i++) {
            byte[] data = receiver.receiveMessage();
            Package pack = decoder.decode(data);

            assertNotNull(data);
            assertTrue(pack.getMessage().getcType() >= 1);
            assertTrue(pack.getMessage().getcType() <= 6);
        }

        assertNull(receiver.receiveMessage());
    }

    @Test
    void shouldFakeNetworkReceiverReturnNullWhenMessagesLimitIsZero() throws Exception {
        FakeNetworkReceiver receiver = new FakeNetworkReceiver(encoder, 0);

        assertNull(receiver.receiveMessage());
    }

    @Test
    void shouldFakeNetworkReceiverGenerateSequentialPacketIds() throws Exception {
        FakeNetworkReceiver receiver = new FakeNetworkReceiver(encoder, 3);

        Package firstPackage = decoder.decode(receiver.receiveMessage());
        Package secondPackage = decoder.decode(receiver.receiveMessage());
        Package thirdPackage = decoder.decode(receiver.receiveMessage());

        assertEquals(1, firstPackage.getbPktId());
        assertEquals(2, secondPackage.getbPktId());
        assertEquals(3, thirdPackage.getbPktId());
    }
}
