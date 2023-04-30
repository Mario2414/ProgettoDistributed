package progetto.session.packet;

import progetto.packet.Packet;

public class KeepAlivePingPacket implements Packet {
    private final long time;

    public KeepAlivePingPacket(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }
}
