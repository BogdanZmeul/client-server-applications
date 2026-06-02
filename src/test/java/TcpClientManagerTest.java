import network.tcp.communication.TcpClientManager;
import org.junit.jupiter.api.Test;

import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

class TcpClientManagerTest {
    @Test
    void shouldSaveAndRemoveClientSocket() {
        TcpClientManager manager = new TcpClientManager();
        Socket socket = new Socket();

        assertTrue(manager.addClient("1:10", socket));
        assertFalse(manager.addClient("1:10", new Socket()));
        assertSame(socket, manager.removeClient("1:10"));
        assertNull(manager.removeClient("1:10"));
    }

    @Test
    void shouldSaveResponseByPacketKey() {
        TcpClientManager manager = new TcpClientManager();
        byte[] response = new byte[] {1, 2, 3};

        manager.addResponse("1:10", response);

        assertArrayEquals(response, manager.getResponse("1:10"));
        assertNull(manager.getResponse("1:11"));
    }
}
