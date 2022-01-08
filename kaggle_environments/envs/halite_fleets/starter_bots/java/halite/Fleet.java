package halite;

public class Fleet {

    public final String id;
    public int shipCount;
    public Direction direction;
    public Point position;
    public String flightPlan;
    public float halite;
    public final int playerId;
    public final Board board;
    
    public Fleet(String fleetId, int shipCount, Direction direction, Point position, float halite, String flightPlan, int playerId, Board board) {
        this.id = fleetId;
        this.shipCount = shipCount;
        this.direction = direction;
        this.position = position;
        this.flightPlan = flightPlan;
        this.halite = halite;
        this.playerId = playerId;
        this.board = board;
    }

    public Fleet cloneToBoard(Board board) {
        return new Fleet(this.id, this.shipCount, this.direction, this.position, this.halite, this.flightPlan, this.playerId, board);
    }

    public Cell cell() {
        return this.board.getCellAtPosition(this.position);
    }

    public Player player() {
        return this.board.players[this.playerId];
    }

    public double collectionRate() {
        return Math.min(Math.log(this.shipCount) / 10, .99);
    }

    /**
     * Returns the length of the longest possible flight plan this fleet can be assigned
     * @return
     */
    public static int maxFlightPlanLenForShipCount(int shipCount) {
        return (int) (Math.floor(2 * Math.log(shipCount)) + 1);
    }

    /**
     * Converts a fleet back to the normalized observation subset that constructed it.
     */
    public String[] observation() {
        return new String[]{
            String.valueOf(this.position.toIndex(this.board.configuration.size)), 
            String.valueOf(this.halite), 
            String.valueOf(this.shipCount), 
            String.valueOf(this.direction.toIndex()), 
            this.flightPlan
        };
    }

    public boolean lessThanOtherAlliedFleet(Fleet other) {
        if (this.shipCount != other.shipCount) {
            return this.shipCount < other.shipCount;
        }
        if (this.halite != other.halite) {
            return this.halite < other.halite;
        }
        return this.direction.toIndex() > other.direction.toIndex();
}

    }
