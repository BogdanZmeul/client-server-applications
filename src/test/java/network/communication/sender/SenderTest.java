package network.communication.sender;

import network.protocol.NetworkSender;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

class SenderTest {

    @Test
    void shouldSenderSendMessages() throws Exception {
        byte[] endPacket = new byte[0];
        byte[] firstMessage = {1, 2, 3};
        byte[] secondMessage = {4, 5, 6};
        InetAddress target = InetAddress.getLoopbackAddress();
        BlockingQueue<byte[]> input = new LinkedBlockingQueue<>();
        TestNetworkSender networkSender = new TestNetworkSender();

        input.add(firstMessage);
        input.add(secondMessage);
        input.add(endPacket);

        Sender sender = new Sender(networkSender, input, endPacket, target);
        sender.run();

        assertEquals(2, networkSender.getSentData().size());
        assertArrayEquals(firstMessage, networkSender.getSentData().poll());
        assertArrayEquals(secondMessage, networkSender.getSentData().poll());
        assertEquals(target, networkSender.getTarget());
    }

    @Test
    void shouldSenderStopWhenFirstPacketIsEndPacket() {
        byte[] endPacket = new byte[0];
        InetAddress target = InetAddress.getLoopbackAddress();
        BlockingQueue<byte[]> input = new LinkedBlockingQueue<>();
        TestNetworkSender networkSender = new TestNetworkSender();

        input.add(endPacket);

        Sender sender = new Sender(networkSender, input, endPacket, target);
        sender.run();

        assertTrue(networkSender.getSentData().isEmpty());
        assertNull(networkSender.getTarget());
    }

    private static class TestNetworkSender implements NetworkSender {
        private final BlockingQueue<byte[]> sentData = new LinkedBlockingQueue<>();
        private InetAddress target;

        @Override
        public void sendMessage(byte[] data, InetAddress target) {
            sentData.add(data);
            this.target = target;
        }

        public BlockingQueue<byte[]> getSentData() {
            return sentData;
        }

        public InetAddress getTarget() {
            return target;
        }
    }
}
