package halite;

public class Observation {
    public final int[] halite;
    public final int[][] players;
    public final int player;
    
    public Observation(int[] halite, int[][] players, int player) {
        this.halite = halite;
        this.players = players;
        this.player = player;
    }
    
    
}
