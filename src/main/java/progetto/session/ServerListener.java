package progetto.session;

import progetto.Server;
import progetto.Session;

public interface ServerListener {
    void onSessionAccepted(Server server, Session session);
    void onServerClosed(Server server, Throwable t);
}
