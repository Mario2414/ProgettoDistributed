package App;

import App.packets.ArrivingGoods;
import progetto.Session;
import progetto.packet.Packet;
import progetto.session.SessionListener;
import progetto.state.State;
import progetto.tcp.TcpClientSession;

import java.io.IOException;

public class MyAppClientSession extends TcpClientSession<Integer> implements SessionListener<Integer> {
    private final float percentage;
    public MyAppClientSession(Integer sessionID, String host, int port, float percentage, StateApp state) {
        super(sessionID, host, port);
        addListener(this);
        this.percentage = percentage;
    }

    @Override
    public void onPacketReceived(Session<Integer> session, Packet packet) {

    }

    @Override
    public void onPacketSent(Session<Integer> session, Packet packet) {

    }

    @Override
    public void onConnected(Session<Integer> session) {

    }

    @Override
    public void onDisconnection(Session<Integer> session, Throwable exception) {

    }


    public float getPercentage() {
        return percentage;
    }
}
