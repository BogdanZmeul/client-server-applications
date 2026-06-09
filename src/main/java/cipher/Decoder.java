package cipher;

import data.Message;
import data.Package;
import utils.Crc16;

import javax.crypto.Cipher;
import java.nio.ByteBuffer;
import java.security.Key;

public class Decoder {

    private final Key secretKey;

    public Decoder(Key secretKey) {
        this.secretKey = secretKey;
    }

    public data.Package decode(byte[] data) throws Exception {
        if(data == null || data.length < 26) {
            throw new DecoderException("Invalid data");
        }

        data.Package pack = new Package();

        ByteBuffer bytes = ByteBuffer.wrap(data);

        if (bytes.get() != 0x13) {
            throw new DecoderException("Invalid bMagic byte");
        }

        pack.setbSrc(bytes.get());
        pack.setbPktId(bytes.getLong());

        int messageLength = bytes.getInt();

        short crc16Header = Crc16.calculateCrc(data, 0, 14);
        if (crc16Header != bytes.getShort()) {
            throw new DecoderException("Header CRC16 error");
        }

        int cType = bytes.getInt();
        int bUserId = bytes.getInt();

        byte[] message = new byte[messageLength-4-4];
        bytes.get(message);

        short crc16Message = Crc16.calculateCrc(data, 16, messageLength);
        if (crc16Message != bytes.getShort()) {
            throw new DecoderException("Message CRC16 error");
        }

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedMessage = cipher.doFinal(message);

        pack.setMessage(new Message(cType, bUserId, new String(decryptedMessage)));

        return pack;
    }
}
