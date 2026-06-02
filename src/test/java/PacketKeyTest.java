import cipher.Encoder;
import data.Message;
import data.Package;
import org.junit.jupiter.api.Test;
import utils.MessageType;
import utils.PacketKey;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PacketKeyTest {
    @Test
    void shouldCreateKeyFromPacketSourceAndId() throws Exception {
        Key secretKey = createKey();
        Encoder encoder = new Encoder(secretKey);
        Package pack = new Package((byte) 7, 25, new Message(MessageType.ADD_PRODUCT, 1, "apple;3"));

        byte[] data = encoder.encode(pack);

        assertEquals("7:25", PacketKey.createKey(data));
    }

    private Key createKey() {
        byte[] keyBytes = "very_strong_key1".getBytes();
        return new SecretKeySpec(keyBytes, "AES");
    }
}
