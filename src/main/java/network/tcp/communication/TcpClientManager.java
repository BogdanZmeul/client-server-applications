package network.tcp.communication;

import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TcpClientManager {
    private final Map<String, Socket> sockets = new ConcurrentHashMap<>();
    private final Map<String, byte[]> responses = new ConcurrentHashMap<>();

    public boolean addClient(String key, Socket socket) {
        return sockets.putIfAbsent(key, socket) == null;
    }

    public Socket removeClient(String key) {
        return sockets.remove(key);
    }

    public void addResponse(String key, byte[] data) {
        responses.put(key, data);
    }

    public byte[] getResponse(String key) {
        return responses.get(key);
    }
}
