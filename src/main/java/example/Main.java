package example;

/**
class SessionIDTest implements SessionID {
    private UUID uuid;

    public SessionIDTest(UUID uuid) {
        this.uuid = uuid;
    }


    public UUID getID() {
        return uuid;
    }

    @Override
    public int compareTo(SessionID o) {
        if(o instanceof SessionIDTest) {
            return uuid.compareTo(((SessionIDTest) o).uuid);
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof SessionID) {
            return this.compareTo((SessionID) obj) == 0;
        }
        return false;
    }
}
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
            System.out.println("sono qui 11");
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
            //node1.snapshot();
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

            TcpClientSession session = new TcpClientSession(new SessionIDTest(UUID.randomUUID()), "localhost", 8081);
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

 **/
