package App.packets;

import progetto.packet.Packet;

import java.util.UUID;

public class SomeoneDown implements Packet{

    private String ip;
    private UUID uuid;


    public SomeoneDown(String ip){
        this.ip = ip;
        uuid = UUID.randomUUID();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;  // same object
        }
        if (obj == null || !(obj instanceof SomeoneDown)) {
            return false;  // null or different class
        }
        if(obj instanceof SomeoneDown) {
            return (this.getUuid().equals(((SomeoneDown) obj).getUuid()) && this.getIp().equals(((SomeoneDown) obj).getIp()));
        }
        return false;
    }

    public UUID getUuid(){
        return uuid;
    }

    public String getIp(){
        return ip;
    }
}
