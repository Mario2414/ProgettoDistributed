package progetto.tcp;

import progetto.Session;
import progetto.packet.Packet;
import progetto.session.SessionListener;
import progetto.tcp.packet.ClientGreetPacket;

import java.io.Serializable;
import java.net.Socket;

/**
 * TcpSession accepted by a TcpServer.
 * Differs by TcpClientSession, which instead is created to connect to a TcpServer.
 * @param <ID> The session id.
 */
public class TcpServerSession<ID extends Comparable<ID> & Serializable> extends TcpSession<ID> implements SessionListener<ID> {
    public TcpServerSession(Socket socket) {
        super(socket);
        this.addListener(this);
    }

    @Override
    public void onPacketReceived(Session<ID> session, Packet packet) {
        if(packet instanceof ClientGreetPacket) {
            this.sessionID = ((ClientGreetPacket<ID>) packet).getSessionID();
        }
    }

    @Override
    public void onPacketSent(Session<ID> session, Packet packet) {

    }

    @Override
    public void onConnected(Session<ID> session) {

    }

    @Override
    public void onDisconnection(Session<ID> session, Throwable exception) {

    }
}
