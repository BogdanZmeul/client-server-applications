package network.tcp.server;

import cipher.Decoder;
import cipher.Encoder;
import network.Server;
import network.processor.Processor;
import network.tcp.communication.context.StoreServerContext;
import network.tcp.communication.TcpNetworkReceiver;
import network.tcp.communication.TcpNetworkSender;

import java.io.IOException;
import java.net.Socket;

public class StoreClientConnection extends Thread {
    private final Socket socket;
    private final StoreServerContext context;

    public StoreClientConnection(Socket socket, StoreServerContext context) {
        this.socket = socket;
        this.context = context;
    }

    @Override
    public void run() {
        System.out.println("Open client connection: " + socket);
        try (socket) {
            Server server = createPipelineServer();
            server.awaitStop();
        } catch (Exception e) {
            System.out.println("Client connection error: " + e.getMessage());
        } finally {
            context.getClientSockets().remove(socket);
            context.getClientThreads().remove(this);
            System.out.println("Close client connection: " + socket);
        }
    }

    private Server createPipelineServer() throws IOException {
        TcpNetworkReceiver receiver = new TcpNetworkReceiver(socket.getInputStream());
        TcpNetworkSender sender = new TcpNetworkSender(socket.getOutputStream());
        Processor processor = new Processor(context.getProductStorage());
        Server server = new Server(receiver, new Decoder(context.getSecretKey()), processor,
                new Encoder(context.getSecretKey()), sender, socket.getInetAddress(),
                context.getReceiversCount(), context.getDecriptorsCount(),
                context.getProcessorsCount(), context.getEncriptorsCount(),
                context.getSendersCount());

        server.start();
        return server;
    }
}
