package progetto.tcp;

import progetto.session.packet.ClientGreetPacket;

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

    @Override
    protected void runImpl() {
        try {
            socket.connect(new InetSocketAddress(host, port));

        } catch (Exception e) {
            System.out.println("connection to "+ host +" failed");
            //e.printStackTrace();
            //listeners.forEachListeners(sessionListener -> sessionListener.onDisconnection(this, e));
            //return; //cause strange behaviour the thread does not terminate and return to main
        }
        if(socket.isConnected()) {
            sendPacket(new ClientGreetPacket(sessionID));
            super.runImpl();
        }
    }
}
