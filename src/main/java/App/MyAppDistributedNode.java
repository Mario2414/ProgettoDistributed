package App;

import App.packets.ArrivingGoods;
import App.packets.GoodsThreadRestart;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MyAppDistributedNode extends DistributedNode<Integer> implements DistributedNodeListener<Integer> {
    private GoodsThread goodsThread;

    private final Map<UUID, SnapshotRestore> snapshotsRestore;
    private final List<MyAppClientSession> outgoingLinks;
    private final int numOfNodes;
    private final StateApp state;
    private final long productionTime;
    private final int batch;
    private final float multiplier;

    private final AtomicInteger wipRestores = new AtomicInteger(0);
    //private final AtomicBoolean enqueueRestart = new AtomicBoolean(false);

    public MyAppDistributedNode(StateApp state) {
        super(state);
        this.state = state;
        outgoingLinks = new CopyOnWriteArrayList<MyAppClientSession>();

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

            goodsThread.start();
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

                wipRestores.incrementAndGet();
                //enqueueRestart.set(false);

                SnapshotRestore snapshot = new SnapshotRestore(uuid, Optional.empty(), sessions, true);
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

        //This block must be seen as a single atomic operation
        //synchronization block is needed to guarantee that snapshot complete is called only once for a given snapshot.
        synchronized (this) {
            if (packet instanceof SnapshotRestorePacket) {
                UUID uuid = ((SnapshotRestorePacket) packet).getSnasphotRestoreId();
                if (!snapshotsRestore.containsKey(uuid)) {
                    try {
                        goodsThread.stopThread();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    wipRestores.incrementAndGet();
                    //enqueueRestart.set(false);

                    List<Session<Integer>> otherSessions = sessions.stream().filter(s -> !s.getID().equals(session.getID())).toList();
                    SnapshotRestore snapshot = new SnapshotRestore(uuid, Optional.of(session), otherSessions, false);

                    //needed also to keep track of completed snapshots.
                    snapshotsRestore.put(uuid, snapshot);

                    if (snapshot.isSnapshotRestoreComplete()) {
                        restoreSnapshot(snapshot);
                    } else {
                        otherSessions.forEach(otherSession -> otherSession.sendPacket(packet));
                    }
                } else {
                    session.sendPacket(new SnapshotRestoreAckPacket(uuid));
                }
            } else if (packet instanceof SnapshotRestoreAckPacket) {
                UUID snapshotID = ((SnapshotRestoreAckPacket) packet).getSnasphotRestoreId();
                SnapshotRestore snapshot = snapshotsRestore.getOrDefault(snapshotID, null);
                if (snapshot != null && !snapshot.isSnapshotRestoreComplete()) {
                    snapshot.markSessionAsDone(session.getID());
                    if (snapshot.isSnapshotRestoreComplete()) {
                        restoreSnapshot(snapshot);
                    }
                }
            } else if(packet instanceof GoodsThreadRestart) {
                if(!goodsThread.isRunning() && wipRestores.get() == 0) { //avoid recursion in case of circular network with this check.
                    goodsThread = new GoodsThread(this, batch, numOfNodes, productionTime);
                    goodsThread.start();

                    sessions.forEach(s -> {
                        if (s != session) s.sendPacket(new GoodsThreadRestart());
                    });
                }
            }
        }

        if (packet instanceof ArrivingGoods) {
            System.out.println("Packet " + ((ArrivingGoods) packet).getAmount());
            state.refreshWorkingOn(((ArrivingGoods) packet).getAmount() * multiplier);
        }
    }

    private void restoreSnapshot(SnapshotRestore snapshot) {
        try {
            System.out.println("Restoring snapshot...");
            restoreSnapshot(new File("latest.snapshot"));
            int val = wipRestores.decrementAndGet();
            if(val == 0) {
                goodsThread = new GoodsThread(this, batch, numOfNodes, productionTime);
                goodsThread.start();
                sessions.forEach(s -> s.sendPacket(new GoodsThreadRestart())); //initiate the goods thread restart procedure.
            }
            /*
            if(snapshot.isRoot()) { //root node is guaranteed to be the last node to restore the snapshot.
                if(val == 0) {
                    goodsThread = new GoodsThread(this, batch, numOfNodes, productionTime);
                    goodsThread.start();
                    sessions.forEach(s -> s.sendPacket(new GoodsThreadRestart())); //initiate the goods thread restart procedure.

                    enqueueRestart.set(false);
                } else {
                    enqueueRestart.set(true);
                }
            } else if(val == 0) {
                if(enqueueRestart.getAndSet(false)) {
                    goodsThread = new GoodsThread(this, batch, numOfNodes, productionTime);
                    goodsThread.start();
                    sessions.forEach(s -> s.sendPacket(new GoodsThreadRestart())); //initiate the goods thread restart procedure.
                }
            }

             */
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }


        snapshot.getRestoreInitiator().ifPresent(opt -> opt.sendPacket(new SnapshotRestoreAckPacket(snapshot.getUuid())));
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
