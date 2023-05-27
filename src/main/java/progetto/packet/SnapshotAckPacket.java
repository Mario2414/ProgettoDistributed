package progetto.packet;

import progetto.packet.Packet;

import java.util.UUID;

public class SnapshotAckPacket implements Packet {
    private final UUID uuid;

    public SnapshotAckPacket(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }
}
