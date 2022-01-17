export class Configuration {
    
    public readonly agentTimeout: number;
    public readonly startingHalite: number;
    public readonly size: number;
    public readonly spawnCost: number;
    public readonly convertCost: number;
    public readonly regenRate: number;
    public readonly maxRegenCellHalite: number;
    public readonly randomSeed: number;

    public constructor(rawConfiguration: string) {
        const config = JSON.parse(rawConfiguration);
        this.agentTimeout = config.agentTimeout;
        this.startingHalite = config.startingHalite;
        this.size = config.size;
        this.spawnCost = config.spawnCost;
        this.convertCost = config.convertCost;
        this.regenRate = config.regenRate;
        this.maxRegenCellHalite = config.maxRegenCellHalite;
        this.randomSeed = config.randomSeed;
    }
}