package App;


import App.packets.ArrivingGoods;

import progetto.*;
import progetto.packet.Packet;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;


public class App {
    private static ConcurrentLinkedQueue<Packet> recoveryPackets = new ConcurrentLinkedQueue<>();

    private static ConcurrentLinkedQueue<Session<Integer>> outgoingLinks = new ConcurrentLinkedQueue<>(); //Accesso in maniera concorrente nei listener

    private static ConcurrentLinkedQueue<Integer> notConnectedNodes = new ConcurrentLinkedQueue<>(); //acceddo in maniera concorrente nei listener

    private static MyAppDistributedNode node;

    public static void main(String[] args) {
        node = new MyAppDistributedNode(new StateApp());

        System.out.println("Node started");

        Scanner stdin = new Scanner(System.in);
        boolean retry;
        int numInput = 1;

        while(true) {
            System.out.println("To add manual goods press 1");
            System.out.println("To start a snapshot press 2");
            System.out.println("To restore from a snapshot press 3");
            System.out.println("To print the state press 4");
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

                node.sendGoods(new ArrivingGoods(numGoods));
            } else if (numInput == 2){
                node.snapshot();
            } else if(numInput == 3) {
                node.restore();
            } else if(numInput == 4) {
                System.out.println(node.getState().toString());
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


    /*private static void connectTo(int id) throws IOException {
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
    }*/
}




