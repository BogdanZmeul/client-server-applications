import cipher.Decoder;
import data.Package;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

import static org.junit.jupiter.api.Assertions.*;

class DecoderTest {

    private Decoder decoder;
    private byte[] validPacket;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = "very_strong_key1".getBytes();
        Key secretKey = new SecretKeySpec(keyBytes, "AES");

        decoder = new Decoder(secretKey);

        validPacket = new byte[]{
                0x13, 0x01, 0x00 ,0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x02, 0x00, 0x00, 0x00, 0x18, (byte)0x98, 0x69, 0x00,
                0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x06, (byte) 0xDA, 0x42, 0x54, 0x5B,
                (byte)0xA3, (byte)0xC8, 0x78, 0x4B, (byte)0xC9, (byte)0xF7, 0x39,
                0x68, 0x23, (byte)0xEA, (byte)0xC3, (byte)0xAF, (byte)0x8C, (byte)0xA4
        };
    }

    @Test
    void shouldDecodeMatchesExpectedPackage() throws Exception {
        Package actual = decoder.decode(validPacket);

        assertEquals((byte) 1, actual.getbSrc());
        assertEquals(2, actual.getbPktId());
        assertEquals(5, actual.getMessage().getcType());
        assertEquals(6, actual.getMessage().getbUserId());
        assertEquals("test", actual.getMessage().getMessage());
    }

    @Test
    void shouldDecodeThrowsExceptionWhenDataIsNull() {
        assertThrows(RuntimeException.class, () -> decoder.decode(null));
    }

    @Test
    void shouldDecodeThrowsExceptionWhenDataIsTooShort() {
        byte[] shortData = new byte[10];

        assertThrows(RuntimeException.class, () -> decoder.decode(shortData));
    }

    @Test
    void shouldDecodeThrowsExceptionWhenMagicByteIsInvalid() {
        validPacket[0] = 0x00;

        assertThrows(RuntimeException.class, () -> decoder.decode(validPacket));
    }

    @Test
    void shouldDecodeThrowsExceptionWhenHeaderCrcIsInvalid() {
        validPacket[1] = 0x05;

        assertThrows(RuntimeException.class, () -> decoder.decode(validPacket));
    }

    @Test
    void shouldDecodeThrowsExceptionWhenMessageCrcIsInvalid() {
        validPacket[16] = 0x01;

        assertThrows(RuntimeException.class, () -> decoder.decode(validPacket));
    }
}