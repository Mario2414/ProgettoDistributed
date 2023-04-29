package App;

import java.io.FileReader;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ConfigReader {
    private float multiplier;
    private int numOfNodes;
    private String[] nodeIPs;
    private int[] nodePorts;
    private float[] nodePercentages;
    private long productionTime;

    public ConfigReader(String fileName) throws Exception {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(new FileReader(fileName), JsonObject.class);

        // Get multiplier
        this.multiplier = jsonObject.get("multiplier").getAsFloat();

        // Get number of nodes
        this.numOfNodes = jsonObject.get("numOfNodes").getAsInt();

        // Get node information
        this.nodeIPs = new String[numOfNodes];
        this.nodePorts = new int[numOfNodes];
        this.nodePercentages = new float[numOfNodes];

        for (int i = 0; i < numOfNodes; i++) {
            JsonObject node = jsonObject.getAsJsonArray("nodes").get(i).getAsJsonObject();
            this.nodeIPs[i] = node.get("ip").getAsString();
            this.nodePorts[i] = node.get("port").getAsInt();
            this.nodePercentages[i] = node.get("percentage").getAsFloat();
        }

        // Get production time
        this.productionTime = jsonObject.get("productionTime").getAsLong();
    }

    public float getMultiplier() {
        return multiplier;
    }

    public int getNumOfNodes() {
        return numOfNodes;
    }

    public String[] getNodeIPs() {
        return nodeIPs;
    }

    public int[] getNodePorts() {
        return nodePorts;
    }

    public float[] getNodePercentages() {
        return nodePercentages;
    }

    public long getProductionTime() {
        return productionTime;
    }






}
