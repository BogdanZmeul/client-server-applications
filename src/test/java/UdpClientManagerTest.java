import network.udp.communication.UdpClientManager;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class UdpClientManagerTest {
    @Test
    void shouldSaveAndRemoveClientAddress() {
        UdpClientManager manager = new UdpClientManager();
        InetSocketAddress client = new InetSocketAddress("localhost", 9001);

        assertTrue(manager.addClient("1:10", client));
        assertFalse(manager.addClient("1:10", new InetSocketAddress("localhost", 9002)));
        assertEquals(client, manager.removeClient("1:10"));
        assertNull(manager.removeClient("1:10"));
    }

    @Test
    void shouldSaveResponseByPacketKey() {
        UdpClientManager manager = new UdpClientManager();
        byte[] response = new byte[] {1, 2, 3};

        manager.addResponse("1:10", response);

        assertArrayEquals(response, manager.getResponse("1:10"));
        assertNull(manager.getResponse("1:11"));
    }
}
