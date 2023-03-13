package progetto.state;

import java.io.*;

public abstract class State implements Serializable, Cloneable {
    @Override
    public State clone() {
        try {
            ByteArrayOutputStream pos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(pos);
            out.writeObject(this);

            InputStream in = new ByteArrayInputStream(pos.toByteArray());
            ObjectInputStream oin = new ObjectInputStream(in);
            Object obj = oin.readObject();

            return (State) obj;
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
