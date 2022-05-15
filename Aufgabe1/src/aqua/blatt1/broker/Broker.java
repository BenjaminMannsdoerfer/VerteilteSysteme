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
import java.sql.Timestamp;
import java.util.Date;
import java.util.TimerTask;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {
    private volatile boolean running = true;
    private final Endpoint endpoint;
    private final ClientCollection<InetSocketAddress> clientCollection;
    private static final int NUM_THREADS = 5;
    private final ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
    private final long LEASE_TIME = 5000L;

    private Broker() {
        this.endpoint = new Endpoint(Properties.PORT);
        this.clientCollection = new ClientCollection<>();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (int i = 0; i < clientCollection.size(); i++) {
                    //long actualTime = new Timestamp(new Date().getTime()).getTime();
                    long actualTime = new Date().getTime();
                    System.out.println(clientCollection.getTimeStamp(i) - actualTime);
                    System.out.println(LEASE_TIME);

                    //if (actualTime - LEASE_TIME > clientCollection.getTimeStamp(i)) {
                    System.out.println("TEST: " + (actualTime - clientCollection.getTimeStamp(i)));
                    if ((actualTime - clientCollection.getTimeStamp(i)) > 5000) {
                        System.out.println("TEST 1");
                        deregister(clientCollection.getClient(i));
                        endpoint.send(clientCollection.getClient(i), new LeasingRunOut());
                        System.out.println(clientCollection.size());
                    }
                    System.out.println("TEST 2");
                }
            }
        }, 0, 3*1000);
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
            } else if (payload instanceof NameResolutionRequest) {
                sendNameResolutionResponse(((NameResolutionRequest) payload), sender);
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
        Timestamp currentTime = new Timestamp(new Date().getTime());
        System.out.println(clientCollection.indexOf(address));
        if (clientCollection.indexOf(address) != -1) {
            System.out.println("Broker: Client " + clientCollection.getId(clientCollection.indexOf(address)) + " reregistered");
            clientCollection.setTimeStamp(clientCollection.indexOf(address), currentTime.getTime());
            endpoint.send(address, new RegisterResponse(clientCollection.getId(clientCollection.indexOf(address)), LEASE_TIME));
        } else {
            ReadWriteLock lock = new ReentrantReadWriteLock();
            lock.writeLock().lock();
            String id = "tank" + clientCollection.size();
            lock.writeLock().unlock();
            lock.writeLock().lock();
            clientCollection.add(id, address, currentTime);
            lock.writeLock().unlock();
            lock.readLock().lock();
            InetSocketAddress neighborLeft = clientCollection.getLeftNeighorOf(clientCollection.indexOf(address));
            InetSocketAddress neighborRight = clientCollection.getRightNeighorOf(clientCollection.indexOf(address));
            //System.out.println(id);
            //System.out.println(neighborLeft);
            //System.out.println(neighborRight);
            if (clientCollection.size() == 1) {
                endpoint.send(address, new NeighborUpdate(address, address));
                endpoint.send(address, new Token());
            } else {
                endpoint.send(address, new NeighborUpdate(neighborLeft, neighborRight));
                endpoint.send(neighborLeft, new NeighborUpdate(clientCollection.getLeftNeighorOf(clientCollection.indexOf(neighborLeft)), address));
                endpoint.send(neighborRight, new NeighborUpdate(address, clientCollection.getRightNeighorOf(clientCollection.indexOf(neighborRight))));
            }
            System.out.println("register " + id);
            // Add fish per register
            endpoint.send(address, new RegisterResponse(id, LEASE_TIME));
            lock.readLock().unlock();
        }
    }


    private synchronized void deregister(InetSocketAddress address) {
        ReadWriteLock lock = new ReentrantReadWriteLock();
        lock.readLock().lock();
        System.out.println("deregister tank" + clientCollection.indexOf(address));
        InetSocketAddress neighborLeft;
        InetSocketAddress neighborRight;
        // mind. 2 Clients
        if (clientCollection.size() != 1) {
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

    private synchronized void sendNameResolutionResponse(NameResolutionRequest nameResolutionRequest, InetSocketAddress sender) {
        ReadWriteLock lock = new ReentrantReadWriteLock();
        lock.readLock().lock();
        int tankIndex = clientCollection.indexOf(nameResolutionRequest.getTankId());
        InetSocketAddress tankAddress = clientCollection.getClient(tankIndex);
        endpoint.send(sender, new NameResolutionResponse(tankAddress, nameResolutionRequest.getRequestId()));
        lock.readLock().unlock();
    }
}
