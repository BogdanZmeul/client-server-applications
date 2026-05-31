package network.tcp.communication.context;

import cipher.Decoder;
import cipher.Encoder;
import data.Package;

import java.net.InetAddress;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class StoreClientContext {
    private final InetAddress address;
    private final int port;
    private final Encoder encoder;
    private final Decoder decoder;
    private final AtomicBoolean isWorking = new AtomicBoolean(false);
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final BlockingDeque<Package> packagesToSend = new LinkedBlockingDeque<>();
    private final BlockingQueue<Package> responses = new LinkedBlockingQueue<>();

    public StoreClientContext(InetAddress address, int port, Encoder encoder, Decoder decoder) {
        this.address = address;
        this.port = port;
        this.encoder = encoder;
        this.decoder = decoder;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public Encoder getEncoder() {
        return encoder;
    }

    public Decoder getDecoder() {
        return decoder;
    }

    public AtomicBoolean getIsWorking() {
        return isWorking;
    }

    public AtomicBoolean getIsConnected() {
        return isConnected;
    }

    public BlockingDeque<Package> getPackagesToSend() {
        return packagesToSend;
    }

    public BlockingQueue<Package> getResponses() {
        return responses;
    }
}
