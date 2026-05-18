import data.Package;
import utils.Crc16;

import javax.crypto.Cipher;
import java.nio.ByteBuffer;
import java.security.Key;

public class Encoder {

    private final Key secretKey;

    public Encoder(Key secretKey) {
        this.secretKey = secretKey;
    }

    public byte[] encode(Package pack) throws Exception {
        if (pack == null)
            throw new NullPointerException("Package cannot be null");

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        if (pack.getMessage() == null)
            throw new NullPointerException("Message cannot be null");

        byte[] origMessage = pack.getMessage().getMessage().getBytes();
        byte[] encryptedMessage = cipher.doFinal(origMessage);

        int messageLength = encryptedMessage.length;

        ByteBuffer bytes = ByteBuffer.allocate(1+1+8+4+2+4+4+messageLength+2);
        bytes.put((byte) 0x13);
        bytes.put(pack.getbSrc());
        bytes.putLong(pack.getbPktId());
        bytes.putInt(4+4+messageLength);

        bytes.putShort(Crc16.calculateCrc(bytes.array(), 0, bytes.position()));

        bytes.putInt(pack.getMessage().getcType());
        bytes.putInt(pack.getMessage().getbUserId());

        bytes.put(encryptedMessage);

        bytes.putShort(Crc16.calculateCrc(bytes.array(), 16, 4+4+messageLength));

        return bytes.array();
    }
}