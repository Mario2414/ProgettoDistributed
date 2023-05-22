package test1;

import org.junit.jupiter.api.Test;
import progetto.utils.Const;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
* This test is used the check the capability of restoring a snapshot that
* had a packet in-between the snapshot marker and snapshot ack.
* In fact the algorithm must restore all the packets in between
* the marker and ack.
* To accomplish that DistributedNode is overridden, and after receiving a marker it
* forces the node to send a IncrementPacket before sending the ack.
* */
public class Test1 {
    @Test
    public void test() throws IOException, ClassNotFoundException {
        Const.DEBUG = true;

        DummySessionTest session1 = new DummySessionTest(1);
        DummySessionTest session2 = new DummySessionTest(2);
        StateTest state1 = new StateTest();
        StateTest state2 = new StateTest();

        DistributedNodeTest<Integer> node1 = new DistributedNodeTest<>(state1);
        node1.addSession(session1);
        node1.addListener((node, snapshot) -> {
            System.out.println("Snapshot1 done!");
            snapshot.writeToFile(new File("snapshot1.snap"));
        });

        DistributedNodeTest<Integer> node2 = new DistributedNodeTest<>(state2);
        node2.addSession(session2);
        node2.addListener((node, snapshot) -> {
            System.out.println("Snapshot2 done!");
            snapshot.writeToFile(new File("snapshot2.snap"));
        });

        session1.otherEnd = session2;
        session2.otherEnd = session1;
        session1.sendPacket(new PacketIncrementTest());
        session2.sendPacket(new PacketIncrementTest());
        session2.sendPacket(new PacketIncrementTest());

        assertEquals(2, state1.state);
        assertEquals(1, state2.state);

        //note: the snapshot will force node2 to send an increment packet to node1 after receiving the marker.
        // The change MUST be included in the snapshot, because it happens before the end of the snapshot.
        node1.snapshot();

        assertEquals(3, state1.state); //+1 because of increment packet in DistributedNodeTest after marker
        assertEquals(1, state2.state);

        session1.sendPacket(new PacketIncrementTest());
        session2.sendPacket(new PacketIncrementTest());
        session2.sendPacket(new PacketIncrementTest());

        assertEquals(5, state1.state);
        assertEquals(2, state2.state);


        //simulate disconnection
        session1 = new DummySessionTest(1);
        session1.otherEnd = session1;
        session2.otherEnd = session2;
        state1 = new StateTest();
        node1 = new DistributedNodeTest<>(state1);
        node1.addSession(session1);

        node1.restoreSnapshot(new File("snapshot1.snap"));
        node2.restoreSnapshot(new File("snapshot2.snap"));

        assertEquals(3, state1.state);
        assertEquals(1, state2.state);
    }
}
