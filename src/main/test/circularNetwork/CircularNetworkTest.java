package circularNetwork;

import common.DummySessionTest;
import common.PacketIncrementTest;
import common.StateTest;
import org.junit.jupiter.api.Test;
import progetto.utils.Const;
import test1.DistributedNodeTest;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CircularNetworkTest {
    @Test
    public void test() {
        Const.DEBUG = true;

        DummySessionTest session1_1 = new DummySessionTest(1);
        DummySessionTest session1_2 = new DummySessionTest(2);
        StateTest state1 = new StateTest();

        DummySessionTest session2_1 = new DummySessionTest(2);
        DummySessionTest session2_2 = new DummySessionTest(3);
        StateTest state2 = new StateTest();

        DummySessionTest session3_1 = new DummySessionTest(3);
        DummySessionTest session3_2 = new DummySessionTest(1);
        StateTest state3 = new StateTest();

        AtomicInteger snapshot1Done = new AtomicInteger(0);
        AtomicInteger snapshot2Done = new AtomicInteger(0);
        AtomicInteger snapshot3Done = new AtomicInteger(0);

        DistributedNodeTest<Integer> node1 = new DistributedNodeTest<>(state1);
        node1.addSession(session1_1);
        node1.addSession(session1_2);
        node1.addListener((node, snapshot) -> {
            System.out.println("Snapshot1 done!");
            snapshot.writeToFile(new File("snapshot1.snap"));
            snapshot1Done.incrementAndGet();
        });

        DistributedNodeTest<Integer> node2 = new DistributedNodeTest<>(state2);
        node2.addSession(session2_1);
        node2.addSession(session2_2);
        node2.addListener((node, snapshot) -> {
            System.out.println("Snapshot2 done!");
            snapshot.writeToFile(new File("snapshot2.snap"));
            snapshot2Done.incrementAndGet();
        });

        DistributedNodeTest<Integer> node3 = new DistributedNodeTest<>(state3);
        node3.addSession(session3_1);
        node3.addSession(session3_2);
        node3.addListener((node, snapshot) -> {
            System.out.println("Snapshot3 done!");
            snapshot.writeToFile(new File("snapshot3.snap"));
            snapshot3Done.incrementAndGet();
        });

        session1_1.otherEnd = session3_2;
        session3_2.otherEnd = session1_1;

        session1_2.otherEnd = session2_1;
        session2_1.otherEnd = session1_2;

        session2_2.otherEnd = session3_1;
        session3_1.otherEnd = session2_2;

        session1_1.sendPacket(new PacketIncrementTest());
        session2_1.sendPacket(new PacketIncrementTest());
        session3_1.sendPacket(new PacketIncrementTest());
        session3_2.sendPacket(new PacketIncrementTest());

        System.out.println(state1.state + " " + state2.state + " " + state3.state);

        node2.snapshot();

        System.out.println(snapshot1Done.get() + " " + snapshot2Done.get() + " " + snapshot3Done.get());

        assertEquals(1, snapshot1Done.get());
        assertEquals(1, snapshot2Done.get());
        assertEquals(1, snapshot3Done.get());
    }
}
