package progetto.session.packet;

import progetto.packet.Packet;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

public class SnapshotMarkerPacket implements Packet {
    private final UUID uuid;
    private final LocalDateTime date;

    public SnapshotMarkerPacket(UUID uuid, LocalDateTime date) {
        this.uuid = uuid;
        this.date = date;
    }

    public UUID getUuid() {
        return uuid;
    }

    public LocalDateTime getDate() {
        return date;
    }
}
