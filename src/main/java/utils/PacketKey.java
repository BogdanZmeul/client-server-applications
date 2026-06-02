package utils;

import java.nio.ByteBuffer;

public class PacketKey {
    public static String createKey(byte[] data) {
        byte source = data[1];
        long packetId = ByteBuffer.wrap(data, 2, 8).getLong();

        return source + ":" + packetId;
    }
}
