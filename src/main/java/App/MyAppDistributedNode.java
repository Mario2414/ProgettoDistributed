package App;

import App.packets.ArrivingGoods;
import App.packets.SnapshotRestoreAckPacket;
import App.packets.SnapshotRestorePacket;
import progetto.DistributedNode;
import progetto.DistributedNodeListener;
import progetto.Session;
import progetto.Snapshot;
import progetto.packet.Packet;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MyAppDistributedNode extends DistributedNode<Integer> implements DistributedNodeListener<Integer> {
    private GoodsThread goodsThread;

    private final Map<UUID, SnapshotRestore> snapshotsRestoreCompleted;
    private final Map<UUID, SnapshotRestore> snapshotsRestore;
    private final List<MyAppClientSession> outgoingLinks;
    private final int numOfNodes;
    private final StateApp state;
    private final long productionTime;
    private final int batch;
    private final float multiplier;

    public MyAppDistributedNode(StateApp state) {
        super(state);
        this.state = state;
        outgoingLinks = new CopyOnWriteArrayList<MyAppClientSession>();

        snapshotsRestoreCompleted = new ConcurrentHashMap<UUID, SnapshotRestore>();
        snapshotsRestore = new ConcurrentHashMap<UUID, SnapshotRestore>();
        this.addListener(this);

        try {
            System.out.println("Reading config parameters");
            ConfigReader parameters = new ConfigReader("prova.JSON");
            System.out.println("Reading completed");


            multiplier = parameters.getMultiplier();
            productionTime = parameters.getProductionTime();
            batch = parameters.getBatch();
            numOfNodes = parameters.getNumOfNodes();

            if(parameters.getServer() != null) {
                ConfigSession session = parameters.getServer();
                System.out.println("l'ip del server è "+ session.getIp());
                System.out.println("la porta del server è " + session.getPort());
                MyAppServer myAppServer = new MyAppServer(this, session.getIp(), session.getPort());
                myAppServer.bind();
            }

            goodsThread = new GoodsThread(this, batch, numOfNodes, productionTime);
            goodsThread.start();

            for(ConfigSession sessionCfg : parameters.getClientSessions()) {

                MyAppClientSession session;
                do {
                    try {
                        session = new MyAppClientSession(sessionCfg.getId(), sessionCfg.getIp(), sessionCfg.getPort(), sessionCfg.getPercentage(), state);
                        session.connect(true);
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

        } catch (Exception e) {
            System.out.println("Error reading config file");
            throw new RuntimeException(e);
        }
    }

    public void restore() {
        try {
            File file = new File("latest.snapshot");
            if (file.exists()) {
                UUID uuid = UUID.randomUUID();

                try {
                    goodsThread.stopThread();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                SnapshotRestore snapshot = new SnapshotRestore(uuid, Optional.empty(), sessions);
                snapshotsRestore.put(uuid, snapshot);
                sessions.forEach(s -> s.sendPacket(new SnapshotRestorePacket(uuid)));
            } else {
                System.out.println("No snapshot to recover!!!");
            }

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
        System.out.println("Packet " + packet.getClass().getSimpleName());

        if(packet instanceof SnapshotRestorePacket) {
            UUID uuid = ((SnapshotRestorePacket) packet).getSnasphotRestoreId();
            boolean firstTime;

            //This block must be seen as a single atomic operation
            synchronized (this) {
                firstTime = !snapshotsRestore.containsKey(uuid);
                if(!snapshotsRestoreCompleted.containsKey(uuid)){
                    if (firstTime) {
                        try {
                            goodsThread.stopThread();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        List<Session<Integer>> otherSessions = sessions.stream().filter(s -> !s.getID().equals(session.getID())).toList();
                        SnapshotRestore snapshot = new SnapshotRestore(uuid, Optional.of(session), otherSessions);

                        if(otherSessions.isEmpty()) {
                            try {
                                System.out.println("Restoring snapshot 1");
                                snapshotsRestoreCompleted.put(uuid, snapshot);
                                restoreSnapshot(new File("latest.snapshot"));
                                goodsThread = new GoodsThread(this, batch, numOfNodes, productionTime);
                                goodsThread.start();

                                session.sendPacket(new SnapshotRestoreAckPacket(uuid)); //initiator
                            } catch (Exception e) {
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            }
                        } else {
                            snapshotsRestore.put(uuid, snapshot);

                            otherSessions.forEach(otherSession -> otherSession.sendPacket(packet));
                        }
                    } else {
                        session.sendPacket(new SnapshotRestoreAckPacket(uuid));
                    }

                }
            }
        } else if(packet instanceof SnapshotRestoreAckPacket) {
            UUID snapshotID = ((SnapshotRestoreAckPacket) packet).getSnasphotRestoreId();
            //synchronization block is needed to guarantee that snapshot complete is called only once
            synchronized (this){

                if(!snapshotsRestoreCompleted.containsKey(snapshotID)){
                    if(snapshotsRestore.containsKey(snapshotID)) {
                        SnapshotRestore snapshot = snapshotsRestore.get(snapshotID);
                        snapshot.markSessionAsDone(session.getID());
                        if (snapshot.isSnapshotRestoreComplete()) {
                            try {
                                System.out.println("Restoring snapshot 2");
                                snapshotsRestoreCompleted.put(snapshotID, snapshotsRestore.get(snapshotID));
                                goodsThread.stopThread();
                                restoreSnapshot(new File("latest.snapshot"));
                                goodsThread = new GoodsThread(this, batch, numOfNodes, productionTime);
                                goodsThread.start();
                            } catch (Exception e) {
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            }
                            snapshot.getRestoreInitiator().ifPresent( opt -> opt.sendPacket(new SnapshotRestoreAckPacket(snapshotID)));
                        }
                    }
                }
            }

        }

        if (packet instanceof ArrivingGoods) {
            System.out.println("Packet " + ((ArrivingGoods) packet).getAmount());
            state.refreshWorkingOn(((ArrivingGoods) packet).getAmount() * multiplier);
        }
    }

    @Override
    public void onPacketSent(Session<Integer> session, Packet packet) {
        if(packet instanceof ArrivingGoods) {

        }
    }

    @Override
    public void onDisconnection(Session<Integer> session, Throwable exception) {
        super.onDisconnection(session, exception);

        if(session instanceof MyAppClientSession) {
            MyAppClientSession appSession = (MyAppClientSession) session;
            outgoingLinks.remove(session);
            System.out.println("Disconnection. Trying to recover");
            new Thread(() -> {
                do {
                    try {
                        MyAppClientSession newSess = new MyAppClientSession(appSession.getID(), appSession.getHost(), appSession.getPort(), appSession.getPercentage(), state);
                        newSess.connect(true);
                        outgoingLinks.add(newSess);

                        newSess.start();

                        addSession(
                                newSess
                        );
                        System.out.println("Recovered");
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                } while (true);
            }).start();
        }
    }

    public void sendGoods(ArrivingGoods goods) {
        state.refreshWorkingOn(goods.getAmount() * multiplier);
    }

    public long getProductionTime() {
        return productionTime;
    }

    public float getMultiplier() {
        return multiplier;
    }

    public StateApp getState() {
        return state;
    }

    public List<MyAppClientSession> getOutgoingLinks(){
        return outgoingLinks;
    }
}
