package progetto;

import progetto.packet.Packet;
import progetto.session.SessionID;
import progetto.session.SessionListener;
import progetto.session.packet.SnapshotAckPacket;
import progetto.session.packet.SnapshotMarkerPacket;
import progetto.state.State;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

class Snapshot {
    private final UUID snapshotID;
    private final State state;
    private final Map<SessionID, Collection<Packet>> recordedPackets;
    private final HashSet<SessionID> pendingSessions;

    // Constructor for creating a snapshot with a unique snapshotID, a state object, and a collection of sessions
    public Snapshot(UUID snapshotID, State state, Collection<Session> sessions) {
        this.snapshotID = snapshotID;
        this.recordedPackets = new ConcurrentHashMap<>();
        this.pendingSessions = sessions.stream().map(Session::getID).collect(Collectors.toCollection(HashSet::new));
        this.state = state;
    }

    // Write the snapshot to a file
    public boolean writeToFile(File file) {
        if(!isSnapshotComplete())
            return false;

        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(snapshotID);
            out.writeObject(state);
            for (Map.Entry<SessionID, Collection<Packet>> packets : recordedPackets.entrySet()) {
                out.writeObject(packets.getKey());
                out.writeInt(packets.getValue().size());
                for(Packet packet: packets.getValue()) {
                    out.writeObject(packet);
                }
            }
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Get the snapshot ID
    public UUID getSnapshotID() {
        return snapshotID;
    }

    // Check if a session is pending in the snapshot
    public boolean isSessionPending(SessionID session) {
        synchronized (pendingSessions) {
            return pendingSessions.contains(session);
        }
    }

    // Mark a session as done in the snapshot
    public void markSessionAsDone(SessionID id) {
        synchronized (pendingSessions) {
            pendingSessions.remove(id);
        }
    }

    // Check if the snapshot is complete
    public boolean isSnapshotComplete() {
        synchronized(pendingSessions) {
            return pendingSessions.isEmpty();
        }
    }

    // Record a packet in the snapshot for a particular session
    public void recordPacket(SessionID id, Packet packet) {
        Collection<Packet> packets;
        if(recordedPackets.containsKey(id)) {
            packets = recordedPackets.get(id);
        } else {
            packets = new ArrayDeque<>();
            recordedPackets.put(id, packets);
        }
        packets.add(packet);
        recordedPackets.put(id, packets);
    }
}

public class DistributedNode implements SessionListener {
    private final Queue<Session> sessions;
    private final Map<UUID, Snapshot> snapshots;
    private final State state;
    private final Queue<Snapshot> activeSnapshots;

    // Constructor for creating a distributed node with a state object
    public DistributedNode(State state) {
        sessions = new ConcurrentLinkedQueue<>();
        this.snapshots = new ConcurrentHashMap<>();
        this.activeSnapshots = new ConcurrentLinkedQueue<>();
        this.state = state;
    }

    // Add a session to the distributed node
    public void addSession(Session session) {
        sessions.add(session);
        session.addListener(this);
    }


    //retuen a copy of session
    public Queue<Session> getSessions() {
        ConcurrentLinkedQueue<Session> clonedQueue = new ConcurrentLinkedQueue<>();
        clonedQueue.addAll(sessions);
        return clonedQueue;
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

            synchronized (this) {
                firstTime = !snapshots.containsKey(uuid);
                if(firstTime) {
                    Snapshot snapshot = new Snapshot(uuid, state.clone(), sessions.stream().filter(s -> s.getID().equals(session.getID())).toList());
                    if(snapshot.isSnapshotComplete()) {
                        //TODO listener for snapshot complete
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

            session.sendPacket(new SnapshotAckPacket(uuid));
        } else if(packet instanceof SnapshotAckPacket) {
            UUID snapshotID = ((SnapshotAckPacket) packet).getUuid();
            if(snapshots.containsKey(snapshotID)) {
                Snapshot snapshot = snapshots.get(snapshotID);
                snapshot.markSessionAsDone(session.getID());
                if(snapshot.isSnapshotComplete()) {
                    //TODO listener for snapshot complete
                    activeSnapshots.remove(snapshot);
                    snapshots.remove(snapshotID);
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