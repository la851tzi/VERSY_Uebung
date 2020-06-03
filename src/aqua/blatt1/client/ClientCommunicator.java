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

		public void handOff(FishModel fish, InetSocketAddress leftNeighbor, InetSocketAddress rightNeighbor) {
			Direction fishDirection = fish.getDirection();
			if(fishDirection == Direction.LEFT) {
				endpoint.send(leftNeighbor, new HandoffRequest(fish));
			} else {
				endpoint.send(rightNeighbor, new HandoffRequest(fish));
			}
		}

		public void sendToken(InetSocketAddress toClient, Token token) {
			endpoint.send(toClient, token);
		}

		public void sendSnapshotMarker(InetSocketAddress toClient) {
			endpoint.send(toClient, new SnapshotMarker());
		}

		public void sendCollector(InetSocketAddress toClient, Collector collector) {
			endpoint.send(toClient, collector);
		}

		public void sendLocationRequest(InetSocketAddress toClient, LocationRequest locationRequest) {
			endpoint.send(toClient, locationRequest);
		}

		public void sendNameResolutionRequest(NameResolutionRequest nameResolutionRequest) {
			endpoint.send(broker, nameResolutionRequest);
		}

		public void sendLocationUpdate(InetSocketAddress toClient, LocationUpdate locationUpdate) {
			endpoint.send(toClient, locationUpdate);
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
					tankModel.setLeftNeighbor(((NeighborUpdate) msg.getPayload()).getLeftNeighbor());
					tankModel.setRightNeighbor(((NeighborUpdate) msg.getPayload()).getRightNeighbor());
				}

				if (msg.getPayload() instanceof Token) {
					tankModel.receiveToken((Token) msg.getPayload());
				}

				if (msg.getPayload() instanceof  SnapshotMarker) {
					tankModel.localSnapshot(msg.getSender());
				}

				if (msg.getPayload() instanceof Collector) {
					tankModel.addLocalSnapShotToCollector((Collector) msg.getPayload());
				}

				if (msg.getPayload() instanceof LocationRequest) {
					/* Changed in Übungsblatt 5 Aufgabe 2
					tankModel.locateFishGlobally(((LocationRequest) msg.getPayload()).getFish());
					*/
					tankModel.locateFishLocally(((LocationRequest) msg.getPayload()).getFish());
				}

				if (msg.getPayload() instanceof NameResolutionResponse) {
					tankModel.informHomeAgent((NameResolutionResponse) msg.getPayload());
				}

				if (msg.getPayload() instanceof LocationUpdate) {
					tankModel.updateCurrentFishAddress(((LocationUpdate) msg.getPayload()).getFishId(), ((LocationUpdate) msg.getPayload()).getNewFishAddress());
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
