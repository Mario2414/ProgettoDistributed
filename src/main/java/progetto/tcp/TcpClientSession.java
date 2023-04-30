package progetto.tcp;

import progetto.session.SessionID;
import progetto.session.packet.ClientGreetPacket;

import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpClientSession extends TcpSession {
    private final String host;
    private final int port;

    public TcpClientSession(SessionID sessionID, String host, int port) {
        super(new Socket());
        this.sessionID = sessionID;
        this.host = host;
        this.port = port;
    }

    @Override
    protected void runImpl() {
        try {
            socket.connect(new InetSocketAddress(host, port), 1000);
        } catch (Exception e) {
            e.printStackTrace();
            listeners.forEachListeners(sessionListener -> sessionListener.onDisconnection(this, e));
            return;
        }
        sendPacket(new ClientGreetPacket(sessionID));
        super.runImpl();
    }
}
