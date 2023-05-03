package progetto.session;

import progetto.Server;
import progetto.Session;

import java.io.Serializable;

public interface ServerListener<ID extends Comparable<ID> & Serializable> {
    void onSessionAccepted(Server<ID> server, Session<ID> session);
    void onSessionClosed(Server<ID> server, Session<ID> session);
    void onServerClosed(Server<ID> server, Throwable t);
}
