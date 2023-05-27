package App;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

class ConfigSession {
    private final int id;
    private final String ip;
    private final int port;
    private final float percentage;

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
    private final float multiplier;
    private final int batch;
    private ConfigSession server;
    private final List<ConfigSession> sessions = new ArrayList<ConfigSession>();
    private final long productionTime;

    public ConfigReader(String fileName) throws Exception {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(new FileReader(fileName), JsonObject.class);

        // Get multiplier
        this.multiplier = jsonObject.get("multiplier").getAsFloat();

        //get batch to elaborate per time
        this.batch = jsonObject.get("batch").getAsInt();

        // Get number of nodes
        if(!jsonObject.get("server").isJsonNull()) {
            server = parseSessionServer(jsonObject.get("server").getAsJsonObject());
        }

        // Get node information
        JsonArray clientNodes = jsonObject.getAsJsonArray("nodes");

        for (int i = 0; i < clientNodes.size(); i++) {
            sessions.add(parseSessionNode(clientNodes.get(i).getAsJsonObject()));
        }

        // Get production time
        this.productionTime = jsonObject.get("productionTime").getAsLong();
    }

    private ConfigSession parseSessionNode(JsonObject node) {
        int id = node.get("id").getAsInt();
        String nodeIPs = node.get("ip").getAsString();
        int nodePorts = node.get("port").getAsInt();
        float nodePercentages = node.get("percentage").getAsFloat();
        return new ConfigSession(id, nodeIPs, nodePorts, nodePercentages);
    }

    private ConfigSession parseSessionServer(JsonObject node) {
        String ip = node.get("ip").getAsString();
        int port = node.get("serverPort").getAsInt();
        return new ConfigSession(0, ip, port, 0);
    }

    public ConfigSession getServer() {
        return server;
    }

    public float getMultiplier() {
        return multiplier;
    }

    public int getBatch() { return batch; }

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
