import java.nio.ByteBuffer;

public class Decoder {
    public Package decode(byte[] data){
        Package pack = new Package();

        ByteBuffer bytes = ByteBuffer.wrap(data);
        bytes.get();

        pack.setbSrc(bytes.get());
        pack.setbPktId(bytes.getLong());

        int messageLength = bytes.getInt();

        short crc16Header = Crc16.calculateCrc(data, 0, 14);
        if (crc16Header != bytes.getShort()) {
            throw new RuntimeException("Header CRC16 error");
        }

        pack.setMessage(new Message(bytes.getInt(), bytes.getInt(), new String(bytes.array(), 24, messageLength-4-4)));

        //TODO: check CRC16
        return pack;
    }
}
