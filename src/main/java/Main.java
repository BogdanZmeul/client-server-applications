import cipher.Decoder;
import cipher.Encoder;
import network.Server;
import network.communication.receiver.FakeNetworkReceiver;
import network.communication.sender.FakeNetworkSender;
import network.processor.Processor;
import network.storage.ProductStorage;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

public class Main {
    public static void main(String[] args) throws Exception {
        byte[] keyBytes = "very_strong_key1".getBytes();
        Key secretKey = new SecretKeySpec(keyBytes, "AES");

        Encoder encoder = new Encoder(secretKey);
        Decoder decoder = new Decoder(secretKey);
        FakeNetworkReceiver receiver = new FakeNetworkReceiver(encoder, 5);
        FakeNetworkSender sender = new FakeNetworkSender();
        ProductStorage productStorage = new ProductStorage();
        Processor processor = new Processor(productStorage);

        Server server = new Server(receiver, decoder, processor, encoder, sender,
                2, 2, 2, 2, 2);

        System.out.println("Server started");
        server.start();
        server.awaitStop();
        System.out.println("Server finished");
    }
}
