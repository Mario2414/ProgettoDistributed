package App.packets;

import progetto.packet.Packet;

import java.util.UUID;

public class SnapshotRestorePacket implements Packet {
    private final UUID snasphotRestoreId;

    public SnapshotRestorePacket(UUID snasphotRestoreId) {
        this.snasphotRestoreId = snasphotRestoreId;
    }

    public UUID getSnasphotRestoreId() {
        return snasphotRestoreId;
    }
}
