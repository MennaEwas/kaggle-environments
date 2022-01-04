package halite;

import java.util.HashMap;

public class Board {

    public final HashMap<String, Shipyard> shipyards;
    public final HashMap<String, Fleet> fleets;
    public final Player[] players;
    public final String currentPlayerId;
    public final Configuration configuration;
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
        HashMap<String, Object> rawObservation,
        HashMap<String, Object> rawConfiguration,
        Optional<HapMap<String, String>[]> nextActions
    ) {
        this.shipyards = null;
        this.fleets = null;
        this.players = null;
        this.currentPlayerId = "";
        this.configuration = null;

        observation = Observation(raw_observation)
        # next_actions is effectively a Dict[Union[[FleetId, FleetAction], [ShipyardId, ShipyardAction]]]
        # but that type's not very expressible so we simplify it to Dict[str, str]
        # Later we'll iterate through it once for each fleet and shipyard to pull all the actions out
        next_actions = next_actions or ([{}] * len(observation.players))

        self._step = observation.step
        self._remaining_overage_time = observation.remaining_overage_time
        self._configuration = Configuration(raw_configuration)
        self._current_player_id = observation.player
        self._players: Dict[PlayerId, Player] = {}
        self._fleets: Dict[FleetId, Fleet] = {}
        self._shipyards: Dict[ShipyardId, Shipyard] = {}
        self._cells: Dict[Point, Cell] = {}

        size = self.configuration.size
        # Create a cell for every point in a size x size grid
        for x in range(size):
            for y in range(size):
                position = Point(x, y)
                halite = observation.halite[position.to_index(size)]
                # We'll populate the cell's fleets and shipyards in _add_fleet and _add_shipyard
                self.cells[position] = Cell(position, halite, None, None, self)

        for (player_id, player_observation) in enumerate(observation.players):
            # We know the len(player_observation) == 3 based on the schema -- this is a hack to have a tuple in json
            [player_halite, player_shipyards, player_fleets] = player_observation
            # We'll populate the player's fleets and shipyards in _add_fleet and _add_shipyard
            self.players[player_id] = Player(player_id, player_halite, [], [], self)
            player_actions = next_actions[player_id] or {}

            for (fleet_id, [fleet_index, fleet_halite, ship_count, direction, flight_plan]) in player_fleets.items():
                # In the raw observation, halite is stored as a 1d list but we convert it to a 2d dict for convenience
                # Accordingly we also need to convert our list indices to dict keys / 2d positions
                fleet_position = Point.from_index(fleet_index, size)
                fleet_direction = Direction.from_index(direction)
                self._add_fleet(Fleet(fleet_id, ship_count, fleet_direction, fleet_position, fleet_halite, flight_plan, player_id, self))

            for (shipyard_id, [shipyard_index, ship_count, turns_controlled]) in player_shipyards.items():
                shipyard_position = Point.from_index(shipyard_index, size)
                raw_action = player_actions.get(shipyard_id)
                action = ShipyardAction.from_str(raw_action)
                self._add_shipyard(Shipyard(shipyard_id, ship_count, shipyard_position, player_id, turns_controlled, self, action))
    }

    public Cell getCellAtPosition(Point position) {
        return null;
    }

    public int size() {
        return 0;
    }

}
