package App;

import progetto.DistributedNode;
import progetto.DistributedNodeListener;
import progetto.Session;
import progetto.Snapshot;
import progetto.state.State;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MyAppDistributedNode extends DistributedNode implements DistributedNodeListener {

    public MyAppDistributedNode(State state) {
        super(state);
        this.addListener(this);
    }

    public MyAppDistributedNode(File snapshotFile, List<Session> sessions) throws IOException, ClassNotFoundException {
        super(snapshotFile, sessions);
        this.addListener(this);
    }

    @Override
    public void onShapshotCompleted(DistributedNode node, Snapshot snapshot) {
        snapshot.writeToFile(new File("latest.snapshot"));
    }
}
