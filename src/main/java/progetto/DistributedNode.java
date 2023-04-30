package progetto;

import progetto.packet.Packet;
import progetto.session.SessionID;
import progetto.session.SessionListener;
import progetto.session.packet.SnapshotAckPacket;
import progetto.session.packet.SnapshotMarkerPacket;
import progetto.state.State;
import progetto.utils.ListenerList;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DistributedNode implements SessionListener {
    private final Queue<Session> sessions;
    private final Map<UUID, Snapshot> snapshots;
    private State state;
    private final Queue<Snapshot> activeSnapshots;
    private final ListenerList<DistributedNodeListener> listeners;

    // Constructor for creating a distributed node with a state object
    public DistributedNode(State state) {
        sessions = new ConcurrentLinkedQueue<>();
        this.snapshots = new ConcurrentHashMap<>();
        this.activeSnapshots = new ConcurrentLinkedQueue<>();
        listeners = new ListenerList<>();
        this.state = state;
    }

    public DistributedNode(File snapshotFile, List<Session> sessions) throws IOException, ClassNotFoundException {
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
        Map<SessionID, Collection<Packet>> packetsToRestore = new HashMap<>();
        for(int i = 0; i < size; i++) {
            SessionID id = (SessionID) in.readObject();
            int packetsSize = in.readInt();
            List<Packet> packets = new ArrayList<>();
            for(int packetCount = 0; packetCount < packetsSize; packetCount++) {
                Packet packet = (Packet) in.readObject();
                packets.add(packet);
            }
            packetsToRestore.put(id, packets);
        }

        for(Session session : sessions) {
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
    public void addSession(Session session) {
        sessions.add(session);
        session.addListener(this);
    }

    public void addListener(DistributedNodeListener listener) {
        listeners.addListener(listener);
    }

    //return a copy of session
    public Queue<Session> getSessions() {
        return new ConcurrentLinkedQueue<>(sessions);
    }

    public void snapshot() {
        try {
            UUID uuid = UUID.randomUUID();
            Snapshot snapshot = new Snapshot(uuid, state.clone(), sessions);
            snapshots.put(uuid, snapshot);
            sessions.forEach(s -> s.sendPacket(new SnapshotMarkerPacket(uuid)));
            activeSnapshots.add(snapshot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPacketReceived(Session session, Packet packet) {
        if(packet instanceof SnapshotMarkerPacket) {
            UUID uuid = ((SnapshotMarkerPacket) packet).getUuid();
            boolean firstTime;

            //This block must be seen as a single atomic operation
            synchronized (this) {
                firstTime = !snapshots.containsKey(uuid);
                if(firstTime) {
                    Snapshot snapshot = new Snapshot(uuid, state.clone(), sessions.stream().filter(s -> s.getID().equals(session.getID())).toList());
                    if(snapshot.isSnapshotComplete()) {
                        listeners.forEachListeners(l -> l.onShapshotCompleted(this));
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
                Snapshot snapshot = snapshots.get(snapshotID);
                //synchronization block is needed to guarantee that snapshot complete is called only once
                synchronized (this) {
                    snapshot.markSessionAsDone(session.getID());
                    if (snapshot.isSnapshotComplete()) {
                        listeners.forEachListeners(l -> l.onShapshotCompleted(this));
                        activeSnapshots.remove(snapshot);
                        snapshots.remove(snapshotID);
                    }
                }
            }
        }

        for(Snapshot snapshot: activeSnapshots) {
            if(snapshot.isSessionPending(session.getID())) {
                snapshot.recordPacket(session.getID(), packet);
            }
        }
    }
    

    @Override
    public void onPacketSent(Session session, Packet packet) {

    }

    @Override
    public void onConnected(Session session) {

    }

    @Override
    public void onDisconnection(Session session, Throwable exception) {
        sessions.remove(session);
    }
}