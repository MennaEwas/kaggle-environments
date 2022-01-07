package halite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;

public class Board {

    public final HashMap<String, Shipyard> shipyards;
    public final HashMap<String, Fleet> fleets;
    public final Player[] players;
    public final int currentPlayerId;
    public final Configuration configuration;
    public final int step;
    public final float remainingOverageTime;
    public final Cell[] cells;
    public final int size;

    public Board(HashMap<String, Shipyard> shipyards, HashMap<String, Fleet> fleets, Player[] players, int currentPlayerId, Configuration configuration, int step, float remainingOverageTime, Cell[] cells, int size) {
        this.shipyards = new HashMap<>();
        shipyards.entrySet().stream().forEach(entry -> this.shipyards.put(entry.getKey(), entry.getValue().cloneToBoard(this)));
        this.fleets = new HashMap<>();
        fleets.entrySet().stream().forEach(entry -> this.fleets.put(entry.getKey(), entry.getValue().cloneToBoard(this)));
        this.players = Arrays.stream(players).map(player -> player.cloneToBoard(this)).toArray(Player[]::new);
        this.currentPlayerId = currentPlayerId;
        this.configuration = configuration;
        this.step = step;
        this.remainingOverageTime = remainingOverageTime;
        this.cells = Arrays.stream(cells).map(cell -> cell.cloneToBoard(this)).toArray(Cell[]::new);
        this.size = size;
    }

    /**
     * Creates a board from the provided observation, configuration, and next_actions as specified by
     *  https://github.com/Kaggle/kaggle-environments/blob/master/kaggle_environments/envs/halite/halite.json
     *  Board tracks players (by id), fleets (by id), shipyards (by id), and cells (by position).
     *  Each entity contains both key values (e.g. fleet.player_id) as well as entity references (e.g. fleet.player).
     *  References are deep and chainable e.g.
     *      [fleet.halite for player in board.players for fleet in player.fleets]
     *      fleet.player.shipyards[0].cell.north.east.fleet
     *  Consumers should not set or modify any attributes except and Shipyard.next_action
     */
    public Board(
        String rawObservation,
        String rawConfiguration,
        String rawNextActions
    ) {
        Observation observation = new Observation(rawObservation);
        // next_actions is effectively a Dict[[ShipyardId, ShipyardAction]]]
        // but that type's not very expressible so we simplify it to Dict[str, str]
        // Later we'll iterate through it once for each fleet and shipyard to pull all the actions out

        this.step = observation.step;
        this.remainingOverageTime = observation.remainingOverageTime;
        this.configuration = new Configuration(rawConfiguration);
        this.currentPlayerId = observation.player;
        this.players = new Player[observation.playerHlt.length];
        this.fleets = new HashMap<String, Fleet>();
        this.shipyards = new HashMap<String, Shipyard>();
        this.cells =  new Cell[observation.halite.length];
        this.size = this.configuration.size;

        // Create a cell for every point in a size x size grid
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                Point position = new Point(x, y);
                float halite = observation.halite[position.toIndex(size)];
                // We'll populate the cell's fleets and shipyards in _add_fleet and _add_shipyard
                this.cells[position.toIndex(size)] = new Cell(position, halite, "", "", this);
            }

        }

        for (int playerId = 0; playerId < observation.playerHlt.length; playerId++) {
            int playerHalite = observation.playerHlt[playerId];
            HashMap<String, int[]> playerShipyards = observation.playerShipyards.get(playerId);
            HashMap<String, String[]> playerFleets = observation.playerFleets.get(playerId);
            this.players[playerId] = new Player(playerId, playerHalite, new ArrayList<String>(), new ArrayList<String>(), this);
            //player_actions = next_actions[player_id] or {}

            for (Entry<String, String[]> entry : playerFleets.entrySet()) {
                String fleetId = entry.getKey();
                String[] fleetStrs = entry.getValue();

                int fleetPosIdx = Integer.parseInt(fleetStrs[0]);
                float fleetHalite = Float.parseFloat(fleetStrs[1]);
                int shipCount = Integer.parseInt(fleetStrs[2]);
                int directionIdx = Integer.parseInt(fleetStrs[3]);
                String flightPlan = fleetStrs[4];

                Point fleetPosition = Point.fromIndex(fleetPosIdx, this.size);
                Direction fleetDirection = Direction.fromIndex(directionIdx);
                this.addFleet(new Fleet(fleetId, shipCount, fleetDirection, fleetPosition, fleetHalite, flightPlan, playerId, this));
            }

            for (Entry<String, int[]> entry : playerShipyards.entrySet()) {
                String shipyardId = entry.getKey();
                int[] shipyardInts = entry.getValue();
                int shipyardPosIdx = shipyardInts[0];
                int shipCount = shipyardInts[1];
                int turnsControlled = shipyardInts[2];
                Point shipyardPosition = Point.fromIndex(shipyardPosIdx, this.size);
                Optional<ShipyardAction> action = Optional.empty();
                if (HaliteJson.containsKey(rawNextActions, shipyardId)) {
                    action = Optional.of(ShipyardAction.fromString(HaliteJson.getStrFromJson(rawNextActions, shipyardId)));
                }
                this.addShipyard(new Shipyard(shipyardId, shipCount, shipyardPosition, playerId, turnsControlled, this, action));
            }
        }
    }

    public Cell getCellAtPosition(Point position) {
        return this.cells[position.toIndex(this.size)];
    }

    public void addFleet(Fleet fleet) {
        fleet.player().fleetIds.add(fleet.id);
        fleet.cell().fleetId = fleet.id;
        this.fleets.put(fleet.id, fleet);
    }

    public void addShipyard(Shipyard shipyard) {
        shipyard.player().shipyardIds.add(shipyard.id);
        shipyard.cell().shipyardId = shipyard.id;
        shipyard.cell().halite = 0;
        this.shipyards.put(shipyard.id, shipyard);
    }

    public void deleteFleet(Fleet fleet) {
        fleet.player().fleetIds.remove(fleet.id);
        if (fleet.cell().fleetId == fleet.id) {
            fleet.cell().fleetId = "";
        }
        this.fleets.remove(fleet.id);
    }

    public void deleteShipyard(Shipyard shipyard) {
        shipyard.player().shipyardIds.remove(shipyard.id);
        if (shipyard.cell().shipyardId == shipyard.id) {
            shipyard.cell().shipyardId = "";
        }
        this.shipyards.remove(shipyard.id);
    }

    public Optional<Fleet> getFleetAtPoint(Point position) {
        return this.fleets.values().stream().filter(fleet -> fleet.position.equals(position)).findAny();
    }

    public Optional<Shipyard> getShipyardAtPoint(Point position) {
        return this.shipyards.values().stream().filter(shipyard -> shipyard.position.equals(position)).findAny();
    }

    /**
     * Returns the current player (generally this is you).
     * @return
     */
    public Player currentPlayer() {
        return this.players[this.currentPlayerId];
    }

    /**
     * Returns all players that aren't the current player.
     * You can get all opponent fleets with [fleet for fleet in player.fleets for player in board.opponents]
     */
    public Player[] opponents() {
        return Arrays.stream(this.players).filter(player -> player.id != this.currentPlayerId).toArray(Player[]::new);
    }


    public Board deepcopy() {


    }

}
