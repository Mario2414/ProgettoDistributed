package App;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

class ConfigSession {
    private int id;
    private String ip;
    private int port;

    private float percentage;

    public ConfigSession(int id, String ip, int port, float percentage) {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.percentage = percentage;
    }

    public int getId() {
        return id;
    }

    public float getPercentage() {
        return percentage;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}

public class ConfigReader {
    private float multiplier;

    private ConfigSession server;
    private List<ConfigSession> sessions = new ArrayList<ConfigSession>();
    private long productionTime;

    public ConfigReader(String fileName) throws Exception {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(new FileReader(fileName), JsonObject.class);

        // Get multiplier
        this.multiplier = jsonObject.get("multiplier").getAsFloat();

        // Get number of nodes
        if(!jsonObject.get("server").isJsonNull()) {
            server = parseSession(jsonObject.get("server").getAsJsonObject());
        }

        // Get node information
        JsonArray clientNodes = jsonObject.getAsJsonArray("nodes");

        for (int i = 0; i < clientNodes.size(); i++) {
            sessions.add(parseSession(clientNodes.get(i).getAsJsonObject()));
        }

        // Get production time
        this.productionTime = jsonObject.get("productionTime").getAsLong();
    }

    private ConfigSession parseSession(JsonObject node) {
        int id = node.get("id").getAsInt();
        String nodeIPs = node.get("ip").getAsString();
        int nodePorts = node.get("port").getAsInt();
        float nodePercentages = node.get("percentage").getAsFloat();
        return new ConfigSession(id, nodeIPs, nodePorts, nodePercentages);
    }

    public ConfigSession getServer() {
        return server;
    }

    public float getMultiplier() {
        return multiplier;
    }

    public int getNumOfNodes() {
        return sessions.size();
    }

    public List<ConfigSession> getClientSessions() {
        return sessions;
    }

    public long getProductionTime() {
        return productionTime;
    }
}
