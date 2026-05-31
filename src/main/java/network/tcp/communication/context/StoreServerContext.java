package network.tcp.communication.context;

import network.storage.ProductStorage;

import java.net.Socket;
import java.security.Key;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class StoreServerContext {
    private final Key secretKey;
    private final ProductStorage productStorage;
    private final int receiversCount;
    private final int decriptorsCount;
    private final int processorsCount;
    private final int encriptorsCount;
    private final int sendersCount;
    private final BlockingQueue<Socket> clientSockets = new LinkedBlockingQueue<>();
    private final BlockingQueue<Thread> clientThreads = new LinkedBlockingQueue<>();

    public StoreServerContext(Key secretKey, ProductStorage productStorage,
                              int receiversCount, int decriptorsCount, int processorsCount,
                              int encriptorsCount, int sendersCount) {
        this.secretKey = secretKey;
        this.productStorage = productStorage;
        this.receiversCount = receiversCount;
        this.decriptorsCount = decriptorsCount;
        this.processorsCount = processorsCount;
        this.encriptorsCount = encriptorsCount;
        this.sendersCount = sendersCount;
    }

    public Key getSecretKey() {
        return secretKey;
    }

    public ProductStorage getProductStorage() {
        return productStorage;
    }

    public int getReceiversCount() {
        return receiversCount;
    }

    public int getDecriptorsCount() {
        return decriptorsCount;
    }

    public int getProcessorsCount() {
        return processorsCount;
    }

    public int getEncriptorsCount() {
        return encriptorsCount;
    }

    public int getSendersCount() {
        return sendersCount;
    }

    public BlockingQueue<Socket> getClientSockets() {
        return clientSockets;
    }

    public BlockingQueue<Thread> getClientThreads() {
        return clientThreads;
    }
}
