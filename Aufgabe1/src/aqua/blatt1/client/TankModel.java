package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.sql.Ref;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.SnapshotToken;
import com.sun.source.tree.MemberReferenceTree;

public class TankModel extends Observable implements Iterable<FishModel> {

    public static final int WIDTH = 600;
    public static final int HEIGHT = 350;
    protected static final int MAX_FISHIES = 5;
    protected static final Random rand = new Random();
    protected volatile String id;
    protected final Set<FishModel> fishies;
    protected int fishCounter = 0;
    protected final ClientCommunicator.ClientForwarder forwarder;
    private InetSocketAddress addressLeft;
    private InetSocketAddress addressRight;
    private boolean checkToken = Boolean.TRUE;
    private final Timer timer = new Timer();

    private enum RecordingMode {IDLE, LEFT, RIGHT, BOTH}

    private RecordingMode mode = RecordingMode.IDLE;
    private int arrivingFish;
    private int totalFishies;
    private boolean initiator = false;
    private int snapshot;
    private boolean capture = false;

    private enum ReferenceMode {HERE, LEFT, RIGHT}

    private final Map<FishModel, ReferenceMode> referenceFishies;

    public TankModel(ClientCommunicator.ClientForwarder forwarder) {
        this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
        this.forwarder = forwarder;
        referenceFishies = new HashMap<>();
    }

    public InetSocketAddress getNeighborAddressLeft() {
        return this.addressLeft;
    }

    public void setNeighborAddressLeft(InetSocketAddress addressLeft) {
        this.addressLeft = addressLeft;
    }

    public InetSocketAddress getNeighborAddressRight() {
        return this.addressRight;
    }

    public void setNeighborAddressRight(InetSocketAddress addressRight) {
        this.addressRight = addressRight;
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
            referenceFishies.put(fish, ReferenceMode.HERE);
        }
    }

    synchronized void receiveFish(FishModel fish) {
        fish.setToStart();
        fishies.add(fish);
        // Befindet sich ein Kanal im Aufzeichnungsmodus, so müssen auf diesem Kanal
        // ankommende Fische dem lokalen Zustand hinzugefügt werden. Passen Sie hierfür
        // die Methode TankModel.receiveFish() entsprechend an.
        switch (mode) {
            case IDLE:
                break;
            case RIGHT:
                if (fish.getDirection() == Direction.RIGHT)
                    arrivingFish++;
                break;
            case LEFT:
                if (fish.getDirection() == Direction.LEFT)
                    arrivingFish++;
                break;
            case BOTH:
                arrivingFish++;
                break;
        }
        referenceFishies.put(fish, ReferenceMode.HERE);
    }

    public String getId() {
        return id;
    }

    public synchronized int getFishCounter() {
        return fishCounter;
    }

    public synchronized int getSnapshot() {
        return snapshot;
    }

    public synchronized Iterator<FishModel> iterator() {
        return fishies.iterator();
    }

    private synchronized void updateFishies() {
        for (Iterator<FishModel> it = iterator(); it.hasNext(); ) {
            FishModel fish = it.next();
            fish.update();

            if (fish.hitsEdge()) {
                if (!hasToken()) {
                    fish.reverse();
                } else {
                    switch (mode) {
                        case IDLE:
                            break;
                        case RIGHT:
                            if (fish.getDirection() == Direction.RIGHT)
                                referenceFishies.put(fish, ReferenceMode.RIGHT);
                                arrivingFish--;
                            break;
                        case LEFT:
                            if (fish.getDirection() == Direction.LEFT)
                                referenceFishies.put(fish, ReferenceMode.LEFT);
                                arrivingFish--;
                            break;
                        case BOTH:
                            arrivingFish--;
                            break;
                    }
                    forwarder.handOff(fish, this);
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

    public synchronized void finish() {
        forwarder.deregister(id);
    }

    public void receiveToken() {
        checkToken = Boolean.TRUE;
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                checkToken = Boolean.FALSE;
                forwarder.sendToken(getNeighborAddressLeft());
            }
        };
        long delay = 2000L;
        timer.schedule(task, delay);
    }

    public boolean hasToken() {
        return checkToken;
    }

    public void initiateSnapshot(InetSocketAddress address) {
        if (mode == RecordingMode.IDLE) {
            if (address.getHostName().equals("Snapshot")) {
                initiator = true;
                mode = RecordingMode.BOTH;
            } else if (address.equals(getNeighborAddressLeft())) {
                mode = RecordingMode.RIGHT;
            } else if (address.equals(getNeighborAddressRight())) {
                mode = RecordingMode.LEFT;
            }
            totalFishies = 0;
            arrivingFish = 0;
            localSnapshot();
            forwarder.sendSnapshotMarker(getNeighborAddressLeft());
            forwarder.sendSnapshotMarker(getNeighborAddressRight());
        } else {
            if (mode == RecordingMode.BOTH) {
                if (address.equals(getNeighborAddressLeft())) {
                    mode = RecordingMode.RIGHT;
                } else if (address.equals(getNeighborAddressRight())) {
                    mode = RecordingMode.LEFT;
                }
            } else if ((mode == RecordingMode.LEFT && address.equals(getNeighborAddressLeft())) ||
                    (mode == RecordingMode.RIGHT && address.equals(getNeighborAddressRight()))) {
                mode = RecordingMode.IDLE;
                snapshot = totalFishies + arrivingFish;
                if (initiator) {
                    forwarder.sendSnapshotToken(getNeighborAddressLeft(), new SnapshotToken());
                }
            }
        }
    }

    private void localSnapshot() {
        this.forEach(fish -> {
            if (!fish.leavingTank())
                totalFishies++;
        });
    }

    public void setCapture() {
        this.capture = false;
    }

    public boolean isCapture() {
        return capture;
    }

    public void receiveSnapshotToken(SnapshotToken payload) {
        payload.setSnapshot(snapshot);
        if (initiator) {
            snapshot = payload.getSnapshot();
            capture = true;
            initiator = false;
        } else {
            forwarder.sendSnapshotToken(getNeighborAddressLeft(), payload);
        }
    }

    public void receiveSnapshotMarker(InetSocketAddress address) {
        initiateSnapshot(address);
    }

    public void locateFishGlobally(String fishId) {
        for (var fish : referenceFishies.entrySet()) {
            if (fish.getKey().getId().equals(fishId)) {
                switch (fish.getValue()) {
                    case HERE:
                        locateFishLocally(fish.getKey());
                        break;
                    case LEFT:
                        forwarder.sendLocationRequest(getNeighborAddressLeft(), fishId);
                        break;
                    case RIGHT:
                        forwarder.sendLocationRequest(getNeighborAddressRight(), fishId);
                        break;
                }
            }
        }
    }

    private void locateFishLocally(FishModel fishModel) {
        if (fishies.contains(fishModel)) {
            fishModel.toggle();
        }
    }
}