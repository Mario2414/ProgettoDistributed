package progetto.tcp;

import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpClientSession extends TcpSession {
    private final String host;
    private final int port;

    public TcpClientSession(String host, int port) {
        super(new Socket());
        this.host = host;
        this.port = port;
    }

    @Override
    protected void runImpl() {
        try {
            socket.connect(new InetSocketAddress(host, port));
        } catch (Exception e) {
            e.printStackTrace();
            //TODO
            return;
        }
        super.runImpl();
    }
}
