package App;

import progetto.Session;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class SnapshotRestore {
    private final UUID uuid;
    private final Optional<Session<Integer>> restoreInitiator;
    private final HashSet<Integer> pendingSessions;

    public SnapshotRestore(UUID uuid, Optional<Session<Integer>> restoreInitiator, Collection<Session<Integer>> sessions) {
        this.uuid = uuid;
        this.restoreInitiator = restoreInitiator;
        this.pendingSessions = sessions.stream().map(Session::getID).collect(Collectors.toCollection(HashSet::new));
        //System.out.println("ci sono tot sessioni aperte: "+this.pendingSessions.size());
    }

    // Check if a session is pending in the snapshot
    public boolean isSessionPending(Integer session) {
        synchronized (pendingSessions) {
            System.out.println("isSessionPending: Missing " + pendingSessions.size());
            return pendingSessions.contains(session);
        }
    }

    // Mark a session as done in the snapshot
    public void markSessionAsDone(Integer id) {
        synchronized (pendingSessions) {
            pendingSessions.remove(id);
            System.out.println("markSessionAsDone: Missing " + pendingSessions.size());
        }
    }

    // Check if the snapshot is complete
    public boolean isSnapshotRestoreComplete() {
        synchronized(pendingSessions) {
            return pendingSessions.isEmpty();
        }
    }

    public Optional<Session<Integer>> getRestoreInitiator() {
        return restoreInitiator;
    }
}
