package progetto.tcp;

import progetto.Session;
import progetto.packet.Packet;
import progetto.session.SessionID;
import progetto.session.SessionListener;
import progetto.session.packet.KeepAlivePingPacket;
import progetto.session.packet.KeepAlivePongPacket;
import progetto.utils.ListenerList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class TcpSession implements Session {
    protected final Socket socket;
    protected final Thread receiveThread;
    protected final Thread writeThead;
    private volatile boolean run = false; //make it atomic boolean, just in case
    protected final ListenerList<SessionListener> listeners = new ListenerList<>();
    protected final BlockingDeque<Packet> outboundPacketQueue;
    protected SessionID sessionID;
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
    public List<SessionListener> getListeners() {
        return listeners.clone();
    }

    @Override
    public void addListener(SessionListener sessionListener) {
        listeners.addListener(sessionListener);
    }

    @Override
    public void start() {
        run = true;
        receiveThread.start();
    }

    @Override
    public SessionID getID() {
        return sessionID;
    }

    protected void runImpl() {
        try {
            listeners.forEachListeners(l -> l.onConnected(this));
            writeThead.start();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            while (run) {
                Packet packet = (Packet) in.readObject();
                if(packet instanceof KeepAlivePingPacket) {
                    lastKeepAlive = ((KeepAlivePingPacket) packet).getTime();
                    sendPacket(new KeepAlivePongPacket(((KeepAlivePingPacket) packet).getTime()));
                }
                listeners.forEachListeners(l -> l.onPacketReceived(this, packet));
            }
        } catch (Exception e) {
            e.printStackTrace();
            listeners.forEachListeners(sessionListener -> sessionListener.onDisconnection(this, e));
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
                listeners.forEachListeners(l -> l.onPacketSent(this, packet));
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
