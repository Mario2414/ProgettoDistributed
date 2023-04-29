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

    public void refreshWorkingOn(float amount){
        workingOn = workingOn + amount;
    }

    public void refreshAfterSent(float amount){
        workingOn = workingOn - amount;
        finished = finished + amount;
    }

}
