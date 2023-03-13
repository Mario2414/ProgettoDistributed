package progetto;

import progetto.packet.Packet;
import progetto.session.SessionListener;

import java.util.List;

public interface Session {
    List<SessionListener> getListeners();
    void addListener(SessionListener session);
    boolean isConnected();
    void disconnect();

    void sendPacket(Packet packet);

    void start();
}
