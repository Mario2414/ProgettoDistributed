package App;


import App.packets.ArrivingGoods;
import App.packets.SomeoneDown;
import progetto.*;
import progetto.packet.Packet;
import progetto.session.ServerListener;
import progetto.session.SessionListener;
import progetto.tcp.TcpClientSession;
import progetto.tcp.TcpServer;
import progetto.tcp.TcpServerSession;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;


public class App {
    private static boolean ableToSend = true;
    private static ConcurrentLinkedQueue<Packet> recoveryPackets = new ConcurrentLinkedQueue<>();
    private static int numberOfNodes;
    private static String[] ips ;
    private static int[] ports ;

    private static ConcurrentLinkedQueue<Session<Integer>> outgoingLinks = new ConcurrentLinkedQueue<>(); //Accesso in maniera concorrente nei listener

    private static ConcurrentLinkedQueue<Integer> notConnectedNodes = new ConcurrentLinkedQueue<>(); //acceddo in maniera concorrente nei listener

    private static StateApp state = new StateApp();

    private static MyAppDistributedNode node;

    private static ConfigReader parameters;

    public static void main(String[] args) {
        node = new MyAppDistributedNode(state);

        try {
            System.out.println("Reading config parameters");
            parameters = new ConfigReader("C:/Users/Mario/Documents/prova.JSON");
            System.out.println("Reading completed");
        } catch (Exception e) {
            System.out.println("Error reading config file");
            throw new RuntimeException(e);
        }

        numberOfNodes = parameters.getNumOfNodes();
        ips = parameters.getNodeIPs();
        ports = parameters.getNodePorts();

        for(int i=0; i < numberOfNodes; i++){
            notConnectedNodes.add(i);
        }

        System.out.println("Starting server...");
        Server<Integer> server = new TcpServer<Integer>("localhost", 8081);
        server.bind();

        server.addServerListener(new ServerListener<Integer>() {
            @Override
            public void onSessionAccepted(Server<Integer> server, Session<Integer> session) {
                node.addSession(session);
                session.addListener(new SessionListener<Integer>() {
                    @Override
                    public void onPacketReceived(Session<Integer> session, Packet packet) {
                        if (packet instanceof ArrivingGoods) {
                            state.refreshWorkingOn(((ArrivingGoods) packet).getAmount() * parameters.getMultiplier());
                            try {
                                Thread.sleep(1000*(parameters.getProductionTime()));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            sendToOut(packet);
                        } else if (packet instanceof SomeoneDown){
                            if(!recoveryPackets.contains(packet)){
                                recoveryPackets.add(packet);
                                sendToAll(packet);
                                System.exit(1);
                            }
                        }
                    }

                    @Override
                    public void onPacketSent(Session<Integer> session, Packet packet) {
                    }

                    @Override
                    public void onConnected(Session<Integer> session) {
                        System.out.println("new node: " + ((TcpServerSession<Integer>) session).getHostAddress() + "connected to the server");
                    }

                    @Override
                    public void onDisconnection(Session<Integer> session, Throwable exception) {
                        SomeoneDown recoveryMessage = new SomeoneDown(((TcpServerSession<Integer>) session).getHostAddress());
                        recoveryPackets.add(recoveryMessage);
                        sendToAll(recoveryMessage);

                        System.exit(1);
                    }
                });

            }

            @Override
            public void onSessionClosed(Server<Integer> server, Session<Integer> session) {

            }

            @Override
            public void onServerClosed(Server<Integer> server, Throwable t) {

            }
        });

        System.out.println("Server started");


        Scanner stdin = new Scanner(System.in);
        boolean retry;
        int numInput = 1;

        tryToConnect2();

        /*
        System.out.println("To connect to predefined clients type 1");

        do {
            try{
                numInput = Integer.parseInt(stdin.nextLine());
                retry = false;
            }catch (Exception e){
                System.out.println("Please insert a number");
                retry = true;
            }
        } while (retry);

        if(numInput == 1){
            tryToConnect2();
        }

         */

        while(true) {
            System.out.println("To add manual goods press 1");
            System.out.println("To clean recovery history press 2");
            System.out.println("To start a snapshot press 3");

            do {
                try{
                    numInput = Integer.parseInt(stdin.nextLine());
                    retry = false;
                }catch (Exception e){
                    System.out.println("Please insert a number");
                    retry = true;
                }
            } while (retry);

            if(numInput == 1){
                float numGoods = 0;
                do {
                    try{
                        System.out.println("Please insert the number of goods to be processed");
                        numGoods = Float.parseFloat(stdin.nextLine());
                        retry = false;
                    } catch (Exception e){
                        System.out.println("Please insert a number");
                        retry = true;
                    }
                } while (retry);
                state.refreshWorkingOn(numGoods * parameters.getMultiplier());
                try {
                    Thread.sleep(1000*(parameters.getProductionTime()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sendToOut(new ArrivingGoods(numGoods));
            } else if (numInput == 2) {
                recoveryPackets = new ConcurrentLinkedQueue<>();
            } else if (numInput == 3){
                node.snapshot();
            } else if(numInput == 4) { //todo check if it makes sense
                try {
                    state = (StateApp) node.restoreSnapshot(new File("latest.snapshot"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }

    public synchronized static void tryToConnect2(){
        while(outgoingLinks.size() != numberOfNodes){
            for(Integer a : notConnectedNodes){
                connectTo((int) a);
            }

            try {
                Thread.sleep(60000); //timer to wait before retrying to connect, set to 60 seconds
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /*

    private static void tryToConnect(int id){
        boolean notSucced = true;
        while(notSucced){
            try{
                TcpClientSession session = new TcpClientSession(new SessionIDTest2(id), ips[id], ports[id]);
                AtomicBoolean failed = new AtomicBoolean(false);
                Semaphore semaphore = new Semaphore(0);
                SessionListener tempListener = new SessionListener() {
                    @Override
                    public void onPacketReceived(Session session, Packet packet) {

                    }

                    @Override
                    public void onPacketSent(Session session, Packet packet) {

                    }

                    @Override
                    public void onConnected(Session session) {
                        semaphore.release();
                        node.addSession(session);
                        session.addListener(new SessionListener() {
                            @Override
                            public void onPacketReceived(Session session, Packet packet) {
                                if(packet instanceof SomeoneDown){
                                    if(!recoveryPackets.contains(packet)){
                                        recoveryPackets.add(packet);
                                        node.snapshot(); // al posto di snapshot ci sarà recovery
                                        sendToAll(packet);
                                        System.exit(1);
                                    }
                                }
                            }

                            @Override
                            public void onPacketSent(Session session, Packet packet) {
                                if(packet instanceof ArrivingGoods) {
                                    state.refreshAfterSent(((ArrivingGoods) packet).getAmount());
                                }
                            }

                            @Override
                            public void onConnected(Session session) {
                                System.out.println("session " + session.getID() + " connessa");
                                outgoingLinks.add(session);
                                if(outgoingLinks.size() == numberOfNodes){
                                    ableToSend = true;
                                }
                            }

                            @Override
                            public void onDisconnection(Session session, Throwable exception) {
                                //recovery part
                                System.out.println("session " + session.getID() + " disconnessa");
                                outgoingLinks.remove(session);
                                ableToSend = false;

                                SomeoneDown recoveryMessage = new SomeoneDown(ips[((SessionIDTest2) session).getID()]);
                                sendToOut(recoveryMessage);
                                System.exit(1);
                                //nodesDown.add(recoveryMessage);
                                //connectTo(((SessionIDTest2) session).getID());
                            }
                        });
                    }

                    @Override
                    public void onDisconnection(Session session, Throwable exception) {
                        semaphore.notify();
                        failed.set(true);
                    }
                };
                session.addListener(tempListener);
                semaphore.acquire();
                session.start();
                if(!failed.get()) {

                }
            } catch (Exception e) {
                System.out.println("Waiting for " + ips[id] + " to be up");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
     */


    private static void connectTo(int id){
        TcpClientSession<Integer> session = new TcpClientSession<Integer>(id, ips[id], ports[id]);

        session.addListener(new SessionListener<Integer>() {
            @Override
            public void onPacketReceived(Session<Integer> session, Packet packet) {
                if(packet instanceof SomeoneDown){
                    if(!recoveryPackets.contains(packet)){
                        recoveryPackets.add(packet);
                        sendToAll(packet);
                        System.exit(1);
                    }
                }
            }

            @Override
            public void onPacketSent(Session<Integer> session, Packet packet) {
                if(packet instanceof ArrivingGoods) {
                    state.refreshAfterSent(((ArrivingGoods) packet).getAmount());
                }
            }

            @Override
            public void onConnected(Session<Integer> session) {
                System.out.println("session " + session.getID() + " connessa");
                node.addSession(session);
                outgoingLinks.add(session);
                notConnectedNodes.remove(id);
                if(outgoingLinks.size() == numberOfNodes){
                    ableToSend = true;
                }
            }

            @Override
            public void onDisconnection(Session<Integer> session, Throwable exception) {
                //recovery part
                System.out.println("session " + session.getID() + " disconnessa");
                outgoingLinks.remove(session);
                notConnectedNodes.add(id);
                ableToSend = false;

                SomeoneDown recoveryMessage = new SomeoneDown(ips[session.getID()]);
                sendToOut(recoveryMessage);
                System.exit(1);
            }
        });
        session.start();
    }


    public static void sendToOut(Packet packet){
        while(!ableToSend) { // se non sono ableToSend continua a riprovare,
            if(outgoingLinks.size() == numberOfNodes){ //altrimenti controlla che tutti i nodi siano connessi
                ableToSend = true;
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        for (Session<Integer> a : outgoingLinks) {
            proceedPacket(packet, a);
        }
    }

    public static void sendToAll(Packet packet) {
        Queue<Session<Integer>> nodeSessions = node.getSessions();
        for(Session<Integer> session : nodeSessions){
            session.sendPacket(packet);
        }
    }

    public static void proceedPacket(Packet packet, Session<Integer> session){
        if(packet instanceof ArrivingGoods){
            float temp = (((ArrivingGoods) packet).getAmount());
            float newAmount = temp * parameters.getMultiplier() * (parameters.getNodePercentages()[session.getID()]);
            session.sendPacket(new ArrivingGoods(newAmount));
        }
        else{
            session.sendPacket(packet);
        }
    }
}




