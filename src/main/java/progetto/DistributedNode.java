package progetto;

import progetto.packet.Packet;
import progetto.session.SessionListener;
import progetto.session.packet.SnapshotAckPacket;
import progetto.session.packet.SnapshotMarkerPacket;
import progetto.state.State;
import progetto.utils.ListenerList;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/*
* TODO
*  Problem with concurrency:
* Le operazioni sullo state risultano asincrone (le varie session operano su thread diversi).
* Si potrebbe scaricare l'onore di rendere di rendere lo state thread safe all'utente, ma serve rendere anche
* il deep copy dello state (durante il write dello snapshot) thread safe
* Opzione A: rendere tutto single threaded
* Opzione B: pensare a un modo per rendere il write dello snapshot thread safe
* */

/*
* TODO
*  Problem with circular networks topology
* */

public class DistributedNode<ID extends Comparable<ID> & Serializable> implements SessionListener<ID> {
    protected final Queue<Session<ID>> sessions;
    protected final Map<UUID, Snapshot<ID>> snapshots;
    protected State state;
    protected final Queue<Snapshot<ID>> activeSnapshots;
    protected final ListenerList<DistributedNodeListener<ID>> listeners;

    // Constructor for creating a distributed node with a state object
    public DistributedNode(State state) {
        sessions = new ConcurrentLinkedQueue<>();
        this.snapshots = new ConcurrentHashMap<>();
        this.activeSnapshots = new ConcurrentLinkedQueue<>();
        listeners = new ListenerList<>();
        this.state = state;
    }

    public DistributedNode(File snapshotFile, List<Session<ID>> sessions) throws IOException, ClassNotFoundException {
        this.sessions = new ConcurrentLinkedQueue<>(sessions);
        this.snapshots = new ConcurrentHashMap<>();
        this.activeSnapshots = new ConcurrentLinkedQueue<>();
        listeners = new ListenerList<>();
        sessions.forEach(s -> s.addListener(this));

        this.state = restoreSnapshot(snapshotFile);
    }

    public State restoreSnapshot(File snapshotFile)  throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(snapshotFile));
        UUID snapshotID = (UUID) in.readObject();
        this.state = (State) in.readObject();
        int size = in.readInt();
        Map<ID, Collection<Packet>> packetsToRestore = new HashMap<>();
        for(int i = 0; i < size; i++) {
            ID id = (ID) in.readObject();
            int packetsSize = in.readInt();
            List<Packet> packets = new ArrayList<>();
            for(int packetCount = 0; packetCount < packetsSize; packetCount++) {
                Packet packet = (Packet) in.readObject();
                packets.add(packet);
            }
            packetsToRestore.put(id, packets);
        }

        for(Session<ID> session : sessions) {
            if(packetsToRestore.containsKey(session.getID())) {
                Collection<Packet> packets = packetsToRestore.get(session.getID());
                for(Packet packet : packets) {
                    session.getListeners().forEach(l -> l.onPacketReceived(session, packet));
                }
            }
        }
        return state;
    }

    // Add a session to the distributed node
    public void addSession(Session<ID> session) {
        sessions.add(session);
        session.addListener(this);
    }

    public void addListener(DistributedNodeListener<ID> listener) {
        listeners.addListener(listener);
    }

    //return a copy of session
    public Queue<Session<ID>> getSessions() {
        return new ConcurrentLinkedQueue<>(sessions);
    }

    public void snapshot() {
        try {
            UUID uuid = UUID.randomUUID();
            LocalDateTime date = LocalDateTime.now();
            Snapshot<ID> snapshot = new Snapshot<>(uuid, date, state.clone(), sessions);
            snapshots.put(uuid, snapshot);
            sessions.forEach(s -> s.sendPacket(new SnapshotMarkerPacket(uuid, date)));
            activeSnapshots.add(snapshot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPacketReceived(Session<ID> session, Packet packet) {
        if(packet instanceof SnapshotMarkerPacket) {
            UUID uuid = ((SnapshotMarkerPacket) packet).getUuid();
            boolean firstTime;

            //This block must be seen as a single atomic operation
            synchronized (this) {
                firstTime = !snapshots.containsKey(uuid);
                if(firstTime) {
                    Snapshot<ID> snapshot = new Snapshot<>(uuid, ((SnapshotMarkerPacket) packet).getDate(), state.clone(), sessions.stream().filter(s -> s.getID().equals(session.getID())).toList());
                    if(snapshot.isSnapshotComplete()) {
                        listeners.forEachListeners(l -> l.onShapshotCompleted(this, snapshot));
                    } else {
                        snapshots.put(uuid, snapshot);
                        activeSnapshots.add(snapshot);
                    }
                }
            }

            if(firstTime) {
                sessions.forEach(otherSession -> {
                    if(otherSession != session) {
                        otherSession.sendPacket(packet);
                    }
                });
            }
            //Todo check this packet
            session.sendPacket(new SnapshotAckPacket(uuid));
        } else if(packet instanceof SnapshotAckPacket) {
            UUID snapshotID = ((SnapshotAckPacket) packet).getUuid();
            if(snapshots.containsKey(snapshotID)) {
                Snapshot<ID> snapshot = snapshots.get(snapshotID);
                //synchronization block is needed to guarantee that snapshot complete is called only once
                synchronized (this) {
                    snapshot.markSessionAsDone(session.getID());
                    if (snapshot.isSnapshotComplete()) {
                        listeners.forEachListeners(l -> l.onShapshotCompleted(this, snapshot));
                        activeSnapshots.remove(snapshot);
                        snapshots.remove(snapshotID);
                    }
                }
            }
        }

        for(Snapshot<ID> snapshot: activeSnapshots) {
            if(snapshot.isSessionPending(session.getID())) {
                snapshot.recordPacket(session.getID(), packet);
            }
        }
    }
    

    @Override
    public void onPacketSent(Session<ID> session, Packet packet) {

    }

    @Override
    public void onConnected(Session<ID> session) {

    }

    @Override
    public void onDisconnection(Session<ID> session, Throwable exception) {
        sessions.remove(session);
    }
}