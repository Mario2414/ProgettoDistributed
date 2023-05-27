package test1;

import common.PacketIncrementTest;
import common.StateTest;
import progetto.DistributedNode;
import progetto.Session;
import progetto.packet.Packet;
import progetto.packet.SnapshotMarkerPacket;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public class DistributedNodeTest<T extends Comparable<T> & Serializable> extends DistributedNode<T> {
    private final StateTest state;
    public DistributedNodeTest(StateTest state) {
        super(state);
        this.state = state;
    }

    @Override
    public void onPacketReceived(Session<T> session, Packet packet) {
        if(packet instanceof SnapshotMarkerPacket) {
            session.sendPacket(new PacketIncrementTest());
        }

        if(packet instanceof PacketIncrementTest) {
            state.state += 1;
        }

        super.onPacketReceived(session, packet);
    }
}
