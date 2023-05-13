package App;

import App.packets.ArrivingGoods;
import App.packets.SnapshotRestorePacket;
import App.packets.SomeoneDown;
import progetto.DistributedNode;
import progetto.Server;
import progetto.Session;
import progetto.packet.Packet;
import progetto.session.ServerListener;
import progetto.session.SessionListener;
import progetto.tcp.TcpServer;
import progetto.tcp.TcpServerSession;

import java.net.Socket;

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