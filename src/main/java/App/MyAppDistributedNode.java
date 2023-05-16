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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class MyAppDistributedNode extends DistributedNode<Integer> implements DistributedNodeListener<Integer> {
    private final Map<UUID, SnapshotRestore> snapshotsRestore;
    private List<MyAppClientSession> outgoingLinks;

    private int numOfNodes;
    private StateApp state;

    private MyAppServer myAppServer;

    private long productionTime;

    private int batch;

    private float multiplier;

    public MyAppDistributedNode(StateApp state) {
        super(state);
        this.state = state;
        outgoingLinks = new CopyOnWriteArrayList<MyAppClientSession>();

        snapshotsRestore = new HashMap<UUID, SnapshotRestore>();
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
                myAppServer = new MyAppServer(this, session.getIp(), session.getPort());
                myAppServer.bind();
            }

            new Thread(() -> {
                while(true){
                    float workingOn = getState().getWorkingOn();
                    if(workingOn-batch>=0){
                        try {
                            Thread.sleep(1000*(productionTime));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Sending to " + outgoingLinks.size() + " nodes");
                        if(numOfNodes==0){ //is the last node of the production chain
                            getState().refreshAfterSent(batch);
                        }
                        else{
                            for (MyAppClientSession a : outgoingLinks) {
                                float newAmount = batch * a.getPercentage();
                                System.out.println("new amount " + newAmount);
                                a.sendPacket(new ArrivingGoods(newAmount));
                            }
                            try {
                                Thread.sleep(1000); //time taken by the machine to deliver the goods always the same
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else{
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();

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
                if(firstTime) {
                    SnapshotRestore snapshot = new SnapshotRestore(uuid, Optional.of(session), sessions.stream().filter(s -> !s.getID().equals(session.getID())).toList());

                    if(snapshot.isSnapshotRestoreComplete()) {
                        session.sendPacket(new SnapshotRestoreAckPacket(uuid)); //this is the initiator
                        try {
                            System.out.println("Restoring snapshot");
                            state = (StateApp) restoreSnapshot(new File("latest.snapshot"));
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    } else {
                        snapshotsRestore.put(uuid, snapshot);
                    }
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
                            System.out.println("Restoring snapshot");
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
            System.out.println("Packet " + ((ArrivingGoods) packet).getAmount());
            state.refreshWorkingOn(((ArrivingGoods) packet).getAmount() * multiplier);
        }
    }

    @Override
    public void onPacketSent(Session<Integer> session, Packet packet) {
        if(packet instanceof ArrivingGoods) {
            state.refreshAfterSent(((ArrivingGoods) packet).getAmount());
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
                                session
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
}
