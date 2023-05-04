package App.packets;

import progetto.packet.Packet;

import java.util.List;
import java.util.UUID;

public class SnapshotResponsePacket implements Packet {
    private final List<UUID> availableSnapshots;

    public SnapshotResponsePacket(List<UUID> availableSnapshots) {
        this.availableSnapshots = availableSnapshots;
    }

    public List<UUID> getAvailableSnapshots() {
        return availableSnapshots;
    }
}
