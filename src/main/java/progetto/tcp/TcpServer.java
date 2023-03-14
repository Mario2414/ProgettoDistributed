package progetto.tcp;

import progetto.Server;
import progetto.Session;
import progetto.session.ServerListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TcpServer implements Server {
    private final Thread thread;
    private final String host;
    private final int port;
    private final ServerSocket server;
    private volatile boolean run = false;
    private final List<ServerListener> listeners = new ArrayList<>();

    public TcpServer(String host, int port) {
        this.host = host;
        this.port = port;
        try {
            this.server = new ServerSocket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.thread = new Thread(this::startServer);
    }
    @Override
    public String getHost() {
        return null;
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public void bind() {
        run = true;
        thread.start();
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
    public void addServerListener(ServerListener listener) {
        listeners.add(listener);
    }

    @Override
    public List<ServerListener> getServerListeners() {
        return listeners; //TODO maybe clone?
    }

    private void startServer() {
        try {
            server.bind(new InetSocketAddress(host, port));
            while (run) {
                Socket socket = server.accept();
                Session session = new TcpServerSession(socket);
                listeners.forEach(l -> l.onSessionAccepted(this, session));
                session.start();
            }
        } catch (Exception e){
            e.printStackTrace();
            listeners.forEach(l -> l.onServerClosed(this, e));
        }
    }
}
