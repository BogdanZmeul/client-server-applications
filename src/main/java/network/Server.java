package network;

import cipher.Decoder;
import cipher.Encoder;
import data.Package;
import network.communication.receiver.Receiver;
import network.security.Decriptor;
import network.security.Encriptor;
import network.communication.sender.Sender;
import network.processor.Processor;
import network.processor.ProcessorWorker;
import network.protocol.NetworkReceiver;
import network.protocol.NetworkSender;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Server {
    private static final byte[] END_PACKET = new byte[0];
    private static final Package END_PACKAGE = new Package();

    private final NetworkReceiver networkReceiver;
    private final Decoder decoder;
    private final Processor processor;
    private final Encoder encoder;
    private final NetworkSender networkSender;
    private final InetAddress target;

    private final int receiversCount;
    private final int decriptorsCount;
    private final int processorsCount;
    private final int encriptorsCount;
    private final int sendersCount;

    private final ExecutorService receiverExecutor;
    private final ExecutorService decriptorExecutor;
    private final ExecutorService processorExecutor;
    private final ExecutorService encriptorExecutor;
    private final ExecutorService senderExecutor;

    private final BlockingQueue<byte[]> receivedQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Package> decodedQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Package> processedQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<byte[]> encodedQueue = new LinkedBlockingQueue<>();

    public Server(NetworkReceiver networkReceiver, Decoder decoder, Processor processor,
                  Encoder encoder, NetworkSender networkSender,
                  int receiversCount, int decriptorsCount, int processorsCount,
                  int encriptorsCount, int sendersCount) {
        this(networkReceiver, decoder, processor, encoder, networkSender,
                InetAddress.getLoopbackAddress(), receiversCount, decriptorsCount,
                processorsCount, encriptorsCount, sendersCount);
    }

    public Server(NetworkReceiver networkReceiver, Decoder decoder, Processor processor,
                  Encoder encoder, NetworkSender networkSender, InetAddress target,
                  int receiversCount, int decriptorsCount, int processorsCount,
                  int encriptorsCount, int sendersCount) {
        checkCount(receiversCount, "Receivers count");
        checkCount(decriptorsCount, "Decriptors count");
        checkCount(processorsCount, "Processors count");
        checkCount(encriptorsCount, "Encoders count");
        checkCount(sendersCount, "Senders count");

        this.networkReceiver = networkReceiver;
        this.decoder = decoder;
        this.processor = processor;
        this.encoder = encoder;
        this.networkSender = networkSender;
        this.target = target;
        this.receiversCount = receiversCount;
        this.decriptorsCount = decriptorsCount;
        this.processorsCount = processorsCount;
        this.encriptorsCount = encriptorsCount;
        this.sendersCount = sendersCount;
        this.receiverExecutor = Executors.newFixedThreadPool(receiversCount);
        this.decriptorExecutor = Executors.newFixedThreadPool(decriptorsCount);
        this.processorExecutor = Executors.newFixedThreadPool(processorsCount);
        this.encriptorExecutor = Executors.newFixedThreadPool(encriptorsCount);
        this.senderExecutor = Executors.newFixedThreadPool(sendersCount);
    }

    public void start() {
        for (int i = 0; i < receiversCount; i++) {
            receiverExecutor.submit(new Receiver(networkReceiver, receivedQueue));
        }

        for (int i = 0; i < decriptorsCount; i++) {
            decriptorExecutor.submit(new Decriptor(decoder, receivedQueue, decodedQueue, END_PACKET));
        }

        for (int i = 0; i < processorsCount; i++) {
            processorExecutor.submit(new ProcessorWorker(processor, decodedQueue, processedQueue, END_PACKAGE));
        }

        for (int i = 0; i < encriptorsCount; i++) {
            encriptorExecutor.submit(new Encriptor(encoder, processedQueue, encodedQueue, END_PACKAGE));
        }

        for (int i = 0; i < sendersCount; i++) {
            senderExecutor.submit(new Sender(networkSender, encodedQueue, END_PACKET, target));
        }
    }

    public void awaitStop() throws InterruptedException {
        receiverExecutor.shutdown();
        waitExecutor(receiverExecutor);
        addEndPackets(receivedQueue, decriptorsCount);

        decriptorExecutor.shutdown();
        waitExecutor(decriptorExecutor);
        addEndPackages(decodedQueue, processorsCount);

        processorExecutor.shutdown();
        waitExecutor(processorExecutor);
        addEndPackages(processedQueue, encriptorsCount);

        encriptorExecutor.shutdown();
        waitExecutor(encriptorExecutor);
        addEndPackets(encodedQueue, sendersCount);

        senderExecutor.shutdown();
        waitExecutor(senderExecutor);
    }

    private void checkCount(int count, String name) {
        if (count < 1) {
            throw new RuntimeException(name + " should be positive");
        }
    }

    private void waitExecutor(ExecutorService executor) throws InterruptedException {
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    private void addEndPackets(BlockingQueue<byte[]> queue, int count) throws InterruptedException {
        for (int i = 0; i < count; i++) {
            queue.put(END_PACKET);
        }
    }

    private void addEndPackages(BlockingQueue<Package> queue, int count) throws InterruptedException {
        for (int i = 0; i < count; i++) {
            queue.put(END_PACKAGE);
        }
    }
}
