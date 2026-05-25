package network.protocol;

import java.net.InetAddress;

public interface NetworkSender {
    void sendMessage(byte[] data, InetAddress target) throws Exception;
}
