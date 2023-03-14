package progetto.session.packet;

import progetto.packet.Packet;
import progetto.session.SessionID;

public class ClientGreetPacket implements Packet {
    private final SessionID sessionID;

    public ClientGreetPacket(SessionID sessionID) {
        this.sessionID = sessionID;
    }

    public SessionID getSessionID() {
        return sessionID;
    }
}
