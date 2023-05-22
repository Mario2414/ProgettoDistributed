package common;

import progetto.Session;
import progetto.packet.Packet;
import progetto.session.SessionListener;
import progetto.utils.ListenerList;

import java.util.List;

public class DummySessionTest implements Session<Integer> {
    public DummySessionTest otherEnd;

    public int id;

    public DummySessionTest(int id) {
        this.id = id;
    }

    protected final ListenerList<SessionListener<Integer>> listeners = new ListenerList<>();

    @Override
    public List<SessionListener<Integer>> getListeners() {
        return listeners.clone();
    }

    @Override
    public void addListener(SessionListener<Integer> session) {
        listeners.addListener(session);
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void disconnect() {

    }

    @Override
    public void sendPacket(Packet packet) {
        if(otherEnd != null) otherEnd.receivePacket(this, packet);
        listeners.forEachListeners(l -> l.onPacketSent(this, packet));
    }

    public void receivePacket(DummySessionTest session, Packet packet) {
        listeners.forEachListeners(l -> l.onPacketReceived(this, packet));
    }

    @Override
    public void start() {

    }

    @Override
    public Integer getID() {
        return id;
    }
}
