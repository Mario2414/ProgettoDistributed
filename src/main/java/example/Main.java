package example;

import progetto.DistributedNode;
import progetto.Server;
import progetto.Session;
import progetto.packet.Packet;
import progetto.session.ServerListener;
import progetto.session.SessionListener;
import progetto.state.State;
import progetto.tcp.TcpClientSession;
import progetto.tcp.TcpServer;
import progetto.tcp.TcpSession;

class StateTest extends State {
    private int test;

    public StateTest(int test) {
        this.test = test;
    }

    public void increment() {
        test++;
    }

    public int getTest() {
        return test;
    }
}
class IncrementPacket implements Packet {

}

public class Main {
    private static void node1() {
        try {
            StateTest test = new StateTest(1);
            DistributedNode node1 = new DistributedNode(test);

            Server server = new TcpServer("localhost", 8081);
            server.bind();

            server.addServerListener(new ServerListener() {
                @Override
                public void onSessionAccepted(Server server, Session session) {
                    node1.addSession(session);

                    session.addListener(new SessionListener() {
                        @Override
                        public void onPacketReceived(Session session, Packet packet) {
                            if(packet instanceof IncrementPacket) {
                                test.increment();
                            }
                        }

                        @Override
                        public void onPacketSent(Session session, Packet packet) {

                        }

                        @Override
                        public void onConnected(Session session) {

                        }

                        @Override
                        public void onDisconnection(Session session, Throwable exception) {

                        }
                    });

                    session.sendPacket(new IncrementPacket());
                }

                @Override
                public void onServerClosed(Server server, Throwable t) {

                }
            });

            Thread.sleep(5000);
            node1.snapshot();
            Thread.sleep(30000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void node2() {
        try {
            Thread.sleep(5000);
            StateTest test = new StateTest(1);
            DistributedNode node1 = new DistributedNode(test);

            TcpClientSession session = new TcpClientSession("localhost", 8081);
            node1.addSession(session);
            session.addListener(new SessionListener() {
                @Override
                public void onPacketReceived(Session session, Packet packet) {

                }

                @Override
                public void onPacketSent(Session session, Packet packet) {

                }

                @Override
                public void onConnected(Session session) {
                    session.sendPacket(new IncrementPacket());
                }

                @Override
                public void onDisconnection(Session session, Throwable exception) {

                }
            });
            session.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Thread(Main::node1).start();
        new Thread(Main::node2).start();
    }

}
