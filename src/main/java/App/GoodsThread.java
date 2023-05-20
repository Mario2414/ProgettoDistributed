package App;

import App.packets.ArrivingGoods;

import java.util.List;

public class GoodsThread extends Thread {
    private MyAppDistributedNode node;

    private int batch;

    private int numOfNodes;

    private long productionTime;
    private volatile boolean isRunning = true;

    public GoodsThread(MyAppDistributedNode node, int batch, int numOfNodes, long productionTime){
        this.batch = batch;
        this.node = node;
        this.numOfNodes = numOfNodes;
        this.productionTime = productionTime;
    }

    public void run() {
        while (isRunning) {
            float workingOn = node.getState().getWorkingOn();
            if (workingOn - batch >= 0) {
                try {
                    Thread.sleep(1000 * (productionTime));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (numOfNodes == 0) { //is the last node of the production chain
                    node.getState().refreshAfterSent(batch);
                } else {
                    for (MyAppClientSession a : node.getOutgoinglink()) {
                        float newAmount = batch * a.getPercentage();
                        if(isRunning){
                            System.out.println("Sending to " + node.getOutgoinglink().size() + " nodes");
                            System.out.println("new amount " + newAmount);
                            a.sendPacket(new ArrivingGoods(newAmount));
                            node.getState().refreshAfterSent(newAmount);
                        }
                    }
                }
            } else {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void stopThread() throws InterruptedException {
        isRunning = false;
        join();
    }
}
