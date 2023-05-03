package progetto.session;

import progetto.Session;
import progetto.packet.Packet;

import java.io.Serializable;

public interface SessionListener<ID extends Comparable<ID> & Serializable> {
    void onPacketReceived(Session<ID> session, Packet packet);
    void onPacketSent(Session<ID> session, Packet packet);
    void onConnected(Session<ID> session);
    void onDisconnection(Session<ID> session, Throwable exception);
}
