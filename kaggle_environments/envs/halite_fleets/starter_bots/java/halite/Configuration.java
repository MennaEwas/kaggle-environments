package halite;

public class Configuration {
    
    public final int agentTimeout;
    public final int startingHalite;
    public final int size;
    public final int spawnCost;
    public final int convertCost;
    public final double regenRate;
    public final int maxCellHalite;
    public final int randomSeed;

    public Configuration(int agentTimeout, int startingHalite, int size, int spawnCost, int convertCost, double regenRate, int maxCellHalite, int randomSeed) {
        this.agentTimeout = agentTimeout;
        this.startingHalite = startingHalite;
        this.size = size;
        this.spawnCost = spawnCost;
        this.convertCost = convertCost;
        this.regenRate = regenRate;
        this.maxCellHalite = maxCellHalite;
        this.randomSeed = randomSeed;
    }
}