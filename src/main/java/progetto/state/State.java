package progetto.state;

import java.io.*;

public interface State extends Serializable, Cloneable {
    void restore(State state);

    State clone();
}
