package progetto;

import progetto.session.ServerListener;

import java.io.Serializable;
import java.util.List;

public interface Server<ID extends Comparable<ID> & Serializable> {
    String getHost();
    int getPort();
    void bind();
    void close();

    void addServerListener(ServerListener<ID> listener);
    List<ServerListener<ID>> getServerListeners();

    List<Session<ID>> getActiveSessions();
}
