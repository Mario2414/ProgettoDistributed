package progetto.tcp;

import progetto.session.packet.ClientGreetPacket;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpClientSession<ID extends Comparable<ID> & Serializable> extends TcpSession<ID> {
    private final String host;
    private final int port;

    public TcpClientSession(ID sessionID, String host, int port) {
        super(new Socket());
        this.sessionID = sessionID;
        this.host = host;
        this.port = port;
    }

    public void connect(boolean blocking) throws IOException {
        if(blocking) {
            _connectBlocking();
        } else {
            new Thread(() -> {
                try {
                    _connectBlocking();
                } catch (Exception e) {
                    e.printStackTrace();
                    listeners.forEachListeners(l -> l.onDisconnection(this, e));
                }
            }).start();
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    private void _connectBlocking() throws IOException {
        socket.connect(new InetSocketAddress(host, port));
        sendPacket(new ClientGreetPacket<ID>(sessionID));
    }
}
