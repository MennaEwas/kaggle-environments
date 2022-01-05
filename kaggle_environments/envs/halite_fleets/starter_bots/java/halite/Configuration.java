package halite;

public class Configuration {
    
    public final int agentTimeout;
    public final int startingHalite;
    public final int size;
    public final int spawnCost;
    public final int convertCost;
    public final float regenRate;
    public final int maxCellHalite;
    public final int randomSeed;

    public Configuration(String rawConfiguration) {
        this.agentTimeout = HaliteJson.getIntFromJson(rawConfiguration, "agentTimeout");
        this.startingHalite = HaliteJson.getIntFromJson(rawConfiguration, "startingHalite");
        this.size = HaliteJson.getIntFromJson(rawConfiguration, "size");
        this.spawnCost = HaliteJson.getIntFromJson(rawConfiguration, "spawnCost");
        this.convertCost = HaliteJson.getIntFromJson(rawConfiguration, "convertCost");
        this.regenRate = HaliteJson.getFloatFromJson(rawConfiguration, "regenRate");
        this.maxCellHalite = HaliteJson.getIntFromJson(rawConfiguration, "maxCellHalite");
        this.randomSeed = HaliteJson.getIntFromJson(rawConfiguration, "randomSeed");
    }
}