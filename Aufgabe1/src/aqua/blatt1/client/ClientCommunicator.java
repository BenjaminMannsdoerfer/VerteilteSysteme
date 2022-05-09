package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

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

		public void sendNameResolutionRequest(String tankId, String requestId) {
			endpoint.send(broker, new NameResolutionRequest(tankId, requestId));
		}

		public void sendLocationRequest(InetSocketAddress neighbor, String fishId) {
			endpoint.send(neighbor, new LocationRequest(fishId));
		}

		public void sendLocationUpdate(InetSocketAddress addressHomeTank, String fishId) {
			endpoint.send(addressHomeTank, new LocationUpdate(fishId, addressHomeTank));
		}

		public void sendSnapshotMarker(InetSocketAddress address) {
			endpoint.send(address, new SnapshotMarker());
		}
	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;
		private final Timer timer = new Timer();

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();

				if (msg.getPayload() instanceof RegisterResponse) {
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId(), ((RegisterResponse) msg.getPayload()).getLeaseTime());
				}

				if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());

				if (msg.getPayload() instanceof NeighborUpdate) {
					tankModel.setNeighborAddressLeft(((NeighborUpdate) msg.getPayload()).getLeftNeighborAddress());
					tankModel.setNeighborAddressRight(((NeighborUpdate) msg.getPayload()).getRightNeighborAddress());
				}
				if (msg.getPayload() instanceof Token)
					tankModel.receiveToken();

				if (msg.getPayload() instanceof SnapshotMarker) {
					tankModel.receiveSnapshotMarker(msg.getSender());
				}

				if (msg.getPayload() instanceof SnapshotToken) {
					tankModel.receiveSnapshotToken((SnapshotToken) msg.getPayload());
				}

				// Antwort vom Broker
				if (msg.getPayload() instanceof NameResolutionResponse) {
					tankModel.receiveNameResolutionResponse(((NameResolutionResponse) msg.getPayload()).getAddress(), ((NameResolutionResponse) msg.getPayload()).getRequestId());
				}

				if (msg.getPayload() instanceof LocationUpdate) {
					tankModel.receiveLocationUpdate(((LocationUpdate) msg.getPayload()).getFishId(), msg.getSender());
				}

				if (msg.getPayload() instanceof LocationRequest) {
					tankModel.locateFishLocally(((LocationRequest) msg.getPayload()).getFishId());
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
