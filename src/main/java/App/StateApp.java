package App;

import progetto.state.State;
import progetto.state.StateAbstract;

public class StateApp extends StateAbstract {
    private float finished;
    private float workingOn;

    public StateApp(){
        super();
        this.workingOn = 0;
        this.finished = 0;
    }

    public synchronized float getWorkingOn(){
        return workingOn;
    }

    public synchronized void refreshWorkingOn(float amount){
        workingOn = workingOn + amount;
    }

    public synchronized void refreshAfterSent(float amount){
        workingOn = workingOn - amount;
        finished = finished + amount;
    }
    @Override
    public synchronized void restore(State state) {
        StateApp stateA = (StateApp) state;
        this.finished = stateA.finished;
        this.workingOn = stateA.workingOn;
    }

    @Override
    public synchronized String toString() {
        return "StateApp{" +
                "finished=" + finished +
                ", workingOn=" + workingOn +
                '}';
    }
}
