package progetto;

import progetto.session.ServerListener;

import java.util.List;

public interface Server {
    String getHost();
    int getPort();
    void bind();
    void close();

    void addServerListener(ServerListener listener);
    List<ServerListener> getServerListeners();

    List<Session> getActiveSessions();
}
