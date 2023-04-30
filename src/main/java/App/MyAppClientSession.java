package App;

import progetto.Session;
import progetto.packet.Packet;
import progetto.session.SessionID;
import progetto.session.SessionListener;
import progetto.tcp.TcpClientSession;

public class MyAppClientSession extends TcpClientSession implements SessionListener {
    public MyAppClientSession(SessionID sessionID, String host, int port) {
        super(sessionID, host, port);
    }

    @Override
    public void onPacketReceived(Session session, Packet packet) {

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
}
