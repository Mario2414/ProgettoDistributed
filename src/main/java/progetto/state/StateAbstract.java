package progetto.state;

import java.io.*;

public abstract class StateAbstract implements State {
    protected final Object lock;

    public StateAbstract(Object lock) {
        this.lock = lock;
    }

    public StateAbstract() {
        this.lock = this;
    }

    @Override
    public StateAbstract clone() {
        synchronized (lock) {
            try {
                ByteArrayOutputStream pos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(pos);
                out.writeObject(this);

                InputStream in = new ByteArrayInputStream(pos.toByteArray());
                ObjectInputStream oin = new ObjectInputStream(in);
                Object obj = oin.readObject();

                return (StateAbstract) obj;
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }
}
