import java.nio.ByteBuffer;

public class Encoder {

    public byte[] encode(Package pack){
        int messageLength = pack.getMessage().getMessage().getBytes().length;

        ByteBuffer bytes = ByteBuffer.allocate(1+1+8+4+2+4+4+messageLength+2);
        bytes.put((byte) 0x13);
        bytes.put(pack.getbSrc());
        bytes.putLong(pack.getbPktId());
        bytes.putInt(4+4+messageLength);

        bytes.putShort(Crc16.calculateCrc(bytes.array(), 0, bytes.position()));

        bytes.putInt(pack.getMessage().getcType());
        bytes.putInt(pack.getMessage().getbUserId());
        bytes.put(pack.getMessage().getMessage().getBytes());
        bytes.putShort(Crc16.calculateCrc(bytes.array(), 16, 4+4+messageLength));
        return bytes.array();
    }

}
