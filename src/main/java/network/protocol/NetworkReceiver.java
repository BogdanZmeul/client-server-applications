package network.protocol;

public interface NetworkReceiver {
    byte[] receiveMessage() throws Exception;
}
