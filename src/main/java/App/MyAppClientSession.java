package App;

import progetto.Session;
import progetto.packet.Packet;
import progetto.session.SessionListener;
import progetto.tcp.TcpClientSession;

public class MyAppClientSession extends TcpClientSession<Integer> implements SessionListener<Integer> {
    public MyAppClientSession(Integer sessionID, String host, int port) {
        super(sessionID, host, port);
    }

    @Override
    public void onPacketReceived(Session<Integer> session, Packet packet) {

    }

    @Override
    public void onPacketSent(Session<Integer> session, Packet packet) {

    }

    @Override
    public void onConnected(Session<Integer> session) {

    }

    @Override
    public void onDisconnection(Session<Integer> session, Throwable exception) {

    }
}
