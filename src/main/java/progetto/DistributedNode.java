package progetto;

import progetto.packet.Packet;
import progetto.session.ServerListener;
import progetto.session.SessionListener;
import progetto.packet.SnapshotAckPacket;
import progetto.packet.SnapshotMarkerPacket;
import progetto.state.State;
import progetto.utils.Const;
import progetto.utils.ListenerList;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The node is used to keep active sessions and handle snapshots.
 * @param <ID> Type of the unique ID for sessions.
 */
public class DistributedNode<ID extends Comparable<ID> & Serializable> implements SessionListener<ID>, ServerListener<ID> {
    protected final Queue<Session<ID>> sessions;
    protected final Queue<Server<ID>> servers;
    protected final Map<UUID, Snapshot<ID>> snapshots;
    protected final State state;
    protected final Queue<Snapshot<ID>> activeSnapshots;
    protected final ListenerList<DistributedNodeListener<ID>> listeners;

    /**
     * Constructor for creating a distributed node with a state object
     * @param state The state object (model in a MVN pattern) for the node.
     */
    public DistributedNode(State state) {
        sessions = new ConcurrentLinkedQueue<>();
        servers = new ConcurrentLinkedQueue<>();
        this.snapshots = new ConcurrentHashMap<>();
        this.activeSnapshots = new ConcurrentLinkedQueue<>();
        listeners = new ListenerList<>();
        this.state = state;
    }

    /**
     *
     * @param snapshotFile Snapshot file to restore the state from
     * @param sessions Lists of connected sessions.
     * @param servers List of servers
     * @throws IOException Thrown if the snapshotFile can't be found.
     * @throws ClassNotFoundException Thrown if deserialization fails.
     */
    public DistributedNode(File snapshotFile, List<Session<ID>> sessions, List<Server<ID>> servers) throws IOException, ClassNotFoundException {
        this.sessions = new ConcurrentLinkedQueue<>(sessions);
        this.servers = new ConcurrentLinkedQueue<>(servers);
        this.snapshots = new ConcurrentHashMap<>();
        this.activeSnapshots = new ConcurrentLinkedQueue<>();
        listeners = new ListenerList<>();
        sessions.forEach(s -> s.addListener(this));
        servers.forEach(s -> {
            s.addServerListener(this);
            for(Session<ID> session : s.getActiveSessions()) {
                if(!sessions.contains(session)) {
                    addSession(session);
                }
            }
        });
        this.state = restoreSnapshot(snapshotFile);
    }

    /**
     * Restore the state from a snapshot file
     * @param snapshotFile The file to restore the snapshot from
     * @return The restored snapshot (same as this.getState())
     * @throws IOException Thrown if the snapshotFile can't be found.
     * @throws ClassNotFoundException Thrown if deserialization fails.
     */
    public State restoreSnapshot(File snapshotFile)  throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(snapshotFile));
        UUID snapshotID = (UUID) in.readObject();
        LocalDateTime date = (LocalDateTime) in.readObject();
        this.state.restore((State) in.readObject());
        int size = in.readInt();
        Map<ID, Collection<Packet>> packetsToRestore = new HashMap<>();
        for(int i = 0; i < size; i++) {
            ID id = (ID) in.readObject();
            int packetsSize = in.readInt();
            if(Const.DEBUG) System.out.println("Packets to restore " + packetsSize);
            List<Packet> packets = new ArrayList<>();
            for(int packetCount = 0; packetCount < packetsSize; packetCount++) {
                Packet packet = (Packet) in.readObject();
                packets.add(packet);
                if(Const.DEBUG) System.out.println("Restoring " + packet.getClass().getSimpleName());
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

    /**
     * Add a session to the distributed node, to allow handling of snapshots.
     * @param session Session to be added to the Distributed node
     */
    public void addSession(Session<ID> session) {
        sessions.add(session);
        session.addListener(this);
    }

    /**
     * Add a server to the distributed node.
     * New Sessions accepted by the server will be automatically added to the DistributedNode
     * @param server Server to be added.
     */
    public void addServer(Server<ID> server) {
        servers.add(server);
        server.addServerListener(this);
    }

    /**
     * Add a listener to DistributedNode for snapshot events
     * @param listener The listener to register
     */
    public void addListener(DistributedNodeListener<ID> listener) {
        listeners.addListener(listener);
    }

    /**
     * Get the sessions for the distributed node.
     * @return The copy of the session list
     */
    public Queue<Session<ID>> getSessions() {
        return new ConcurrentLinkedQueue<>(sessions);
    }

    /**
     * Start the snapshot procedure.
     */
    public void snapshot() {
        try {
            UUID uuid = UUID.randomUUID();
            LocalDateTime date = LocalDateTime.now();
            Snapshot<ID> snapshot = new Snapshot<>(uuid, date, state.clone(), sessions);
            snapshots.put(uuid, snapshot);
            activeSnapshots.add(snapshot);
            sessions.forEach(s -> s.sendPacket(new SnapshotMarkerPacket(uuid, date)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPacketReceived(Session<ID> session, Packet packet) {
        //This block must be seen as a single atomic operation
        synchronized (this) {
            if(packet instanceof SnapshotMarkerPacket) {
                UUID uuid = ((SnapshotMarkerPacket) packet).getUuid();
                boolean firstTime = !snapshots.containsKey(uuid);
                if(firstTime) {
                    Snapshot<ID> snapshot = new Snapshot<>(uuid, ((SnapshotMarkerPacket) packet).getDate(), state.clone(), sessions.stream().filter(s -> !s.getID().equals(session.getID())).toList());
                    if(snapshot.isSnapshotComplete()) {
                        listeners.forEachListeners(l -> l.onShapshotCompleted(this, snapshot));
                    } else {
                        snapshots.put(uuid, snapshot);
                        activeSnapshots.add(snapshot);
                    }

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
                    Snapshot<ID> snapshot = snapshots.get(snapshotID);
                    snapshot.markSessionAsDone(session.getID());
                    if (snapshot.isSnapshotComplete()) {
                        listeners.forEachListeners(l -> l.onShapshotCompleted(this, snapshot));
                        activeSnapshots.remove(snapshot);
                    }
                }
            }

            for(Snapshot<ID> snapshot: activeSnapshots) {
                if(snapshot.isSessionPending(session.getID())) {
                    snapshot.recordPacket(session, packet);
                }
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

    @Override
    public void onSessionAccepted(Server<ID> server, Session<ID> session) {
        addSession(session);
    }

    @Override
    public void onSessionClosed(Server<ID> server, Session<ID> session) {
        //not needed, because it is handled by SessionListener.onDisconnection
    }

    @Override
    public void onServerClosed(Server<ID> server, Throwable t) {
        servers.remove(server);
    }
}