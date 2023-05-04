package App.packets;

import progetto.packet.Packet;

import java.util.UUID;

public class SnapshotRestorePacket implements Packet {
    private final UUID uuid;

    public SnapshotRestorePacket(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getSnapshot() {
        return uuid;
    }
}
