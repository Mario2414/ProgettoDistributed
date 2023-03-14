package progetto.tcp;

import progetto.Session;
import progetto.packet.Packet;
import progetto.session.SessionListener;
import progetto.session.packet.ClientGreetPacket;

import java.net.Socket;

public class TcpServerSession extends TcpSession implements SessionListener {

    public TcpServerSession(Socket socket) {
        super(socket);
        this.addListener(this);
    }


    @Override
    public void onPacketReceived(Session session, Packet packet) {
        if(packet instanceof ClientGreetPacket) {
            this.sessionID = ((ClientGreetPacket) packet).getSessionID();
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
}
