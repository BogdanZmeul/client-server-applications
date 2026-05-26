import network.communication.receiver.Receiver;
import network.protocol.NetworkReceiver;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

class ReceiverTest {

    @Test
    void shouldReceiverPutMessagesToOutput() {
        byte[] firstMessage = {1, 2, 3};
        byte[] secondMessage = {4, 5, 6};
        BlockingQueue<byte[]> output = new LinkedBlockingQueue<>();
        Receiver receiver = new Receiver(new TestNetworkReceiver(firstMessage, secondMessage), output);

        receiver.receiveMessage();

        assertEquals(2, output.size());
        assertArrayEquals(firstMessage, output.poll());
        assertArrayEquals(secondMessage, output.poll());
    }

    @Test
    void shouldReceiverStopWhenFirstMessageIsNull() {
        BlockingQueue<byte[]> output = new LinkedBlockingQueue<>();
        Receiver receiver = new Receiver(new TestNetworkReceiver(), output);

        receiver.receiveMessage();

        assertTrue(output.isEmpty());
    }

    @Test
    void shouldReceiverPutManyMessagesToOutput() {
        int messagesCount = 100;
        byte[][] messages = new byte[messagesCount][];
        BlockingQueue<byte[]> output = new LinkedBlockingQueue<>();

        for (int i = 0; i < messagesCount; i++) {
            messages[i] = new byte[]{(byte) i};
        }

        Receiver receiver = new Receiver(new TestNetworkReceiver(messages), output);
        receiver.receiveMessage();

        assertEquals(messagesCount, output.size());
        for (int i = 0; i < messagesCount; i++) {
            assertArrayEquals(messages[i], output.poll());
        }
    }

    private static class TestNetworkReceiver implements NetworkReceiver {
        private final BlockingQueue<byte[]> messages = new LinkedBlockingQueue<>();

        public TestNetworkReceiver() {}

        public TestNetworkReceiver(byte[][] messages) {
            for (byte[] message : messages) {
                this.messages.add(message);
            }
        }

        public TestNetworkReceiver(byte[] firstMessage, byte[] secondMessage) {
            messages.add(firstMessage);
            messages.add(secondMessage);
        }

        @Override
        public byte[] receiveMessage() {
            return messages.poll();
        }
    }
}
