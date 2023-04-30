package progetto;

import progetto.packet.Packet;
import progetto.session.SessionID;
import progetto.state.State;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Snapshot {
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
            out.writeInt(recordedPackets.size());
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