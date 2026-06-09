package network.communication.receiver;

import cipher.Encoder;
import data.Message;
import data.Package;
import network.protocol.NetworkReceiver;
import utils.MessageType;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class FakeNetworkReceiver implements NetworkReceiver {
    private final Encoder encoder;
    private final int messagesLimit;
    private final Random random = new Random();
    private final AtomicLong packetId = new AtomicLong(1);
    private int generatedMessages;

    public FakeNetworkReceiver(Encoder encoder, int messagesLimit) {
        this.encoder = encoder;
        this.messagesLimit = messagesLimit;
    }

    @Override
    public synchronized byte[] receiveMessage() throws Exception {
        if (generatedMessages >= messagesLimit) {
            return null;
        }

        generatedMessages++;

        int type = random.nextInt(6) + 1;
        String text = createMessage(type);
        Package pack = new Package((byte) 1, packetId.getAndIncrement(), new Message(type, 1, text));

        return encoder.encode(pack);
    }

    private String createMessage(int type) {
        return switch (type) {
            case MessageType.GET_PRODUCT_COUNT -> "1";
            case MessageType.TAKE_PRODUCT -> "1;1";
            case MessageType.ADD_PRODUCT -> "1;2";
            case MessageType.ADD_GROUP -> "fruit";
            case MessageType.ADD_PRODUCT_TO_GROUP -> "1;1";
            case MessageType.SET_PRICE -> "1;71.5";
            default -> "";
        };
    }
}
