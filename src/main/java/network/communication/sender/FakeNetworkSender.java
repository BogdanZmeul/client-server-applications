package network.communication.sender;

import network.protocol.NetworkSender;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FakeNetworkSender implements NetworkSender {
    private final List<byte[]> sentData = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void sendMessage(byte[] data, InetAddress target) {
        sentData.add(data);
        System.out.println(Arrays.toString(data));
    }

    public List<byte[]> getSentData() {
        return sentData;
    }
}
