package App;

import App.packets.ArrivingGoods;
import App.packets.SnapshotRestoreAckPacket;
import App.packets.SnapshotRestorePacket;
import App.packets.SomeoneDown;
import progetto.DistributedNode;
import progetto.DistributedNodeListener;
import progetto.Session;
import progetto.Snapshot;
import progetto.packet.Packet;
import progetto.session.packet.SnapshotMarkerPacket;
import progetto.state.State;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

public class MyAppDistributedNode extends DistributedNode<Integer> implements DistributedNodeListener<Integer> {
    private final Map<UUID, SnapshotRestore> snapshotsRestore;
    private List<MyAppClientSession> outgoingLinks;
    private StateApp state;

    private MyAppServer myAppServer;

    private long productionTime;

    private float multiplier;

    public MyAppDistributedNode(StateApp state) {
        super(state);
        this.state = state;
        outgoingLinks = new ArrayList<MyAppClientSession>();

        snapshotsRestore = new HashMap<UUID, SnapshotRestore>();
        this.addListener(this);

        try {
            System.out.println("Reading config parameters");
            ConfigReader parameters = new ConfigReader("prova.JSON");
            System.out.println("Reading completed");

            multiplier = parameters.getMultiplier();
            productionTime = parameters.getProductionTime();

            for(ConfigSession sessionCfg : parameters.getClientSessions()) {

                MyAppClientSession session;
                do {
                    try {
                        session = new MyAppClientSession(sessionCfg.getId(), sessionCfg.getIp(), sessionCfg.getPort(), sessionCfg.getPercentage());
                        outgoingLinks.add(session);
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Thread.sleep(10000);
                } while (true);

                session.start();

                addSession(
                    session
                );
            }

            if(parameters.getServer() != null) {
                ConfigSession session = parameters.getServer();
                myAppServer = new MyAppServer(this, session.getIp(), session.getPort());
                myAppServer.bind();
            }
        } catch (Exception e) {
            System.out.println("Error reading config file");
            throw new RuntimeException(e);
        }
    }

    public void restore() {
        try {
            UUID uuid = UUID.randomUUID();
            SnapshotRestore snapshot = new SnapshotRestore(uuid, Optional.empty(), sessions);
            snapshotsRestore.put(uuid, snapshot);
            sessions.forEach(s -> s.sendPacket(new SnapshotRestorePacket(uuid)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onShapshotCompleted(DistributedNode<Integer> node, Snapshot<Integer> snapshot) {
        snapshot.writeToFile(new File("latest.snapshot"));
    }

    @Override
    public void onPacketReceived(Session<Integer> session, Packet packet) {
        super.onPacketReceived(session, packet);
        System.out.printf("Packet " + packet.getClass().getSimpleName());

        if(packet instanceof SnapshotRestorePacket) {
            //TODO
            UUID uuid = ((SnapshotRestorePacket) packet).getSnasphotRestoreId();
            boolean firstTime;
            SnapshotRestore snapshot;

            //This block must be seen as a single atomic operation
            synchronized (this) {
                firstTime = !snapshotsRestore.containsKey(uuid);
                if(firstTime) {
                    snapshot = new SnapshotRestore(uuid, Optional.of(session), sessions.stream().filter(s -> s.getID().equals(session.getID())).toList());

                    if(snapshot.isSnapshotRestoreComplete()) {
                        snapshot.getRestoreInitiator().ifPresent(opt -> opt.sendPacket(new SnapshotRestoreAckPacket(uuid)));
                        try {
                            state = (StateApp) restoreSnapshot(new File("latest.snapshot"));
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    } else {
                        snapshotsRestore.put(uuid, snapshot);
                    }
                } else {
                    snapshot = snapshotsRestore.get(uuid);
                }
            }

            if(firstTime) {
                sessions.forEach(otherSession -> {
                    if(otherSession != session) {
                        otherSession.sendPacket(packet);
                    }
                });
            } else {
                session.sendPacket(new SnapshotRestoreAckPacket(uuid));
            }
        } else if(packet instanceof SnapshotRestoreAckPacket) {
            UUID snapshotID = ((SnapshotRestoreAckPacket) packet).getSnasphotRestoreId();
            if(snapshotsRestore.containsKey(snapshotID)) {
                SnapshotRestore snapshot = snapshotsRestore.get(snapshotID);
                //synchronization block is needed to guarantee that snapshot complete is called only once
                synchronized (this) {
                    snapshot.markSessionAsDone(session.getID());
                    if (snapshot.isSnapshotRestoreComplete()) {
                        try {
                            state = (StateApp) restoreSnapshot(new File("latest.snapshot"));
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                        snapshotsRestore.remove(snapshotID);

                        snapshot.getRestoreInitiator().ifPresent( opt -> opt.sendPacket(new SnapshotRestoreAckPacket(snapshotID)));
                    }
                }
            }
        }

        if (packet instanceof ArrivingGoods) {
            System.out.printf("Packet " + ((ArrivingGoods) packet).getAmount());
            new Thread(() -> sendGoods((ArrivingGoods) packet)).start();

        }
    }

    public void sendGoods(ArrivingGoods goods) {
        state.refreshWorkingOn(goods.getAmount() * multiplier);

        try {
            Thread.sleep(1000*(productionTime));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Sending to " + outgoingLinks.size() + " nodes");
        for (MyAppClientSession a : outgoingLinks) {
            float temp = goods.getAmount();
            float newAmount = temp * multiplier * a.getPercentage();
            System.out.println("new amount " + newAmount);
            a.sendPacket(new ArrivingGoods(newAmount));
        }
    }

    public long getProductionTime() {
        return productionTime;
    }

    public float getMultiplier() {
        return multiplier;
    }
}
