import {Board} from "./Board";
import {Shipyard} from "./Shipyard";

export class Player {
    public readonly id: number; 
    public halite: number; 
    public readonly shipyardIds: string[]; 
    public readonly fleetIds: string[];
    public readonly board: Board;
    
    public constructor(playerId: number, halite: number, shipyardIds: string[], fleetIds: string[], board: Board) {
        this.id = playerId;
        this.halite = halite;
        this.shipyardIds = shipyardIds;
        this.fleetIds = fleetIds;
        this.board = board;
    }

    public cloneToBoard(board: Board): Player {
        return new Player(this.id, this.halite, new ArrayList<String>(this.shipyardIds.stream().collect(Collectors.toList())), new ArrayList<String>(this.fleetIds.stream().collect(Collectors.toList())), board);
    }

    /**
     * Returns all shipyards owned by this player.
     * @return
     */
    public shipyards(): Shipyard[] {
        return this.board.shipyards.values().stream().filter(shipyard -> this.shipyardIds.stream().anyMatch(sId -> sId == shipyard.id)).toArray(Shipyard[]::new);
    }

    /**
     * Returns all fleets owned by this player.
     */
    public fleets(): Fleet[] {
        return this.board.fleets.values().stream().filter(fleet -> this.fleetIds.stream().anyMatch(fId -> fId == fleet.id)).toArray();
    }

    /**
     * Returns whether this player is the current player (generally if this returns True, this player is you.
     */
    public isCurrentPlayer(): boolean {
        return this.id == this.board.currentPlayerId;
    }

    /**
     * Returns all queued fleet and shipyard actions for this player formatted for the halite interpreter to receive as an agent response.
     */
    public nextActions(): Map<String, ShipyardAction> {
        HashMap<String, ShipyardAction> result = new HashMap<>();
        Arrays.stream(this.shipyards()).filter(shipyard -> shipyard.nextAction.isPresent()).forEach(shipyard -> result.put(shipyard.id, shipyard.nextAction.get()));
        return result;
    }

    /**
     * Converts a player back to the normalized observation subset that constructed it.
     */
    public observation(): any[] {
        HashMap<String, int[]> shipyards = new HashMap<>(); 
        Arrays.stream(this.shipyards()).forEach(shipyard -> shipyards.put(shipyard.id, shipyard.observation()));
        HashMap<String, String[]> fleets = new HashMap<>();
        Arrays.stream(this.fleets()).forEach(fleet -> fleets.put(fleet.id, fleet.observation()));
        return new Object[]{this.halite, shipyards, fleets};
    }
}
