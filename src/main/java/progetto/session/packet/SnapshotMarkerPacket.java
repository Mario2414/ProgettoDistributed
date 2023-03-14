package progetto.session.packet;

import progetto.packet.Packet;

import java.util.UUID;

public class SnapshotMarkerPacket implements Packet {
    private final UUID uuid;

    public SnapshotMarkerPacket(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }
}
