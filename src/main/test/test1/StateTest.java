package test1;

import progetto.state.State;
import progetto.state.StateAbstract;

public class StateTest extends StateAbstract {
    int state;
    public StateTest() {
        super();
        this.state = 0;
    }

    @Override
    public synchronized void restore(State state) {
        this.state = ((StateTest)state).state;
    }
}
