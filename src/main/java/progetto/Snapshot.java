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

    /**
     * Constructor for creating a snapshot
     * @param snapshotID unique id of the snapshot
     * @param date Starting time of the snapshot
     * @param state State to be saved
     * @param sessions Sessions pending for an ack
     */
    public Snapshot(UUID snapshotID, LocalDateTime date, State state, Collection<Session<ID>> sessions) {
        this.snapshotID = snapshotID;
        this.date = date;
        this.recordedPackets = new ConcurrentHashMap<>();
        this.pendingSessions = sessions.stream().map(Session::getID).collect(Collectors.toCollection(HashSet::new));
        this.state = state;
    }

    /**
     * Write the snapshot to a file if the snapshot is complete
     * @param file File to write the snapshot to.
     * @return true if the snapshot was saved correctly. false otherwise.
     */
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

    /**
     * Get the snapshot ID
     * @return Snapshot unique id
     */
    public UUID getSnapshotID() {
        return snapshotID;
    }

    public LocalDateTime getDate() {
        return date;
    }

    /**
     * Check if a session is pending in the snapshot
     * @param session Session to check.
     * @return true if the session has not yet sent an ack
     */
    public boolean isSessionPending(ID session) {
        synchronized (pendingSessions) {
            return pendingSessions.contains(session);
        }
    }

    /**
     * Mark a session as done in the snapshot, after an ACK is received
     */
    public void markSessionAsDone(ID id) {
        synchronized (pendingSessions) {
            pendingSessions.remove(id);
        }
    }

    /**
     * Check if the snapshot is complete
     * @return Returns true if all sessions initially pending sent an ack and called markSessionAsDone
     */
    public boolean isSnapshotComplete() {
        synchronized(pendingSessions) {
            return pendingSessions.isEmpty();
        }
    }

    /**
     * Record a packet in the snapshot for a particular session (must be pending!).
     * @param session Session that sent the packet
     * @param packet Packet to record.
     */
    public void recordPacket(Session<ID> session, Packet packet) {
        assert isSessionPending(session.getID());

        Collection<Packet> packets;
        if(recordedPackets.containsKey(session.getID())) {
            packets = recordedPackets.get(session.getID());
        } else {
            packets = new ArrayDeque<>();
            recordedPackets.put(session.getID(), packets);
        }
        packets.add(packet);
        recordedPackets.put(session.getID(), packets);
    }
}