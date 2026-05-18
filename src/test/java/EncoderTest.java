import data.Message;
import data.Package;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

import static org.junit.jupiter.api.Assertions.*;

class EncoderTest {

    private Encoder encoder;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = "very_strong_key1".getBytes();
        Key secretKey = new SecretKeySpec(keyBytes, "AES");

        encoder = new Encoder(secretKey);
    }

    @Test
    void shouldEncodeMatchesExpectedByteArray() throws Exception {
        byte[] expected = {
                0x13, 0x01, 0x00 ,0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x02, 0x00, 0x00, 0x00, 0x18, (byte)0x98, 0x69, 0x00,
                0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x06, (byte) 0xDA, 0x42, 0x54, 0x5B,
                (byte)0xA3, (byte)0xC8, 0x78, 0x4B, (byte)0xC9, (byte)0xF7, 0x39,
                0x68, 0x23, (byte)0xEA, (byte)0xC3, (byte)0xAF, (byte)0x8C, (byte)0xA4
        };

        data.Package pack = new data.Package((byte) 1, 2, new Message(5, 6, "test"));

        byte[] actual = encoder.encode(pack);

        assertArrayEquals(expected, actual);
    }

    @Test
    void shouldEncodeThrowsExceptionWhenPackageIsNull() {
        assertThrows(NullPointerException.class, () -> encoder.encode(null));
    }

    @Test
    void shouldEncodeThrowsExceptionWhenMessageIsNull() {
        data.Package pack = new data.Package((byte) 1, 2, null);

        assertThrows(NullPointerException.class, () -> encoder.encode(pack));
    }

    @Test
    void shouldEncodeWorkWhenMessageIsEmpty() throws Exception {
        data.Package pack = new data.Package((byte) 1, 2, new Message(5, 6, ""));
        encoder.encode(pack);
    }

    @Test
    void shouldEncodeHaveCorrectMagicByteAtStart() throws Exception {
        data.Package pack = new data.Package((byte) 5, 100, new Message(1, 2, "checking magic byte"));

        byte[] actual = encoder.encode(pack);

        assertEquals((byte) 0x13, actual[0]);
    }

    @Test
    void shouldEncodeHandleLargeMessageCorrectly() throws Exception {
        StringBuilder largeMessage = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeMessage.append("a");
        }

        data.Package pack = new Package((byte) 1, 2, new Message(5, 6, largeMessage.toString()));

        byte[] actual = encoder.encode(pack);

        assertTrue(actual.length > 1000);
    }

}