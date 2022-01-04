package halite;

import java.util.Optional;

public class Shipyard {
    public final String id; 
    public final int shipCount; 
    public final Point position; 
    public final int playerId; 
    public final int turnsControlled; 
    public final Board board; 
    public Optional<ShipyardAction> nextAction;
    public final int[] SPAWN_VALUES;
    
    public Shipyard(String shipyardId, int shipCount, Point position, int playerId, int turnsControlled, Board board, Optional<ShipyardAction> nextAction) {
        this.id = shipyardId;
        this.shipCount = shipCount;
        this.position = position;
        this.playerId = playerId;
        this.turnsControlled = turnsControlled;
        this.board = board;
        this.nextAction = nextAction;

        int[] upgradeTimes = new int[9];
        for(int i = 1; i < 10; i++) {
            upgradeTimes[i-1] = (int) Math.pow(i, 2);
        }
        SPAWN_VALUES = new int[9];
        int current = 0;
        for(int i = 1; i < 10; i++) {
            current += upgradeTimes[i-1];
            SPAWN_VALUES[i-1] = current;
        }
    }

    public int maxSpawn() {
        for (int i = 0; i < this.SPAWN_VALUES.length; i++) {
            if (this.turnsControlled < this.SPAWN_VALUES[i]) {
                return i + 1;
            }
        }
        return this.SPAWN_VALUES.length + 1;
    }

    /**
     *  Returns the cell this shipyard is on.
     */
    public Cell cell() {
        return this.board.getCellAtPosition(this.position);
    }

    public Player player() {
        return this.board.players[this.playerId];
    }
   
    /**
     * Converts a shipyard back to the normalized observation subset that constructed it.
     */
    public int[] observation() {
        return new int[]{this.position.toIndex(this.board.configuration.size), this.shipCount, this.turnsControlled};
    }
}