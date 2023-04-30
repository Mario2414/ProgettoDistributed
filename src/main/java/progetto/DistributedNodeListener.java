package progetto;

public interface DistributedNodeListener {
    void onShapshotCompleted(DistributedNode node, Snapshot snapshot);
}
