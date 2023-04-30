package progetto.session.packet;

import progetto.packet.Packet;

public class KeepAlivePongPacket implements Packet {
    private final long time;

    public KeepAlivePongPacket(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }
}
