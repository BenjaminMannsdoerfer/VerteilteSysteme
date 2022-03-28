package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import blatt2.broker.PoisonPill;
import messaging.Endpoint;
import messaging.Message;

import javax.swing.*;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {
    private boolean running = true;
    private final Endpoint endpoint;
    private final ClientCollection<InetSocketAddress> clientCollection;
    private static final int NUM_THREADS = 5;
    private final ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

    private Broker() {
        this.endpoint = new Endpoint(Properties.PORT);
        this.clientCollection = new ClientCollection<>();
    }

    public static void main(String[] args) {
        Broker broker = new Broker();
        broker.broker();
    }

    private class BrokerTask implements Runnable {
        ClientCollection<InetSocketAddress> clientCollection;
        Serializable payload;
        InetSocketAddress sender;

        private BrokerTask(Serializable payload, InetSocketAddress sender, ClientCollection<InetSocketAddress> clientCollection) {
            this.clientCollection = clientCollection;
            this.payload = payload;
            this.sender = sender;
        }

        @Override
        public void run() {
            if (payload instanceof RegisterRequest) {
                register(sender);
            } else if (payload instanceof DeregisterRequest) {
                deregister(sender);
            } else if (payload instanceof HandoffRequest) {
                handoffFish(((HandoffRequest) payload), sender);
            }
        }
    }

    // dispatcher
    public void broker() {
        /*executor.execute(() -> {
            JOptionPane.showMessageDialog(null, "Press OK button to stop server");
            running = false;
        });*/
        while (running) {
            Message message = endpoint.blockingReceive();
            if (message.getPayload() instanceof PoisonPill) {
                running = false;
            }
            executor.execute(new BrokerTask(message.getPayload(), message.getSender(), clientCollection));
        }
        executor.shutdown();
    }

    private synchronized void register(InetSocketAddress address) {
        String id = "tank" + clientCollection.size();
        System.out.println("register " + id);
        clientCollection.add(id, address);
        // Add fish per register
        endpoint.send(address, new RegisterResponse(id));
    }

    private synchronized void deregister(InetSocketAddress address) {
        System.out.println("deregister " + clientCollection.indexOf(address));
        clientCollection.remove(clientCollection.indexOf(address));
    }

    private synchronized void handoffFish(HandoffRequest handoffRequest, InetSocketAddress address) {
        ReadWriteLock lock = new ReentrantReadWriteLock();
        lock.writeLock().lock();
        InetSocketAddress neighbor;
        if (handoffRequest.getFish().getDirection() == Direction.LEFT && handoffRequest.getFish().getX() == 0) {
            neighbor = clientCollection.getLeftNeighorOf(clientCollection.indexOf(address));
        } else {
            neighbor = clientCollection.getRightNeighorOf(clientCollection.indexOf(address));
        }
        System.out.println("Fish entered");
        System.out.println(clientCollection.getClient(clientCollection.indexOf(address)));
        endpoint.send(neighbor, handoffRequest);
        lock.writeLock().unlock();
    }
}
