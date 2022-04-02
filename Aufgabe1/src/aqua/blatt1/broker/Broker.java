package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.*;
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
    private volatile boolean running = true;
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
        Serializable payload;
        InetSocketAddress sender;

        private BrokerTask(Serializable payload, InetSocketAddress sender) {
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

    private class StopTask implements Runnable {

        @Override
        public void run() {
            JOptionPane.showMessageDialog(null, "Press OK button to stop server");
            running = false;
        }
    }

    // dispatcher
    public void broker() {
        //Thread thread = new Thread(new StopTask());
        //thread.start();
        while (running) {
            Message message = endpoint.blockingReceive();
            if (message.getPayload() instanceof PoisonPill) {
                running = false;
            }
            executor.execute(new BrokerTask(message.getPayload(), message.getSender()));
        }
        executor.shutdown();
    }

    private synchronized void register(InetSocketAddress address) {
        ReadWriteLock lock = new ReentrantReadWriteLock();
        lock.writeLock().lock();
        String id = "tank" + clientCollection.size();
        lock.writeLock().unlock();
        lock.writeLock().lock();
        clientCollection.add(id, address);
        lock.writeLock().unlock();
        lock.readLock().lock();
        InetSocketAddress neighborLeft = clientCollection.getLeftNeighorOf(clientCollection.indexOf(address));
        InetSocketAddress neighborRight = clientCollection.getRightNeighorOf(clientCollection.indexOf(address));
        if(clientCollection.size() == 1) {
            endpoint.send(address, new NeighborUpdate(address, address));
        } else {
            endpoint.send(address, new NeighborUpdate(neighborLeft, neighborRight));
            endpoint.send(neighborLeft, new NeighborUpdate(clientCollection.getLeftNeighorOf(clientCollection.indexOf(neighborLeft)), address));
            endpoint.send(neighborRight, new NeighborUpdate(address, clientCollection.getRightNeighorOf(clientCollection.indexOf(neighborRight))));
        }
        System.out.println("register " + id);
        // Add fish per register
        endpoint.send(address, new RegisterResponse(id));
        lock.readLock().unlock();


    }

    private synchronized void deregister(InetSocketAddress address) {
        ReadWriteLock lock = new ReentrantReadWriteLock();
        lock.readLock().lock();
        System.out.println("deregister tank" + clientCollection.indexOf(address));
        InetSocketAddress neighborLeft;
        InetSocketAddress neighborRight;
        // mind. 2 Clients
        if(clientCollection.size() != 1) {
            neighborLeft = clientCollection.getLeftNeighorOf(clientCollection.indexOf(address));
            neighborRight = clientCollection.getRightNeighorOf(clientCollection.indexOf(address));
            endpoint.send(neighborLeft, new NeighborUpdate(clientCollection.getRightNeighorOf(clientCollection.indexOf(neighborLeft)), neighborRight));
            endpoint.send(neighborLeft, new NeighborUpdate(neighborLeft, clientCollection.getRightNeighorOf(clientCollection.indexOf(neighborRight))));
        } else {
            endpoint.send(address, new NeighborUpdate(address, address));
        }
        lock.readLock().unlock();
        lock.writeLock().lock();
        clientCollection.remove(clientCollection.indexOf(address));
        lock.writeLock().unlock();
    }

    private synchronized void handoffFish(HandoffRequest handoffRequest, InetSocketAddress address) {
        ReadWriteLock lock = new ReentrantReadWriteLock();
        lock.readLock().lock();
        InetSocketAddress neighbor;
        if (handoffRequest.getFish().getDirection() == Direction.LEFT && handoffRequest.getFish().getX() == 0) {
            neighbor = clientCollection.getLeftNeighorOf(clientCollection.indexOf(address));
        } else {
            neighbor = clientCollection.getRightNeighorOf(clientCollection.indexOf(address));
        }
        System.out.println(clientCollection.getClient(clientCollection.indexOf(address)));
        endpoint.send(neighbor, handoffRequest);
        lock.readLock().unlock();
        System.out.println("Fish entered");
    }
}
