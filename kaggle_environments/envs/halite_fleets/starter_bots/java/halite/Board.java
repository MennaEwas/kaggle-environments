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

    private int uidCounter;

    private Board(HashMap<String, Shipyard> shipyards, HashMap<String, Fleet> fleets, Player[] players, int currentPlayerId, Configuration configuration, int step, float remainingOverageTime, Cell[] cells, int size) {
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

    public Board cloneBoard() {
        return new Board(this.shipyards, this.fleets, this.players, this.currentPlayerId, this.configuration, this.step, this.remainingOverageTime, this.cells, this.size);
    }

    /**
     * Creates a board from the provided observation, configuration, and nextActions as specified by
     *  https://github.com/Kaggle/kaggle-environments/blob/master/kaggle_environments/envs/halite/halite.json
     *  Board tracks players (by id), fleets (by id), shipyards (by id), and cells (by position).
     *  Each entity contains both key values (e.g. fleet.player_id) as well as entity references (e.g. fleet.player).
     *  References are deep and chainable e.g.
     *      [fleet.halite for player in board.players for fleet in player.fleets]
     *      fleet.player.shipyards[0].cell.north.east.fleet
     *  Consumers should not set or modify any attributes except and Shipyard.nextAction
     */
    public Board(
        String rawObservation,
        String rawConfiguration,
        String rawNextActions
    ) {
        Observation observation = new Observation(rawObservation);
        // nextActions is effectively a Dict[[ShipyardId, ShipyardAction]]]
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
                // We'll populate the cell's fleets and shipyards in _add_fleet and addShipyard
                this.cells[position.toIndex(size)] = new Cell(position, halite, "", "", this);
            }

        }

        for (int playerId = 0; playerId < observation.playerHlt.length; playerId++) {
            int playerHalite = observation.playerHlt[playerId];
            HashMap<String, int[]> playerShipyards = observation.playerShipyards.get(playerId);
            HashMap<String, String[]> playerFleets = observation.playerFleets.get(playerId);
            this.players[playerId] = new Player(playerId, playerHalite, new ArrayList<String>(), new ArrayList<String>(), this);
            //player_actions = nextActions[player_id] or {}

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

    private String createUid() {
        Sting uid = String.format("%d-%d", this.step + 1, this.uidCounter);
        this.counter += 1;
        return uid;
    }

    private boolean isValidFlightPlan(Stirng flightPlan) {
        String allowed = "NESWC0123456789";
        for (int i = 0; i < allowed.length(); i++) {
            Char c = allowed.charAt(i)
            flightPlan = flightPlan.replace(c, "");
        }
        return flightPlan.length() == 0;
    }

    private findFirstNonDigit(String candidateStr) {
        if (candidateStr.length() == 0) return 0;
        for (int i = 0; i < candidateStr.length(); i++) {
            if (!Character.isDigit(candidateStr.charAt(i))) {
                return i;
            }
        }
        return candidateStr.length() + 1;
    }

    private String combineFleets(Board board, String fid1, String fid2) {
        Fleet f1 = board.fleets.get(fid1);
        Fleet f2 = board.fleets.get(fid2);
        if (f1.lessThanOtherAlliedFleet(f2)) {
            Fleet temp = f1;
            f1 = f2;
            f2 = temp;
            String temp = fid1;
            fid1 = fid2;
            fid2 = temp;
        }
        f1.halite += f2.halite;
        f1.shipCount += f2.shipCount;
        board.deleteFleet(f2);
        return fid1;
    }

    /**
     * Accepts the list of fleets at a particular position (must not be empty).
     * Returns the fleet with the most ships or None in the case of a tie along with all other fleets.
     */
    public resolve_collision(List<Fleet> fleets) {
        if (fleets.length == 1) {
            return fleets[0], []
        }
        fleets_by_ships = group_by(fleets, lambda fleet: fleet.shipCount)
        most_ships = max(fleets_by_ships.keys())
        largest_fleets = fleets_by_ships[most_ships]
        if len(largest_fleets) == 1:
            // There was a winner, return it
            winner = largest_fleets[0]
            return winner, [fleet for fleet in fleets if fleet != winner]
        // There was a tie for most ships, all are deleted
        return None, fleets
    }


    /**
     * Returns a new board with the current board's next actions applied.
     * The current board is unmodified.
     * This can form a halite interpreter, e.g.
     *     next_observation = Board(current_observation, configuration, actions).next().observation
     */
    public Board next() {
        // Create a copy of the board to modify so we don't affect the current board
        Board board = this.cloneBoard();
        Configuration configuration = board.configuration;
        int converstCost = configuration.convertCost;
        int spawnCost = configuration.spawnCost;
        this.uidCounter = 0;


        // Process actions and store the results in the fleets and shipyards lists for collision checking
        for (Player player : board.players) {
            for (Shipyard shipyard : player.shipyards) {
                if (shipyard.nextAction.isEmpty()) {
                    continue;
                }
                ShipyardAction nextAction = shipyard.nextAction.get();

                if  (shipyard.nextAction.shipCount == 0) {
                    shipyard.nextAction = Optional.empty();
                    continue;
                }

                if (nextAction.actionType == ShipyardActionType.SPAWN 
                        && player.halite >= spawnCost * shipyard.nextAction.numShips 
                        && shipyard.nextAction.numShips <= shipyard.maxSpawn()) {}
                    player.halite -= spawnCost * shipyard.nextAction.numShips
                    shipyard.shipCount += shipyard.nextAction.numShips
                } else if (shipyard.nextAction.actionType == ShipyardActionType.LAUNCH and shipyard.shipCount >= shipyard.nextAction.numShips) {
                    String flightPlan = shipyard.nextAction.flightPlan;
                    if (!flightPlan or !isValidFlightPlan(flight_plan)) {
                        shipyard.nextAction = Optional.empty();
                        continue;
                    }
                    shipyard.shipCount -= shipyard.nextAction.numShips;
                    Direction direction = Direction.fromChar(flightPlan.substring(0, 1));
                    int maxFlightPlanLen = Fleet.maxFlightPlanLenForShipCount(shipyard.nextAction.numShips);
                    if (flightPlan.length() > maxFlightPlanLen) {
                        flightPlan = flightPlan.substring(0, maxFlightPlanLen;
                    }
                    board.addFleet(Fleet(FleetId(this.createUid()), shipyard.nextAction.numShips, direction, shipyard.position, 0, shipyard.nextAction.flightPlan, player.id, board))
                    uidCounter += 1;
                }
                
                shipyard.nextAction = Optional.empty();
                shipyard.turnsControlled += 1;
            }

            
            for (Fleet fleet : player.fleets) {
                // remove any errant 0s
                while (fleet.flightPlan.length() > 0 && fleet.flightPlan.startswith("0") ) {
                    fleet.flightPlan = fleet.flightPlan.substring(1);
                }
                if (fleet.flightPlan.length() > 0 && fleet.flightPlan.startsWith("C") && fleet.shipCount >= converstCost && fleet.cell.shipyardId.length() == 0) {
                    player.halite += fleet.halite;
                    fleet.cell().halite = 0;
                    board.addShipyard(Shipyard(ShipyardId(create_uid()), fleet.shipCount - converstCost, fleet.position, player.id, 0, board));
                    board.deleteFleet(fleet);
                    continue;
                } else if (fleet.flightPlan.length() > 0 && fleet.flightPlan.startsWith("C")) {
                    // couldn't build, remove the Convert and continue with flight plan
                    fleet.flightPlan = fleet.flightPlan.substring(1);
                }

                if (fleet.flightPlan.length() > 0 && fleet.flight_plan[0].isalpha()) {
                    fleet.direction = Direction.fromChar(fleet.flightPlan.substring(0, 1));
                    fleet.flightPlan = fleet.flightPlan.substring(1);
                } else if (fleet.flightPlan.length() > 0) {
                    idx = find_first_non_digit(fleet.flightPlan)
                    digits = int(fleet.flightPlan[:idx])
                    rest = fleet.flightPlan[idx:]
                    digits -= 1
                    if digits > 0:
                        fleet._flightPlan = str(digits) + rest
                    else:
                        fleet._flightPlan = rest
                }

                // continue moving in the fleet's direction
                fleet.cell().fleetId = "";
                fleet.position = fleet.position.translate(fleet.direction.toPoint(), configuration.size);
                // We don't set the new cell's fleet_id here as it would be overwritten by another fleet in the case of collision.
            }

            fleets_by_loc = group_by(player.fleets, lambda fleet: fleet.position.to_index(configuration.size))
            for value in fleets_by_loc.values():
                fid = value[0].id
                for i in range (1, len(value)):
                    fid = combine_fleets(fid, value[i].id)

            # Lets just check and make sure.
            assert player.halite >= 0
        }

        
        // Check for fleet to fleet collisions
        fleet_collision_groups = group_by(board.fleets.values(), lambda fleet: fleet.position)
        for position, collided_fleets in fleet_collision_groups.items():
            winner, deleted = resolve_collision(collided_fleets)
            shipyard = group_by(board.shipyards.values(), lambda shipyard: shipyard.position).get(position)
            if winner is not None:
                winner.cell._fleet_id = winner.id
                max_enemy_size = max([fleet.shipCount for fleet in deleted]) if deleted else 0
                winner._shipCount -= max_enemy_size
            for fleet in deleted:
                board.deleteFleet(fleet)
                if winner is not None:
                    // Winner takes deleted fleets' halite
                    winner._halite += fleet.halite
                elif winner is None and shipyard and shipyard[0].player:
                    // Desposit the halite into the shipyard
                    player._halite += fleet.halite
                elif winner is None:
                    // Desposit the halite on the square
                    board.cells[position]._halite += fleet.halite


        // Check for fleet to shipyard collisions
        for (Shipyard shipyard : board.shipyards.values()) {
            Optional<Fleet> optFleet = shipyard.cell().fleet;
            if (!optFleet.isEmpty() && optFleet.get().playerId != shipyard.playerId) {
                Fleet = optFleet.get();
                if (fleet.shipCount > shipyard.shipCount) {
                    count = fleet.shipCount - shipyard.shipCount;
                    board.deleteShipyard(shipyard);
                    board.addShipyard(Shipyard(ShipyardId(create_uid()), count, shipyard.position, fleet.player.id, 1, board));
                    fleet.player.halite += fleet.halite;
                    board.deleteFleet(fleet);
                } else {
                    shipyard.shipCount -= fleet.shipCount;
                    shipyard.player().halite += fleet.halite;
                    board.deleteFleet(fleet);
                }
            }
        }

        // Deposit halite from fleets into shipyards
        for (Shipyard shipyard : board.shipyards.values()) {
            Optional<Fleet> optFleet = shipyard.cell().fleet;
            if (!optFleet.empty() && optFleet.get().player_id == shipyard.player_id) {
                Fleet fleet = optFleet.get();
                shipyard.player().halite += fleet.halite;
                shipyard.shipCount += fleet.shipCount;
                board.deleteFleet(fleet);
            }
        }

        // apply fleet to fleet damage on all orthagonally adjacent cells
        incoming_dmg = DefaultDict(int)
        for fleet in board.fleets.values():
            for direction in Direction.list_directions():
                curr_pos = fleet.position.translate(direction.to_point(), board.configuration.size)
                fleet_at_pos = board.get_fleet_at_point(curr_pos)
                if fleet_at_pos and not fleet_at_pos.player_id == fleet.player_id:
                    incoming_dmg[fleet_at_pos.id] += fleet.shipCount

        for fleet_id, damage in incoming_dmg.items():
            fleet = board.fleets[fleet_id]
            if damage >= fleet.shipCount:
                fleet.cell._halite += fleet.halite
                board.deleteFleet(fleet)
            else:
                fleet._shipCount -= damage

        # Collect halite from cells into fleets
        for fleet in board.fleets.values():
            cell = fleet.cell
            delta_halite = int(cell.halite * min(fleet.collection_rate, 99))
            if delta_halite > 0:
                fleet._halite += delta_halite
                cell._halite -= delta_halite

        # Regenerate halite in cells
        for cell in board.cells.values():
            if cell.fleet_id is None and cell.shipyard_id is None:
                if cell.halite < configuration.max_cell_halite:
                    next_halite = round(cell.halite * (1 + configuration.regen_rate), 3)
                    cell._halite = next_halite

        board._step += 1

        # self.print()

        return board

    }

}
