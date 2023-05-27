package App.packets;

import progetto.packet.Packet;

public class ArrivingGoods implements Packet{
    private final float amount;

    public ArrivingGoods(float amount){
        this.amount = amount;
    }

    public float getAmount(){
        return amount;
    }
}
