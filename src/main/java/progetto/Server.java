package progetto;

import progetto.session.ServerListener;

import java.io.Serializable;
import java.util.List;

public interface Server<ID extends Comparable<ID> & Serializable> {
    void addServerListener(ServerListener<ID> listener);
    List<ServerListener<ID>> getServerListeners();
    List<Session<ID>> getActiveSessions();
}
