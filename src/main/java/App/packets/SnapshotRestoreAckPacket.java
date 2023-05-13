package App.packets;

import progetto.packet.Packet;

import java.util.UUID;

public class SnapshotRestoreAckPacket implements Packet {
    private final UUID snasphotRestoreId;

    public SnapshotRestoreAckPacket(UUID snasphotRestoreId) {
        this.snasphotRestoreId = snasphotRestoreId;
    }

    public UUID getSnasphotRestoreId() {
        return snasphotRestoreId;
    }
}
