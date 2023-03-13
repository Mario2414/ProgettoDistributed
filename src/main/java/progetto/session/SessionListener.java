package progetto.session;

import progetto.Session;
import progetto.packet.Packet;

public interface SessionListener {
    void onPacketReceived(Session session, Packet packet);
    void onPacketSent(Session session, Packet packet);
    void onConnected(Session session, Packet packet);
    void onDisconnection(Session session, Throwable exception);
}
