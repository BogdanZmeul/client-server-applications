import network.communication.sender.FakeNetworkSender;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

class FakeNetworkSenderTest {

    @Test
    void shouldFakeNetworkSenderSaveSentMessages() {
        FakeNetworkSender sender = new FakeNetworkSender();
        byte[] data = {7, 8, 9};

        sender.sendMessage(data, InetAddress.getLoopbackAddress());

        assertEquals(1, sender.getSentData().size());
        assertArrayEquals(data, sender.getSentData().getFirst());
    }

    @Test
    void shouldFakeNetworkSenderSaveSeveralMessages() {
        FakeNetworkSender sender = new FakeNetworkSender();
        byte[] firstMessage = {1, 2, 3};
        byte[] secondMessage = {4, 5, 6};

        sender.sendMessage(firstMessage, InetAddress.getLoopbackAddress());
        sender.sendMessage(secondMessage, InetAddress.getLoopbackAddress());

        assertEquals(2, sender.getSentData().size());
        assertArrayEquals(firstMessage, sender.getSentData().get(0));
        assertArrayEquals(secondMessage, sender.getSentData().get(1));
    }
}
