package App;

import progetto.state.State;

public class StateApp extends State {
    private float finished;
    private float workingOn;

    public StateApp(){
        this.workingOn = 0;
        this.finished = 0;
    }

    public StateApp(float workingOn,float finished) {
        this.workingOn = workingOn;
        this.finished = finished;
    }

    public synchronized void refreshWorkingOn(float amount){
        workingOn = workingOn + amount;
    }

    public synchronized void refreshAfterSent(float amount){
        workingOn = workingOn - amount;
        finished = finished + amount;
    }

    @Override
    public synchronized String toString() {
        return "StateApp{" +
                "finished=" + finished +
                ", workingOn=" + workingOn +
                '}';
    }
}
