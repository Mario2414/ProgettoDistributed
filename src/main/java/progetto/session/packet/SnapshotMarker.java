package progetto.session.packet;

import progetto.packet.Packet;

import java.util.UUID;

public class SnapshotMarker implements Packet {
    private final UUID uuid;

    public SnapshotMarker(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }
}
