package progetto.tcp;

import progetto.Session;
import progetto.packet.Packet;
import progetto.session.SessionListener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class TcpSession implements Session {
    protected final Socket socket;
    protected final Thread receiveThread;
    protected final Thread writeThead;
    private volatile boolean run = false; //make it atomic boolean, just in case
    private final List<SessionListener> listeners = new ArrayList<>();
    protected final BlockingDeque<Packet> outboundPacketQueue;
    public TcpSession(Socket socket) {
        this.socket = socket;
        receiveThread = new Thread(this::runImpl);
        writeThead = new Thread(this::writeThread);
        this.outboundPacketQueue = new LinkedBlockingDeque<>();
    }

    @Override
    public List<SessionListener> getListeners() {
        return listeners; //maybe return copy?
    }

    @Override
    public void addListener(SessionListener sessionListener) {
        listeners.add(sessionListener);
    }

    @Override
    public void start() {
        run = true;
        receiveThread.start();
    }

    protected void runImpl() {
        try {
            listeners.forEach(l -> l.onConnected(this));
            writeThead.start();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            while (run) {
                Packet packet = (Packet) in.readObject();
                listeners.forEach(l -> l.onPacketReceived(this, packet));
            }
        } catch (Exception e) {
            e.printStackTrace();
            listeners.forEach(sessionListener -> sessionListener.onDisconnection(this, e));
        } finally {
            run = false;
        }
    }

    protected void writeThread() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            while (run) {
                Packet packet = outboundPacketQueue.take();
                out.writeObject(packet);
                listeners.forEach(l -> l.onPacketSent(this, packet));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            run = false;
        }
    }

    @Override
    public boolean isConnected() {
        return socket.isConnected();
    }

    @Override
    public void disconnect() {
        run = false;
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendPacket(Packet packet) {
        outboundPacketQueue.add(packet);
    }
}
