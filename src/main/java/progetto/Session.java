package progetto;

import progetto.packet.Packet;
import progetto.session.SessionID;
import progetto.session.SessionListener;

import java.util.List;
import java.util.UUID;

public interface Session {
    List<SessionListener> getListeners();
    void addListener(SessionListener session);
    boolean isConnected();
    void disconnect();

    void sendPacket(Packet packet);

    void start();
    SessionID getID();
}
