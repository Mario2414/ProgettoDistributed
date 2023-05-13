package progetto.tcp;

import progetto.session.packet.ClientGreetPacket;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpClientSession<ID extends Comparable<ID> & Serializable> extends TcpSession<ID> {
    private final String host;
    private final int port;

    public TcpClientSession(ID sessionID, String host, int port) throws IOException {
        super(new Socket());
        this.sessionID = sessionID;
        this.host = host;
        this.port = port;

        socket.connect(new InetSocketAddress(host, port));

        if(socket.isConnected()) {
            sendPacket(new ClientGreetPacket<ID>(sessionID));
        }
    }
}
