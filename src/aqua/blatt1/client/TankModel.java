package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.RecordState;
import aqua.blatt1.common.Reference;
import aqua.blatt1.common.msgtypes.*;

import static aqua.blatt1.common.RecordState.*;

public class TankModel extends Observable implements Iterable<FishModel> {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 50;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected final Set<FishModel> fishies;
	protected int fishCounter = 0;
	protected final ClientCommunicator.ClientForwarder forwarder;
	private InetSocketAddress leftNeighbor;
	private InetSocketAddress rightNeighbor;
	private boolean hasToken;
	private Timer timer = new Timer();
	private int localFishCounter;
	private RecordState recordState = RecordState.IDLE;
	private volatile boolean localSnapshotDone = false;
	private boolean initiator = false;
	private int globalSnapShot = 0;
	private boolean showGlobalSnapshotDialog = false;
	/* Changed in Übungsblatt 5 Aufgabe 2
	protected HashMap<String, Reference> fishReferences = new HashMap<>();
	*/
	private HashMap<String, InetSocketAddress> homeAgent = new HashMap<>();

	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
	}

	synchronized void onRegistration(String id) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
	}

	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
			y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
			/* Changed in Übungsblatt 5 Aufgabe 2
			fishReferences.put(fish.getId(), Reference.HERE);
			 */
			homeAgent.put(fish.getId(), null);
		}
	}

	synchronized void receiveFish(FishModel fish) {
		fish.setToStart();
		fishies.add(fish);
		if (fish.getDirection() == Direction.RIGHT && (recordState == BOTH || recordState == LEFT)) {
			localFishCounter++;
		}
		if (fish.getDirection() == Direction.LEFT && (recordState == BOTH || recordState == RIGHT)) {
			localFishCounter++;
		}
		/* Changed in Übungsblatt 5 Aufgabe 2
		if (!fishReferences.containsKey(fish)) {
			fishReferences.put(fish.getId(), Reference.HERE);
		} else {
			fishReferences.replace(fish.getId(), Reference.HERE);
		}
		*/
		if (homeAgent.containsKey(fish.getId())) {
			homeAgent.replace(fish.getId(), null);
		} else {
			NameResolutionRequest nameResolutionRequest = new NameResolutionRequest(fish.getTankId(), fish.getId());
			forwarder.sendNameResolutionRequest(nameResolutionRequest);
		}
	}

	public String getId() {
		return id;
	}

	public synchronized int getFishCounter() {
		return fishCounter;
	}

	public synchronized Iterator<FishModel> iterator() {
		return fishies.iterator();
	}

	public synchronized void finish() {
		if (hasToken) {
			forwarder.sendToken(leftNeighbor, new Token());
		}
		forwarder.deregister(id);
	}

	public void setLeftNeighbor(InetSocketAddress leftNeighbor) {
		this.leftNeighbor = leftNeighbor;
	}

	public InetSocketAddress getLeftNeighbor() {
		return leftNeighbor;
	}

	public void setRightNeighbor(InetSocketAddress rightNeighbor) {
		this.rightNeighbor = rightNeighbor;
	}

	public InetSocketAddress getRightNeighbor() {
		return rightNeighbor;
	}

	public void receiveToken(Token token) {
		this.hasToken = true;
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				hasToken = false;
				forwarder.sendToken(leftNeighbor, token);
			}
		}, 5000);
	}

	public boolean hasToken() {
		return this.hasToken;
	}

	public void initiateSnapshot() {
		initiator = true;
		//1. Zustand speichern
		localFishCounter = fishies.size();
		//2. Aufnahme beider Eingangskanäle
		recordState = RecordState.BOTH;
		//3. Marker senden an alle Ausgangskanäle
		if (!leftNeighbor.equals(rightNeighbor)) {
			forwarder.sendSnapshotMarker(leftNeighbor);
			forwarder.sendSnapshotMarker(rightNeighbor);
		} else {
			forwarder.sendSnapshotMarker(leftNeighbor);
		}
		//wenn fertig, alle Ergebnisse local Snapshots sammeln
				while (!localSnapshotDone) {

				}
				Collector collector = new Collector(localFishCounter);
				forwarder.sendCollector(leftNeighbor, collector);
	}

	public void localSnapshot(InetSocketAddress sender) {
			//falls nicht bereits Aufzeichnen
			if (recordState == RecordState.IDLE) {
				System.out.println("Client: " + id + " fängt an aufzuzeichnen");
				//1. Zustand speichern
				localFishCounter = fishies.size();

				//2. andere Eingangskanäle aufnehmen
				if (!leftNeighbor.equals(rightNeighbor)) {
					if (sender.equals(leftNeighbor)) {
						recordState = RIGHT;
					} else if (sender.equals(rightNeighbor)) {
						recordState = LEFT;
					}
					//3. sende Markierer an alle Ausgangskanäle
					forwarder.sendSnapshotMarker(leftNeighbor);
					forwarder.sendSnapshotMarker(rightNeighbor);
				} else {
					recordState = BOTH;
					forwarder.sendSnapshotMarker(leftNeighbor);
				}

			}

			//falls bereits Aufzeichnet
			if (recordState != RecordState.IDLE) {
				System.out.println("Client: " + id + " hört auf aufzuzeichnen");
				//Beenden Aufzeichnung des Eingangskanals (der Markierer sendet)
				if (!leftNeighbor.equals(rightNeighbor)) {
					if (sender.equals(leftNeighbor)) {
						if (recordState == BOTH) {
							recordState = RIGHT;
						}
						if (recordState == LEFT)
							recordState = IDLE;
							localSnapshotDone = true;
					}
					if (sender.equals(rightNeighbor)) {
						if (recordState == BOTH) {
							recordState = LEFT;
						}
						if (recordState == RIGHT) {
							recordState = IDLE;
							localSnapshotDone = true;
						}
					}
				} else {
					recordState = IDLE;
					localSnapshotDone = true;
				}
			}
	}

	public void addLocalSnapShotToCollector(Collector collector) {
		if (initiator) {
			initiator = false;
			globalSnapShot = collector.getGlobalFishCounter();
			showGlobalSnapshotDialog = true;
		} else {
			while (!localSnapshotDone) {

			}
			collector.setGlobalFishCounter(collector.getGlobalFishCounter() + localFishCounter);
			forwarder.sendCollector(leftNeighbor, collector);
			localSnapshotDone = false;
		}
	}

	public int getGlobalSnapShot() {
		return this.globalSnapShot;
	}

	public boolean getShowGlobalSnapshotDialog() {
		return this.showGlobalSnapshotDialog;
	}

	public void setShowGlobalSnapshotDialog(boolean showGlobalSnapshotDialog) {
		this.showGlobalSnapshotDialog = showGlobalSnapshotDialog;
	}

	public void locateFishGlobally(String fish, String fishTankId) {
		/* Changed in Übungsblatt 5 Aufgabe 2
			Reference fishReference = fishReferences.get(fish);
			if (fishReference == Reference.HERE) {
				locateFishLocally(fish);
			} else if (fishReference == Reference.LEFT) {
				forwarder.sendLocationRequest(leftNeighbor, new LocationRequest(fish));
			} else{
				forwarder.sendLocationRequest(rightNeighbor, new LocationRequest(fish));
			}
		*/
			InetSocketAddress fishCurrentAddress = homeAgent.get(fish);
			if (fishCurrentAddress == null) {
				locateFishLocally(fish);
			} else {
				LocationRequest locationRequest = new LocationRequest(fish);
				forwarder.sendLocationRequest(fishCurrentAddress, locationRequest);
			}
	}

	public void locateFishLocally(String fish) {
		for (FishModel f : fishies) {
			if (f.getId().equals(fish)) {
				f.toggle();
				break;
			}
		}
	}

	public void informHomeAgent(NameResolutionResponse nameResolutionResponse) {
		String fishId = nameResolutionResponse.getRequestId();
		InetSocketAddress fishHomeAgent = nameResolutionResponse.getAddressTankId();
		InetSocketAddress ownAddress = nameResolutionResponse.getNewFishAddress();
		LocationUpdate locationUpdate = new LocationUpdate(fishId, ownAddress);
		forwarder.sendLocationUpdate(fishHomeAgent, locationUpdate);
	}

	public void updateCurrentFishAddress(String fishId, InetSocketAddress currentAddress) {
		homeAgent.replace(fishId, currentAddress);
	}

	protected void run() {
		forwarder.register();

		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}

	private synchronized void updateFishies() {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge()) {
				if (this.hasToken) {
					/* Changed in Übungsblatt 5 Aufgabe 2
					fishReferences.replace(fish.getId(), fish.getDirection() == Direction.LEFT ? Reference.LEFT : Reference.RIGHT);
					*/
					forwarder.handOff(fish, leftNeighbor, rightNeighbor);
				} else {
					fish.reverse();
				}
			}

			if (fish.disappears())
				it.remove();
		}
	}

	private synchronized void update() {
		updateFishies();
		setChanged();
		notifyObservers();
	}

}