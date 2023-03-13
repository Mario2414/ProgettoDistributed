package progetto;

import progetto.packet.Packet;
import progetto.session.SessionListener;
import progetto.session.packet.SnapshotAck;
import progetto.session.packet.SnapshotMarker;
import progetto.state.State;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

class Snapshot {
    private final UUID snapshotID;
    private final State state;
    private final Map<UUID, List<Packet>> recordedPackets; //key is uuid of session?
    public Snapshot(UUID snapshotID, State state) {
        this.snapshotID = snapshotID;
        recordedPackets = new ConcurrentHashMap<>();
        this.state = state;
    }

    public void writeToFile() {
        try {
            File file = new File(snapshotID.toString() + ".snapshot");
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(snapshotID);
            out.writeObject(state);
            for (Map.Entry<UUID, List<Packet>> packets : recordedPackets.entrySet()) {
                out.writeObject(packets.getKey());
                out.writeInt(packets.getValue().size());
                for(Packet packet: packets.getValue()) {
                    out.writeObject(packet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

public class DistributedNode implements SessionListener {
    private final Queue<Session> sessions;
    private final Map<UUID, Snapshot> snapshots;
    private final State state;

    public DistributedNode(State state) {
        sessions = new ConcurrentLinkedQueue<>();
        this.snapshots = new ConcurrentHashMap<>();
        this.state = state;
    }

    public void addSession(Session session) {
        sessions.add(session);
        session.addListener(this);
    }

    public void snapshot() {
        try {
            UUID uuid = UUID.randomUUID();
            snapshots.put(uuid, new Snapshot(uuid, state.clone()));
            sessions.forEach(s -> s.sendPacket(new SnapshotMarker(uuid)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //TODO
    }

    @Override
    public void onPacketReceived(Session session, Packet packet) {
        if(packet instanceof SnapshotMarker) {
            UUID uuid = ((SnapshotMarker) packet).getUuid();
            boolean firstTime;
            Snapshot snapshot;
            synchronized (this) {
                firstTime = !snapshots.containsKey(uuid);
                if(firstTime) {
                    snapshot = new Snapshot(uuid, state.clone());
                    snapshots.put(uuid, snapshot);
                } else {
                    snapshot = snapshots.get(uuid);
                }
            }

            if(firstTime) {
                sessions.forEach(otherSession -> {
                    if(otherSession != session) {
                        otherSession.sendPacket(packet);
                    }
                });
            }

            session.sendPacket(new SnapshotAck(uuid));
            //TODO save
        } else if(packet instanceof SnapshotAck) {
            //TODO
        }

        //TODO save packets from session
    }

    @Override
    public void onPacketSent(Session session, Packet packet) {

    }

    @Override
    public void onConnected(Session session, Packet packet) {

    }

    @Override
    public void onDisconnection(Session session, Throwable exception) {
        sessions.remove(session);
    }
}
