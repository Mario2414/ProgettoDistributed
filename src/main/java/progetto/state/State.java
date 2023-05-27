package progetto.state;

import java.io.*;

public interface State extends Serializable, Cloneable {
    /**
     * Method to implement the restore of a saved state.
     * @param state The state to be restored.
     */
    void restore(State state);

    /**
     * Method to implement the cloning of a state.
     * @return The deep copy of the current state
     */
    State clone();
}
