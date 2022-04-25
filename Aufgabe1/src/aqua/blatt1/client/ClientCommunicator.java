package aqua.blatt1.client;

import java.net.InetSocketAddress;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;

public class ClientCommunicator {
	private final Endpoint endpoint;

	public ClientCommunicator() {
		endpoint = new Endpoint();
	}

	public class ClientForwarder {
		private final InetSocketAddress broker;

		private ClientForwarder() {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
		}

		public void register() {
			endpoint.send(broker, new RegisterRequest());
		}

		public void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public void handOff(FishModel fish, TankModel tankModel) {
			if (fish.getDirection() == Direction.LEFT && fish.getX() == 0) {
				System.out.println("entered from left");
				endpoint.send(tankModel.getNeighborAddressLeft(), new HandoffRequest(fish));
			} else {
				System.out.println("entered from right");
				endpoint.send(tankModel.getNeighborAddressRight(), new HandoffRequest(fish));
			}
		}

		public void sendToken(InetSocketAddress address) {
			endpoint.send(address, new Token());
		}

		public void sendSnapshotToken(InetSocketAddress address, SnapshotToken token) {
			endpoint.send(address, token);
		}

		// Die Klassen
		// ClientForwarder und ClientReceiver müssen so angepasst werden, dass sie
		// den SnapshotMarker senden bzw. empfangen können.

		public void sendSnapshotMarker(InetSocketAddress address) {
			endpoint.send(address, new SnapshotMarker());
		}
	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();

				if (msg.getPayload() instanceof RegisterResponse)
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId());

				if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());

				if (msg.getPayload() instanceof NeighborUpdate) {
					tankModel.setNeighborAddressLeft(((NeighborUpdate) msg.getPayload()).getLeftNeighborAddress());
					tankModel.setNeighborAddressRight(((NeighborUpdate) msg.getPayload()).getRightNeighborAddress());
				}
				if (msg.getPayload() instanceof Token)
					tankModel.receiveToken();

				// Die Klassen
				// ClientForwarder und ClientReceiver müssen so angepasst werden, dass sie
				// den SnapshotMarker senden bzw. empfangen können.

				if (msg.getPayload() instanceof SnapshotMarker) {
					// TODO Empfängt ein Klient einen SnapshotMarker, dann agiert er entsprechend dem
					// TODO Algorithmus von Lamport zum Ermitteln seinen lokalen Schnappschusses.
					tankModel.receiveSnapshotMarker(msg.getSender());
				}

				if (msg.getPayload() instanceof SnapshotToken) {
					tankModel.receiveSnapshotToken((SnapshotToken) msg.getPayload());
				}


			}
			System.out.println("Receiver stopped.");
		}
	}

	public ClientForwarder newClientForwarder() {
		return new ClientForwarder();
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

}
