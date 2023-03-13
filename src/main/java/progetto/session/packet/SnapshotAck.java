package progetto.session.packet;

import progetto.packet.Packet;

import java.util.UUID;

public class SnapshotAck implements Packet {
    private final UUID uuid;

    public SnapshotAck(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }
}
