package progetto;

import progetto.packet.Packet;
import progetto.session.SessionListener;

import java.io.Serializable;
import java.util.List;

public interface Session<ID extends Comparable<ID> & Serializable> {
    List<SessionListener<ID>> getListeners();
    void addListener(SessionListener<ID> session);
    boolean isConnected();
    void disconnect();

    void sendPacket(Packet packet);

    void start();
    ID getID();
}
