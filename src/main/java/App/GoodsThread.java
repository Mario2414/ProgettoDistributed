package App;

import App.packets.ArrivingGoods;

public class GoodsThread extends Thread {
    private final MyAppDistributedNode node;
    private final int batch;
    private final int numOfNodes;
    private final long productionTime;
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
                    System.out.println("Sending to " + node.getOutgoingLinks().size() + " nodes");
                    for (MyAppClientSession a : node.getOutgoingLinks()) {
                        float amountToSend = batch * a.getPercentage();
                        System.out.println("Sending " + amountToSend);
                        a.sendPacket(new ArrivingGoods(amountToSend));
                        node.getState().refreshAfterSent(amountToSend);
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
