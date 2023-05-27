package progetto.session;

import progetto.Session;
import progetto.packet.Packet;

import java.io.Serializable;

public interface SessionListener<ID extends Comparable<ID> & Serializable> {
    /**
     * Method to notify that a packet has been received by a session
     * @param session Reference to the server object that this event is generated by.
     *                Useful in case a class registers to multiple sessions with the same listener
     * @param packet Packet that has been received
     */
    void onPacketReceived(Session<ID> session, Packet packet);

    /**
     * Method to notify that a packet has been sent by a session
     * @param session Reference to the server object that this event is generated by.
     *                Useful in case a class registers to multiple sessions with the same listener
     * @param packet Packet that has been sent
     */
    void onPacketSent(Session<ID> session, Packet packet);

    /**
     * Method to notify that the session is now connected.
     * @param session Reference to the server object that this event is generated by.
     *               Useful in case a class registers to multiple sessions with the same listener
     */
    void onConnected(Session<ID> session);

    /**
     * Method to notify that the session is now disconnected.
     * @param session Reference to the server object that this event is generated by.
     *                Useful in case a class registers to multiple sessions with the same listener
     * @param exception Exception in case the disconnection was caused by an exception. Might be null.
     */
    void onDisconnection(Session<ID> session, Throwable exception);
}
