package progetto.tcp;

import progetto.Server;
import progetto.Session;
import progetto.packet.Packet;
import progetto.session.ServerListener;
import progetto.session.SessionListener;
import progetto.session.packet.KeepAlivePingPacket;
import progetto.session.packet.KeepAlivePongPacket;
import progetto.utils.ListenerList;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TcpServer<ID extends Comparable<ID> & Serializable> implements Server<ID>, SessionListener<ID> {
    private final Thread thread;
    private final Thread keepAliveThread;
    private final String host;
    private final int port;
    private final ServerSocket server;
    private volatile boolean run = false;
    private final ListenerList<ServerListener<ID>> listeners = new ListenerList<>();

    private final List<Session<ID>> activeSessions = new LinkedList<>();
    private final Map<ID, Long> lastKeepAlive = new ConcurrentHashMap<>();
    private final long maxKeepAliveTimeGap = 5000; //5 secs

    public TcpServer(String host, int port) {
        this.host = host;
        this.port = port;
        try {
            this.server = new ServerSocket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.thread = new Thread(this::startServer);
        this.keepAliveThread = new Thread(this::startKeepAlive);
    }
    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void bind() {
        run = true;
        thread.start();
        keepAliveThread.start();
    }

    @Override
    public void close() {
        try {
            run = false;
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addServerListener(ServerListener<ID> listener) {
        listeners.addListener(listener);
    }

    @Override
    public List<ServerListener<ID>> getServerListeners() {
        return listeners.clone();
    }

    private void startServer() {
        try {
            server.bind(new InetSocketAddress(host, port));
            while (run) {
                Socket socket = server.accept();
                Session<ID> session = new TcpServerSession<ID>(socket);
                lastKeepAlive.put(session.getID(), System.currentTimeMillis());
                synchronized (activeSessions) {
                    activeSessions.add(session);
                }
                listeners.forEachListeners(l -> l.onSessionAccepted(this, session));
                session.start();
                session.addListener(this);
            }
        } catch (Exception e){
            e.printStackTrace();
            listeners.forEachListeners(l -> l.onServerClosed(this, e));
        }
    }

    private void startKeepAlive() {
        try {
            while (run) {
                synchronized (activeSessions) {
                    long time = System.currentTimeMillis();

                    Iterator<Session<ID>> it = activeSessions.iterator();
                    while (it.hasNext()) {
                        Session<ID> session = it.next();
                        if (time - lastKeepAlive.get(session.getID()) > maxKeepAliveTimeGap * 3) {
                            it.remove();
                            lastKeepAlive.remove(session.getID());
                            session.disconnect(); //the callback onSessionClosed will be called in the onDisconnection method
                        } else {
                            session.sendPacket(new KeepAlivePingPacket(time));
                        }
                    }

                    Thread.sleep(maxKeepAliveTimeGap);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    //Session Listener methods
    @Override
    public void onPacketReceived(Session<ID> session, Packet packet) {
        if(packet instanceof KeepAlivePongPacket) {
            lastKeepAlive.put(session.getID(), ((KeepAlivePongPacket) packet).getTime());
        }
    }

    @Override
    public void onPacketSent(Session<ID> session, Packet packet) {

    }

    @Override
    public void onConnected(Session<ID> session) {

    }

    @Override
    public List<Session<ID>> getActiveSessions() {
        synchronized (this) {
            return new ArrayList<>(activeSessions);
        }
    }

    @Override
    public void onDisconnection(Session<ID> session, Throwable exception) {
        listeners.forEachListeners(l -> l.onSessionClosed(this, session));
        lastKeepAlive.remove(session.getID());
        synchronized (activeSessions) {
            activeSessions.removeIf(s -> s.getID() == session.getID());
        }
    }
}
