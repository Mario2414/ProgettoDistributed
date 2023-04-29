package App;


import App.packets.ArrivingGoods;
import App.packets.SomeoneDown;
import progetto.DistributedNode;
import progetto.Server;
import progetto.Session;
import progetto.packet.Packet;
import progetto.session.ServerListener;
import progetto.session.SessionListener;
import progetto.tcp.TcpClientSession;
import progetto.tcp.TcpServer;
import progetto.tcp.TcpServerSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;


public class Main {
    private static Boolean ableToSend=false;
    private static List<Packet> recoveryPackets = new ArrayList();
    private static int numberOfNodes;
    private static String[] ips ;
    private static int[] ports ;

    private static List<Session> outgoingLinks = new ArrayList<>();

    private static StateApp state = new StateApp();

    private static DistributedNode node;

    private static ConfigReader parameters;






    public static void main(String[] args) {
        node = new DistributedNode(state);

        try {
            System.out.println("Reading config parameters");
            parameters = new ConfigReader("C:/Users/Mario/Documents/prova.JSON");
            System.out.println("Reading completed");
        } catch (Exception e) {
            System.out.println("Error reading config file");
            throw new RuntimeException(e);
        }

        System.out.println("Starting server...");
        Server server = new TcpServer("localhost", 8081);
        server.bind();


        server.addServerListener(new ServerListener() {
            @Override
            public void onSessionAccepted(Server server, Session session) {
                node.addSession(session);


                session.addListener(new SessionListener() {
                    @Override
                    public void onPacketReceived(Session session, Packet packet) {
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
                                node.snapshot();
                                sendToOut(packet);
                            }
                        }

                    }

                    @Override
                    public void onPacketSent(Session session, Packet packet) {
                        //throw new MonoDirectionalException("The channel should be monodirectional");
                        System.out.println("The channel should be monodirectional");
                    }

                    @Override
                    public void onConnected(Session session) {
                        System.out.println("new node: " + ((TcpServerSession) session).getHostAddress() + "connected to the server");
                    }

                    @Override
                    public void onDisconnection(Session session, Throwable exception) {
                        SomeoneDown recoveryMessage = new SomeoneDown(((TcpServerSession) session).getHostAddress());
                        recoveryPackets.add(recoveryMessage);
                        sendToOut(recoveryMessage);
                        node.snapshot();


                    }
                });

            }

            @Override
            public void onServerClosed(Server server, Throwable t) {

            }
        });

        System.out.println("Server started");

        numberOfNodes = parameters.getNumOfNodes();
        ips = parameters.getNodeIPs();
        ports = parameters.getNodePorts();


        Scanner stdin = new Scanner(System.in);

        System.out.println("To connect to predefined clients type 1");
        Boolean retry = true;
        int numInput = 1;
        while (retry){
            try{
                numInput = Integer.parseInt(stdin.nextLine());
                retry = false;
            }catch (Exception e){
                System.out.println("Please insert a number");
            }
        }

        if(numInput == 1){
            for (int i = 0; i < numberOfNodes; i++) {
                tryToConnect(i);
            }
        }

        while(true){
            System.out.println("To add manual goods press 1");
            System.out.println("To clean recovery history press 2");
            System.out.println("To start a snapshot press 3");

            while (retry){
                try{
                    numInput = Integer.parseInt(stdin.nextLine());
                    retry = false;
                }catch (Exception e){
                    System.out.println("Please insert a number");
                }
            }

            if(numInput == 1){
                float numGoods = 0;
                while (retry){
                    try{
                        System.out.println("Please insert the number of goods to be processed");
                        numGoods = Float.parseFloat(stdin.nextLine());
                        retry = false;
                    }catch (Exception e){
                        System.out.println("Please insert a number");
                    }
                }
                state.refreshWorkingOn(numGoods * parameters.getMultiplier());
                try {
                    Thread.sleep(1000*(parameters.getProductionTime()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                sendToOut(new ArrivingGoods(numGoods));
            }
            else if (numInput == 2) {
                recoveryPackets = new ArrayList<>();
            }
            else if (numInput == 3){
                node.snapshot();
            }

        }
    }

    private static void tryToConnect(int id){
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Boolean succed = true;
        while(succed){
            try{
                TcpClientSession session = new TcpClientSession(new SessionIDTest2(id), ips[id], ports[id]);
                node.addSession(session);

                session.addListener(new SessionListener() {
                    @Override
                    public void onPacketReceived(Session session, Packet packet) {
                        System.out.println("The channel should be monodirectional");
                    }

                    @Override
                    public void onPacketSent(Session session, Packet packet) {
                        state.refreshAfterSent(((ArrivingGoods) packet).getAmount());
                    }

                    @Override
                    public void onConnected(Session session) {
                        System.out.println("session " + session.getID() + " connessa");
                        outgoingLinks.add(session);
                        if(outgoingLinks.size()==numberOfNodes){
                            ableToSend = true;
                        }
                    }

                    @Override
                    public void onDisconnection(Session session, Throwable exception) {
                        //recovery part
                        System.out.println("session " + session.getID() + " disconnessa");
                        outgoingLinks.remove(session);
                        ableToSend = false;

                        //SomeoneDown recoveryMessage = new SomeoneDown(ips[((SessionIDTest2) session).getID()]);
                        //sendToOut(recoveryMessage);
                        //nodesDown.add(recoveryMessage);
                        tryToConnect(((SessionIDTest2) session).getID());

                    }
                });
                session.start();

                succed=false;


            } catch (Exception e) {
                System.out.println("Waiting for " + ips[id] + " to be up");
            }
        }

    }


    public static void sendToOut(Packet packet){
        while(!ableToSend){ // se non sono ableToSend continua a riprovare,
            if(outgoingLinks.size()==numberOfNodes){ //altrimenti controlla che tutti i nodi siano connessi
                ableToSend = true;
                for (Session a : outgoingLinks) {
                        proceedPacket(packet, a);
                }
                break;
            }
            else{
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void sendToAll(Packet packet){
        Queue<Session> nodeSessions = node.getSessions();
        for(Session session : nodeSessions){
            session.sendPacket(packet);
        }
    }

    public static void proceedPacket(Packet packet, Session session){
        if(packet instanceof ArrivingGoods){
            float temp = (((ArrivingGoods) packet).getAmount());
            float newAmount = temp * parameters.getMultiplier() * (parameters.getNodePercentages()[((SessionIDTest2) session).getID()]);
            session.sendPacket(new ArrivingGoods(newAmount));
        }
        else{
            session.sendPacket(packet);
        }
    }

    }






