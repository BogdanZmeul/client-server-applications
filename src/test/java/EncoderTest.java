import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncoderTest {
    @Test
    void testEncode() {
        Package p = new Package((byte) 1, 2, new Message(3, 4, "test"));
        Encoder e = new Encoder();
        byte[] bytes = e.encode(p);

        assertEquals("130100000000000000020000000c9769000000030000000474657374c8cb", Hex.encodeHexString(bytes));
    }
}