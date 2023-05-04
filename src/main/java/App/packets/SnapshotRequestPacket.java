package App.packets;

import progetto.packet.Packet;

import java.util.List;
import java.util.UUID;

public class SnapshotRequestPacket implements Packet {
    private final List<UUID> availableSnapshots;

    public SnapshotRequestPacket(List<UUID> availableSnapshots) {
        this.availableSnapshots = availableSnapshots;
    }

    public List<UUID> getAvailableSnapshots() {
        return availableSnapshots;
    }
}
