package progetto;

import progetto.packet.Packet;
import progetto.session.SessionListener;

import java.io.Serializable;
import java.util.List;

public interface Session<ID extends Comparable<ID> & Serializable> {
    /**
     * Returns listeners for the session.
     * @return listeners for the session.
     */
    List<SessionListener<ID>> getListeners();

    /**
     * Add a listener to the session
     * @param session The listener to be added
     */
    void addListener(SessionListener<ID> session);

    /**
     * Sends a packet
     * @param packet Packet to send
     */
    void sendPacket(Packet packet);

    /**
     * Unique identifier for the link
     * @return the id of the link
     */
    ID getID();
}
