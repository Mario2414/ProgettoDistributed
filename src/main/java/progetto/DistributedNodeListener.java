package progetto;

import java.io.Serializable;

public interface DistributedNodeListener<ID extends Comparable<ID> & Serializable> {
    void onShapshotCompleted(DistributedNode<ID> node, Snapshot<ID> snapshot);
}
