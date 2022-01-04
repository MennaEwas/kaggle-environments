package halite;

import java.util.Arrays;
import java.util.HashMap;

public class Player {
    public final String id; 
    public final int halite; 
    public final String[] shipyardIds; 
    public final String[] fleetIds;
    public final Board board;
    
    public Player(String playerId, int halite, String[] shipyardIds, Stirng[] fleetIds, Board board) {
        this.id = playerId;
        this.halite = halite;
        this.shipyardIds = shipyardIds;
        this.fleetIds = fleetIds;
        this.board = board;
    }

    /**
     * Returns all shipyards owned by this player.
     * @return
     */
    public Shipyard[] shipyards() {
        return (Shipyard[])this.board.shipyards.values().stream().filter(shipyard -> Arrays.stream(this.shipyardIds).anyMatch(sId -> sId == shipyard.id)).toArray();
    }

    /**
     * Returns all fleets owned by this player.
     */
    public Fleet[] fleets() {
        return (Fleet[])this.board.fleets.values().stream().filter(fleet -> Arrays.stream(this.fleetIds).anyMatch(fId -> fId == fleet.id)).toArray();
    }

    /**
     * Returns whether this player is the current player (generally if this returns True, this player is you.
     */
    public boolean isCurrentPlayer() {
        return this.id == this.board.currentPlayerId;
    }

    /**
     * Returns all queued fleet and shipyard actions for this player formatted for the halite interpreter to receive as an agent response.
     */
    public HashMap<String, ShipyardAction> nextActions() {
        HashMap<String, ShipyardAction> result = new HashMap<>();
        Arrays.stream(this.shipyards()).filter(shipyard -> shipyard.nextAction.isPresent()).forEach(shipyard -> result.put(shipyard.id, shipyard.nextAction.get()));
        return result;
    }

    /**
     * Converts a player back to the normalized observation subset that constructed it.
     */
    public Object[] observation() {
        HashMap<String, int[]> shipyards = new HashMap<>(); 
        Arrays.stream(this.shipyards()).forEach(shipyard -> shipyards.put(shipyard.id, shipyard.observation()));
        HashMap<String, String[]> fleets = new HashMap<>();
        Arrays.stream(this.fleets()).forEach(fleet -> fleets.put(fleet.id, fleet.observation()));
        return new Object[]{self.halite, shipyards, fleets};
    }
}
