package progetto.tcp.packet;

import progetto.packet.Packet;

public class ClientGreetPacket<ID> implements Packet {
    private final ID sessionID;

    public ClientGreetPacket(ID sessionID) {
        this.sessionID = sessionID;
    }

    public ID getSessionID() {
        return sessionID;
    }
}
