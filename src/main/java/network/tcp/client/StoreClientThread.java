package network.tcp.client;

import data.Package;
import network.tcp.communication.context.StoreClientContext;
import network.tcp.communication.TcpNetworkReceiver;
import network.tcp.communication.TcpNetworkSender;

import java.io.IOException;
import java.net.Socket;

public class StoreClientThread extends Thread {
    private static final int RECONNECT_DELAY = 1000;

    private final StoreClientContext context;

    private Socket socket;
    private TcpNetworkReceiver receiver;
    private TcpNetworkSender sender;

    public StoreClientThread(StoreClientContext context) {
        this.context = context;
    }

    @Override
    public void run() {
        System.out.println("StoreClientTCP started");
        while (context.getIsWorking().get()) {
            try {
                if (!context.getIsConnected().get() && !tryConnect()) {
                    sleepBeforeReconnect();
                    continue;
                }

                Package pack = context.getPackagesToSend().take();
                sendAndReadResponse(pack);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        closeConnection();
        System.out.println("StoreClientTCP finished");
    }

    public void closeConnection() {
        context.getIsConnected().set(false);
        if (sender != null) {
            sender.close();
        }

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Client socket close error: " + e.getMessage());
        }
    }

    private boolean tryConnect() {
        try {
            socket = new Socket(context.getAddress(), context.getPort());
            receiver = new TcpNetworkReceiver(socket.getInputStream());
            sender = new TcpNetworkSender(socket.getOutputStream());
            context.getIsConnected().set(true);
            System.out.println("Connected to server: " + socket);
            return true;
        } catch (IOException e) {
            context.getIsConnected().set(false);
            System.out.println("Server is not available. Reconnecting...");
            return false;
        }
    }

    private void sendAndReadResponse(Package pack) {
        try {
            byte[] request = context.getEncoder().encode(pack);
            sender.sendMessage(request, context.getAddress());

            byte[] response = receiver.receiveMessage();
            if (response == null) {
                throw new IOException("Connection closed");
            }

            context.getResponses().put(context.getDecoder().decode(response));
        } catch (Exception e) {
            context.getIsConnected().set(false);
            context.getPackagesToSend().addFirst(pack);
            closeConnection();
            System.out.println("Client TCP error: " + e.getMessage());
            sleepBeforeReconnect();
        }
    }

    private void sleepBeforeReconnect() {
        try {
            Thread.sleep(RECONNECT_DELAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
