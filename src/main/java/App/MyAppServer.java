package App;


import progetto.Server;
import progetto.Session;
import progetto.session.ServerListener;
import progetto.tcp.TcpServer;

public class MyAppServer extends TcpServer<Integer> implements ServerListener<Integer> {
    private final MyAppDistributedNode node;

    public MyAppServer(MyAppDistributedNode node, String host, int port) {
        super(host, port);
        this.node = node;
        addServerListener(this);
    }

    @Override
    public void onSessionAccepted(Server<Integer> server, Session<Integer> session) {
        System.out.println("onSessionAccepted");
        node.addSession(session);
    }

    @Override
    public void onSessionClosed(Server<Integer> server, Session<Integer> session) {

    }

    @Override
    public void onServerClosed(Server<Integer> server, Throwable t) {

    }
}