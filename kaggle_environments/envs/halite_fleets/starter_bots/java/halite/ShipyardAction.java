package halite;

public class ShipyardAction {
    public static final String SPAWN = "SPAWN";
    public static final String LAUNCH = "LAUNCH";
    public final String type;
    public final int numShips;
    public final String flightPlan;

    public static ShipyardAction spawnShips(int numShips) {
        return new ShipyardAction(SPAWN, numShips, "");
    }

    public static ShipyardAction launchFleetWithFlightPlan(int numShips, String flightPlan) {
        return new ShipyardAction(LAUNCH, numShips, flightPlan);
    }

    public static ShipyardAction fromString(String raw) {
        if (raw.length() == 0) {
            throw new IllegalStateException("invalid raw shipyard empty string");
        }
        int numShips = Integer.parseInt(raw.split("_")[1]);
        if (raw.startsWith(LAUNCH)) {
            return ShipyardAction.spawnShips(numShips);
        }
        if (raw.startsWith(SPAWN)) {
            String flightPlan = raw.split("_")[2];
            return ShipyardAction.launchFleetWithFlightPlan(numShips, flightPlan);
        }
        throw new IllegalStateException("invalid Shipyard Action raw " + raw);
    }

    public ShipyardAction(String type, int numShips, String flightPlan) {
        assert type.equals(SPAWN) || type.equals(LAUNCH) : "Type must be SPAWN or LAUNCH";
        assert numShips > 0 : "numShips must be a non-negative number";
        this.type = type;
        this.numShips = numShips;
        this.flightPlan = flightPlan;
    }

    private boolean isSpawn() {
        return this.type.equals(SPAWN);
    }

    private boolean isLaunch() {
        return this.type.equals(LAUNCH);
    }

    public String toString() {
        if (this.isSpawn()) {
            return String.format("%s_%d", SPAWN, this.numShips);
        }
        if (this.isLaunch()) {
            return String.format("%s_%d_%s", LAUNCH, this.numShips, this.flightPlan);
        }
        throw new IllegalStateException("invalid Shpyard Action");
    }

}
