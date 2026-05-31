package network.tcp.communication;

import network.protocol.NetworkReceiver;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class TcpNetworkReceiver implements NetworkReceiver {
    private static final int HEADER_LENGTH = 16;
    private static final int MESSAGE_CRC_LENGTH = 2;

    private final DataInputStream input;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public TcpNetworkReceiver(InputStream input) {
        this.input = new DataInputStream(input);
    }

    @Override
    public synchronized byte[] receiveMessage() throws Exception {
        if (closed.get()) {
            return null;
        }

        try {
            byte[] header = new byte[HEADER_LENGTH];
            input.readFully(header);

            int messageLength = ByteBuffer.wrap(header, 10, 4).getInt();
            byte[] packet = new byte[HEADER_LENGTH + messageLength + MESSAGE_CRC_LENGTH];
            System.arraycopy(header, 0, packet, 0, HEADER_LENGTH);

            input.readFully(packet, HEADER_LENGTH, messageLength + MESSAGE_CRC_LENGTH);

            return packet;
        } catch (EOFException e) {
            closed.set(true);
            return null;
        }
    }
}
