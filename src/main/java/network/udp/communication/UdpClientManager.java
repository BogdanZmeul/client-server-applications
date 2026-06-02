package network.udp.communication;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UdpClientManager {
    private final Map<String, InetSocketAddress> clients = new ConcurrentHashMap<>();
    private final Map<String, byte[]> responses = new ConcurrentHashMap<>();

    public boolean addClient(String key, InetSocketAddress client) {
        return clients.putIfAbsent(key, client) == null;
    }

    public InetSocketAddress removeClient(String key) {
        return clients.remove(key);
    }

    public void addResponse(String key, byte[] data) {
        responses.put(key, data);
    }

    public byte[] getResponse(String key) {
        return responses.get(key);
    }
}
