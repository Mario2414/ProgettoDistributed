package progetto;

import progetto.packet.Packet;
import progetto.state.State;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Snapshot<ID extends Comparable<ID> & Serializable> {
    private final UUID snapshotID;

    private final LocalDateTime date;
    private final State state;
    private final Map<ID, Collection<Packet>> recordedPackets;
    private final HashSet<ID> pendingSessions;

    // Constructor for creating a snapshot with a unique snapshotID, a state object, and a collection of sessions
    public Snapshot(UUID snapshotID, LocalDateTime date, State state, Collection<Session<ID>> sessions) {
        this.snapshotID = snapshotID;
        this.date = date;
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
            out.writeObject(date);
            out.writeObject(state);
            out.writeInt(recordedPackets.size());
            for (Map.Entry<ID, Collection<Packet>> packets : recordedPackets.entrySet()) {
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

    public LocalDateTime getDate() {
        return date;
    }

    // Check if a session is pending in the snapshot
    public boolean isSessionPending(ID session) {
        synchronized (pendingSessions) {
            return pendingSessions.contains(session);
        }
    }

    // Mark a session as done in the snapshot
    public void markSessionAsDone(ID id) {
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
    public void recordPacket(ID id, Packet packet) {
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