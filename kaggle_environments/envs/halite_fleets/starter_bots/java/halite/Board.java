package halite;

import java.util.HashMap;

public class Board {

    public final HashMap<String, Shipyard> shipyards;
    public final HashMap<String, Fleet> fleets;
    public final Player[] players;
    public final Configuration configuration;
    
    public Board() {
        this.shipyards = null;
        this.fleets = null;
        this.players = null;
        this.configuration = null;
    }

    public Cell getCellAtPosition(Point position) {
        return null;
    }

    public int size() {
        return 0;
    }

}
