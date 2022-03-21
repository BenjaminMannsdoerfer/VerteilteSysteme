package aqua.blatt1.broker;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;
import java.net.InetSocketAddress;

public class Broker {
    private Endpoint endpoint;
    private ClientCollection<InetSocketAddress> clientCollection;

    private Broker() {
        this.endpoint = new Endpoint(Properties.PORT);
        this.clientCollection = new ClientCollection<>();
    }

    public static void main(String[] args) {
        Broker broker = new Broker();
        broker.broker();
    }

    public void broker() {
        boolean running = true;
        while (running) {
            Message message = endpoint.blockingReceive();
            System.out.println(message.getPayload());
            if (message.getPayload() instanceof RegisterRequest) {
                register(message.getSender());
            } else if (message.getPayload() instanceof DeregisterRequest) {
                deregister(message.getSender());
            } else if (message.getPayload() instanceof HandoffRequest) {
                handoffFish(((HandoffRequest) message.getPayload()), message.getSender());
            } else {
                running = false;
            }
        }
    }

    private void register(InetSocketAddress address) {
        String id = "tank" + clientCollection.size();
        System.out.println("register " + id);
        clientCollection.add(id, address);
        // Add fish per register
        endpoint.send(address, new RegisterResponse(id));
    }

    private void deregister(InetSocketAddress address) {
        System.out.println("deregister " + clientCollection.indexOf(address));
        clientCollection.remove(clientCollection.indexOf(address));
    }

    private void handoffFish(HandoffRequest handoffRequest, InetSocketAddress address) {
        InetSocketAddress neighbor;
        // handoffRequest.getFish().hitsEdge()
        if (handoffRequest.getFish().getDirection() == Direction.LEFT && handoffRequest.getFish().getX() == 0) {
            neighbor = clientCollection.getLeftNeighorOf(clientCollection.indexOf(address));
        } else {
            neighbor = clientCollection.getRightNeighorOf(clientCollection.indexOf(address));
        }
        System.out.println("Fish entered");
        System.out.println(clientCollection.getClient(clientCollection.indexOf(address)));
        endpoint.send(neighbor, handoffRequest);
    }
}
