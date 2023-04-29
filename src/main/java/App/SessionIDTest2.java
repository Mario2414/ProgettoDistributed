package App;

import progetto.session.SessionID;



public class SessionIDTest2 implements SessionID{
    private int id;

    public SessionIDTest2(int id) {
        this.id = id;
    }

    public int getID() {
        return id;
    }


    @Override
    public int compareTo(SessionID o) {
        if(o instanceof SessionIDTest2) {
            return this.id - ((SessionIDTest2) o).id ;
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof SessionID) {
            return this.compareTo((SessionID) obj) == 0;
        }
        return false;
    }


}
