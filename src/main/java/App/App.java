package App;


import App.packets.ArrivingGoods;

import progetto.*;
import progetto.packet.Packet;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;


public class App {
    private static ConcurrentLinkedQueue<Packet> recoveryPackets = new ConcurrentLinkedQueue<>();

    private static ConcurrentLinkedQueue<Session<Integer>> outgoingLinks = new ConcurrentLinkedQueue<>(); //Accesso in maniera concorrente nei listener

    private static ConcurrentLinkedQueue<Integer> notConnectedNodes = new ConcurrentLinkedQueue<>(); //acceddo in maniera concorrente nei listener

    private static MyAppDistributedNode node;

    public static void main(String[] args) {
        node = new MyAppDistributedNode(new StateApp());

        System.out.println("Node started");

        Scanner stdin = new Scanner(System.in);
        boolean retry;
        int numInput = 1;

        while(true) {
            System.out.println("To add manual goods press 1");
            System.out.println("To start a snapshot press 2");
            System.out.println("To restore from a snapshot press 3");
            System.out.println("To print the state press 4");
            do {
                try{
                    numInput = Integer.parseInt(stdin.nextLine());
                    retry = false;
                }catch (Exception e){
                    System.out.println("Please insert a number");
                    retry = true;
                }
            } while (retry);

            if(numInput == 1){
                float numGoods = 0;
                do {
                    try{
                        System.out.println("Please insert the number of goods to be processed");
                        numGoods = Float.parseFloat(stdin.nextLine());
                        retry = false;
                    } catch (Exception e){
                        System.out.println("Please insert a number");
                        retry = true;
                    }
                } while (retry);

                node.sendGoods(new ArrivingGoods(numGoods));
            } else if (numInput == 2){
                node.snapshot();
            } else if(numInput == 3) {
                node.restore();
            } else if(numInput == 4) {
                System.out.println(node.getState().toString());
            }

        }
    }
}




