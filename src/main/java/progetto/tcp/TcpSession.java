package progetto.tcp;

import progetto.Session;
import progetto.packet.Packet;
import progetto.session.SessionListener;
import progetto.utils.ListenerList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class TcpSession<ID extends Comparable<ID> & Serializable> implements Session<ID> {
    protected final Socket socket;
    protected final Thread receiveThread;
    protected final Thread writeThead;
    private final AtomicBoolean run = new AtomicBoolean(false);
    protected final ListenerList<SessionListener<ID>> listeners = new ListenerList<>();
    protected final BlockingDeque<Packet> outboundPacketQueue;
    protected ID sessionID;
    protected long lastKeepAlive;

    public TcpSession(Socket socket) {
        this.socket = socket;
        this.receiveThread = new Thread(this::runImpl);
        this.writeThead = new Thread(this::writeThread);
        this.outboundPacketQueue = new LinkedBlockingDeque<>();
        lastKeepAlive = System.currentTimeMillis();
    }

    public String getHostAddress(){
        return socket.getInetAddress().getHostAddress();
    }

    @Override
    public List<SessionListener<ID>> getListeners() {
        return listeners.clone();
    }

    @Override
    public void addListener(SessionListener<ID> sessionListener) {
        listeners.addListener(sessionListener);
    }

    public void start() {
        run.set(true);
        receiveThread.start();
    }

    @Override
    public ID getID() {
        return sessionID;
    }

    protected void runImpl() {
        try {
            writeThead.start();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            while (run.get()) {
                Packet packet = (Packet) in.readObject();
                listeners.forEachListeners(l -> l.onPacketReceived(this, packet));
            }
        } catch (Exception e) {
            e.printStackTrace();
            listeners.forEachListeners(sessionListener -> sessionListener.onDisconnection(this, e));
        } finally {
            run.set(false);
            outboundPacketQueue.clear();
        }
    }

    protected void writeThread() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            while (run.get()) {
                Packet packet = outboundPacketQueue.take();
                out.writeObject(packet);
                listeners.forEachListeners(l -> l.onPacketSent(this, packet));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            run.set(false);
        }
    }

    public boolean isConnected() {
        return socket.isConnected();
    }

    public void disconnect() {
        run.set(false);
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
