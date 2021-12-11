# Copyright 2021 Kaggle Inc
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from copy import deepcopy
from enum import Enum, auto
from functools import wraps
from kaggle_environments.helpers import Point, group_by
from typing import *
import sys
import math
import random
import kaggle_environments.helpers


# region Data Model Classes
class Observation(kaggle_environments.helpers.Observation):
    """
    Observation primarily used as a helper to construct the Board from the raw observation.
    This provides bindings for the observation type described at https://github.com/Kaggle/kaggle-environments/blob/master/kaggle_environments/envs/halite/halite.json
    """
    @property
    def halite(self) -> List[float]:
        """Serialized list of available halite per cell on the board."""
        return self["halite"]

    @property
    def players(self) -> List[List[int]]:
        """List of players and their assets."""
        return self["players"]

    @property
    def player(self) -> int:
        """The current agent's player index."""
        return self["player"]


class Configuration(kaggle_environments.helpers.Configuration):
    """
    Configuration provides access to tunable parameters in the environment.
    This provides bindings for the configuration type described at https://github.com/Kaggle/kaggle-environments/blob/master/kaggle_environments/envs/halite/halite.json
    """
    @property
    def agent_timeout(self) -> float:
        """Maximum runtime (seconds) to initialize an agent."""
        return self["agentTimeout"]

    @property
    def starting_halite(self) -> int:
        """The starting amount of halite available on the board."""
        return self["startingHalite"]

    @property
    def size(self) -> int:
        """The number of cells vertically and horizontally on the board."""
        return self["size"]

    @property
    def spawn_cost(self) -> int:
        """The amount of halite to spawn a new ship."""
        return self["spawnCost"]

    @property
    def convert_cost(self) -> int:
        """The amount of ships needed from a fleet to create a shipyard."""
        return self["convertCost"]

    @property
    def move_cost(self) -> float:
        """The percent deducted from fleet's current halite per move."""
        return self["moveCost"]

    @property
    def collect_rate(self) -> float:
        """The rate of halite collected by a fleet from a cell when moving through."""
        return self["collectRate"]

    @property
    def regen_rate(self) -> float:
        """The rate halite regenerates on the board."""
        return self["regenRate"]

    @property
    def max_cell_halite(self) -> int:
        """The maximum halite that can be in any cell."""
        return self["maxCellHalite"]

    @property
    def random_seed(self) -> int:
        """The seed to the random number generator (0 means no seed)."""
        return self["randomSeed"]

class Direction(Enum):
    NORTH = auto()
    EAST = auto()
    SOUTH = auto()
    WEST = auto()

    def to_point(self) -> Point:
        """
        This returns the position offset associated with a particular action 
        NORTH -> (0, 1)
        EAST -> (1, 0)
        SOUTH -> (0, -1)
        WEST -> (-1, 0)
        """
        return (
            Point(0, 1) if self == Direction.NORTH else
            Point(1, 0) if self == Direction.EAST else
            Point(0, -1) if self == Direction.SOUTH else
            Point(-1, 0) if self == Direction.WEST else
            None
        )

    def __str__(self) -> str:
        return self.name

    def to_index(self) -> int:
        return (
            0 if self == Direction.NORTH else
            1 if self == Direction.EAST else
            2 if self == Direction.SOUTH else
            3 if self == Direction.WEST else
            None
        )

    def to_char(self) -> str:
        return (
            "N" if self == Direction.NORTH else
            "E" if self == Direction.EAST else
            "S" if self == Direction.SOUTH else
            "W" if self == Direction.WEST else
            None
        )

    @staticmethod
    def from_str(str_dir: str):
        return  (
            Direction.NORTH if str_dir == "NORTH" else
            Direction.EAST if str_dir == "EAST" else
            Direction.SOUTH if str_dir == "SOUTH" else
            Direction.WEST if str_dir == "WEST" else
            None
        )

    @staticmethod
    def from_char(str_char: str):
        return  (
            Direction.NORTH if str_char == "N" else
            Direction.EAST if str_char == "E" else
            Direction.SOUTH if str_char == "S" else
            Direction.WEST if str_char == "W" else
            None
        )

    @staticmethod
    def from_index(idx: int):
        return (
            Direction.NORTH if idx == 0 else
            Direction.EAST if idx == 1 else
            Direction.SOUTH if idx == 2 else
            Direction.WEST if idx == 3 else
            None
        )

    @staticmethod
    def random_direction() -> 'Direction':
        rand = random.random()
        if rand <= .25:
            return Direction.NORTH
        elif rand <= .5:
            return Direction.EAST
        elif rand <= .75:
            return Direction.SOUTH
        else:
            return Direction.WEST

    @staticmethod
    def moves() -> List['Direction']:
        return [
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
        ]

class ShipyardActionType(Enum):
    SPAWN = auto()
    LAUNCH = auto()

    def __str__(self) -> str:
        return self.name

class ShipyardAction:

    def __init__(self, type: ShipyardActionType, num_ships: Optional[int], flight_plan: Optional[str]) -> None:
        self._type = type
        assert num_ships > 0, "must be a positive number"
        assert num_ships == int(num_ships), "must be an integer"
        self._num_ships = num_ships
        self._flight_plan = flight_plan

    def __str__(self) -> str:
        if self._type == ShipyardActionType.SPAWN:
            return f'{self._type.name}_{self._num_ships}'
        if self._type == ShipyardActionType.LAUNCH:
            return f'{self._type.name}_{self._num_ships}_{self._flight_plan}'
    
    @property
    def name(self):
        return str(self)

    @staticmethod
    def from_str(raw: str):
        if not raw:
            return None
        if raw.startswith(ShipyardActionType.SPAWN.name):
            return ShipyardAction.spawn_ships(int(raw.split("_")[1]))
        if raw.startswith(ShipyardActionType.LAUNCH.name):
            _, ship_str, plan_str = raw.split("_")
            num_ships = int(ship_str)
            return ShipyardAction.launch_ships_in_direction(num_ships, plan_str)

    @staticmethod
    def launch_ships_in_direction(number_ships: int, flight_plan: str):
        assert number_ships > 0, "must be a positive number_ships"
        assert number_ships == int(number_ships), "must be an integer number_ships"
        assert flight_plan is not None and len(flight_plan) > 0, "flight_plan must be a str of len > 0"
        return ShipyardAction(ShipyardActionType.LAUNCH, number_ships, flight_plan)

    @staticmethod
    def spawn_ships(number_ships: int):
        assert number_ships > 0, "must be a positive number_ships"
        assert number_ships == int(number_ships), "must be an integer number_ships"
        return ShipyardAction(ShipyardActionType.SPAWN, number_ships, None)

    @property
    def action_type(self) -> ShipyardActionType:
        return self._type
    
    @property
    def num_ships(self) -> Optional[int]:
        return self._num_ships

    @property
    def flight_plan(self) -> Optional[str]:
        return self._flight_plan


FleetId = NewType('FleetId', str)
ShipyardId = NewType('ShipyardId', str)
PlayerId = NewType('PlayerId', int)


class Cell:
    def __init__(self, position: Point, halite: float, shipyard_id: Optional[ShipyardId], fleet_id: Optional[FleetId], board: 'Board') -> None:
        self._position = position
        self._halite = halite
        self._shipyard_id = shipyard_id
        self._fleet_id = fleet_id
        self._board = board

    @property
    def position(self) -> Point:
        return self._position

    @property
    def halite(self) -> float:
        return self._halite

    @property
    def shipyard_id(self) -> Optional[ShipyardId]:
        return self._shipyard_id

    @property
    def fleet_id(self) -> Optional[FleetId]:
        return self._fleet_id

    @property
    def fleet(self) -> Optional['Fleet']:
        """Returns the fleet on this cell if it exists and None otherwise."""
        return self._board.fleets.get(self.fleet_id)

    @property
    def shipyard(self) -> Optional['Shipyard']:
        """Returns the shipyard on this cell if it exists and None otherwise."""
        return self._board.shipyards.get(self.shipyard_id)

    def neighbor(self, offset: Point) -> 'Cell':
        """Returns the cell at self.position + offset."""
        (x, y) = self.position + offset
        return self._board[x, y]

    @property
    def north(self) -> 'Cell':
        """Returns the cell north of this cell."""
        return self.neighbor(Direction.NORTH.to_point())

    @property
    def south(self) -> 'Cell':
        """Returns the cell south of this cell."""
        return self.neighbor(Direction.SOUTH.to_point())

    @property
    def east(self) -> 'Cell':
        """Returns the cell east of this cell."""
        return self.neighbor(Direction.EAST.to_point())

    @property
    def west(self) -> 'Cell':
        """Returns the cell west of this cell."""
        return self.neighbor(Direction.WEST.to_point())


class Fleet:
    def __init__(self, fleet_id: FleetId, ship_count: int, direction: Direction, position: Point, halite: int, flight_plan: str, player_id: PlayerId, board: 'Board') -> None:
        self._id = fleet_id
        self._ship_count = ship_count
        self._direction = direction
        self._position = position
        self._flight_plan = flight_plan
        self._halite = halite
        self._player_id = player_id
        self._board = board

    @property
    def id(self) -> FleetId:
        return self._id

    @property
    def ship_count(self) -> int:
        return self._ship_count

    @property
    def direction(self) -> Direction:
        return self._direction

    @property
    def position(self) -> Point:
        return self._position

    @property
    def halite(self) -> int:
        return self._halite

    @property
    def player_id(self) -> PlayerId:
        return self._player_id

    @property
    def cell(self) -> Cell:
        """Returns the cell this fleet is on."""
        return self._board[self.position]

    @property
    def player(self) -> 'Player':
        """Returns the player that owns this ship."""
        return self._board.players[self.player_id]

    @property
    def flight_plan(self) -> str:
        """Returns the current flight plan of the fleet"""
        return self._flight_plan

    @property
    def collection_rate(self) -> float:
        """ln(ship_count) / 10"""
        return math.log(self.ship_count) / 10

    @property
    def max_flight_plan_len(self) -> int:
        """Returns the length of the longest possible flight plan this fleet can be assigned"""
        return math.floor(2 * math.log(self.ship_count)) + 1

    @property
    def _observation(self) -> List[int]:
        """Converts a fleet back to the normalized observation subset that constructed it."""
        return [self.position.to_index(self._board.configuration.size), self.halite, self.ship_count, self.direction.to_index(), self.flight_plan]

    def less_than_other_allied_fleet(self, other):
        if not self.ship_count == other.ship_count:
            return self.ship_count < other.ship_count
        if not self.halite == other.halite:
            return self.halite < other.halite
        return self.direction.to_index() > other.direction.to_index()



class Shipyard:
    def __init__(self, shipyard_id: ShipyardId, ship_count: int, position: Point, player_id: PlayerId, turns_controlled: int, board: 'Board', next_action: Optional[ShipyardAction] = None) -> None:
        self._id = shipyard_id
        self._ship_count = ship_count
        self._position = position
        self._player_id = player_id
        self._turns_controlled = turns_controlled
        self._board = board
        self._next_action = next_action

    @property
    def id(self) -> ShipyardId:
        return self._id

    @property
    def ship_count(self):
        return self._ship_count

    @property
    def position(self) -> Point:
        return self._position

    @property
    def player_id(self) -> PlayerId:
        return self._player_id

    @property
    def max_spawn(self) -> int:
        spawn_values = (11, 25, 44, 70, 105, 151, 210, 284, 375)
        for idx, target in enumerate(spawn_values):
            if self._turns_controlled < target:
                return idx + 1
        return len(spawn_values) + 1

    @property
    def cell(self) -> Cell:
        """Returns the cell this shipyard is on."""
        return self._board[self.position]

    @property
    def player(self) -> 'Player':
        return self._board.players[self.player_id]

    @property
    def next_action(self) -> ShipyardAction:
        """Returns the action that will be executed by this shipyard when Board.next() is called (when the current turn ends)."""
        return self._next_action

    @next_action.setter
    def next_action(self, value: Optional[ShipyardAction]) -> None:
        """Sets the action that will be executed by this shipyard when Board.next() is called (when the current turn ends)."""
        self._next_action = value

    @property
    def _observation(self) -> List[int]:
        """Converts a shipyard back to the normalized observation subset that constructed it."""
        return [self.position.to_index(self._board.configuration.size), self.ship_count, self._turns_controlled]


class Player:
    def __init__(self, player_id: PlayerId, halite: int, shipyard_ids: List[ShipyardId], fleet_ids: List[FleetId], board: 'Board') -> None:
        self._id = player_id
        self._halite = halite
        self._shipyard_ids = shipyard_ids
        self._fleet_ids = fleet_ids
        self._board = board

    @property
    def id(self) -> PlayerId:
        return self._id

    @property
    def halite(self) -> int:
        return self._halite

    @property
    def shipyard_ids(self) -> List[ShipyardId]:
        return self._shipyard_ids

    @property
    def fleet_ids(self) -> List[FleetId]:
        return self._fleet_ids

    @property
    def shipyards(self) -> List[Shipyard]:
        """Returns all shipyards owned by this player."""
        return [
            self._board.shipyards[shipyard_id]
            for shipyard_id in self.shipyard_ids
        ]

    @property
    def fleets(self) -> List[Fleet]:
        """Returns all fleets owned by this player."""
        return [
            self._board.fleets[fleet_id]
            for fleet_id in self.fleet_ids
        ]

    @property
    def is_current_player(self) -> bool:
        """Returns whether this player is the current player (generally if this returns True, this player is you)."""
        return self.id == self._board.current_player_id

    @property
    def next_actions(self) -> Dict[str, str]:
        """Returns all queued fleet and shipyard actions for this player formatted for the halite interpreter to receive as an agent response."""
        shipyard_actions = {
            shipyard.id: shipyard.next_action.name
            for shipyard in self.shipyards
            if shipyard.next_action is not None
        }
        return {**shipyard_actions}

    @property
    def _observation(self):
        """Converts a player back to the normalized observation subset that constructed it."""
        shipyards = {shipyard.id: shipyard._observation for shipyard in self.shipyards}
        fleets = {fleet.id: fleet._observation for fleet in self.fleets}
        return [self.halite, shipyards, fleets]
# endregion


class Board:
    def __init__(
        self,
        raw_observation: Dict[str, Any],
        raw_configuration: Union[Configuration, Dict[str, Any]],
        next_actions: Optional[List[Dict[str, str]]] = None
    ) -> None:
        """
        Creates a board from the provided observation, configuration, and next_actions as specified by
        https://github.com/Kaggle/kaggle-environments/blob/master/kaggle_environments/envs/halite/halite.json
        Board tracks players (by id), fleets (by id), shipyards (by id), and cells (by position).
        Each entity contains both key values (e.g. fleet.player_id) as well as entity references (e.g. fleet.player).
        References are deep and chainable e.g.
            [fleet.halite for player in board.players for fleet in player.fleets]
            fleet.player.shipyards[0].cell.north.east.fleet
        Consumers should not set or modify any attributes except and Shipyard.next_action
        """
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

    @property
    def configuration(self) -> Configuration:
        return self._configuration

    @property
    def players(self) -> Dict[PlayerId, Player]:
        return self._players

    @property
    def fleets(self) -> Dict[FleetId, Fleet]:
        """Returns all fleets on the current board."""
        return self._fleets

    @property
    def shipyards(self) -> Dict[ShipyardId, Shipyard]:
        """Returns all shipyards on the current board."""
        return self._shipyards

    @property
    def cells(self) -> Dict[Point, Cell]:
        """Returns all cells on the current board."""
        return self._cells

    @property
    def step(self) -> int:
        return self._step

    @property
    def current_player_id(self) -> PlayerId:
        return self._current_player_id

    @property
    def current_player(self) -> Player:
        """Returns the current player (generally this is you)."""
        return self._players[self.current_player_id]

    @property
    def opponents(self) -> List[Player]:
        """
        Returns all players that aren't the current player.
        You can get all opponent fleets with [fleet for fleet in player.fleets for player in board.opponents]
        """
        return [player for player in self.players.values() if not player.is_current_player]

    @property
    def observation(self) -> Dict[str, Any]:
        """Converts a Board back to the normalized observation that constructed it."""
        size = self.configuration.size
        halite = [self[Point.from_index(index, size)].halite for index in range(size * size)]
        players = [player._observation for player in self.players.values()]

        return {
            "halite": halite,
            "players": players,
            "player": self.current_player_id,
            "step": self.step,
            "remainingOverageTime": self._remaining_overage_time,
        }

    def __deepcopy__(self, _) -> 'Board':
        actions = [player.next_actions for player in self.players.values()]
        return Board(self.observation, self.configuration, actions)

    def __getitem__(self, point: Union[Tuple[int, int], Point]) -> Cell:
        """
        This method will wrap the supplied position to fit within the board size and return the cell at that location.
        e.g. on a 3x3 board, board[2, 1] is the same as board[5, 1]
        """
        if not isinstance(point, Point):
            (x, y) = point
            point = Point(x, y)
        return self._cells[point % self.configuration.size]

    def __str__(self) -> str:
        """
        The board is printed in a grid with the following rules:
        Capital letters are shipyards
        Lower case letters are fleets
        Digits are cell halite and scale from 0-9 directly proportional to a value between 0 and self.configuration.max_cell_halite
        Player 1 is letter a/A
        Player 2 is letter b/B
        etc.
        """
        size = self.configuration.size
        result = ''
        for y in range(size):
            for x in range(size):
                cell = self[(x, size - y - 1)]
                result += '|'
                result += (
                    chr(ord('a') + cell.ship.player_id)
                    if cell.fleet is not None
                    else ' '
                )
                # This normalizes a value from 0 to max_cell halite to a value from 0 to 9
                normalized_halite = int(9.0 * cell.halite / float(self.configuration.max_cell_halite))
                result += str(normalized_halite)
                result += (
                    chr(ord('A') + cell.shipyard.player_id)
                    if cell.shipyard is not None
                    else ' '
                )
            result += '|\n'
        return result

    def _add_fleet(self: 'Board', fleet: Fleet) -> None:
        fleet.player.fleet_ids.append(fleet.id)
        fleet.cell._fleet_id = fleet.id
        self._fleets[fleet.id] = fleet

    def _add_shipyard(self: 'Board', shipyard: Shipyard) -> None:
        shipyard.player.shipyard_ids.append(shipyard.id)
        shipyard.cell._shipyard_id = shipyard.id
        shipyard.cell._halite = 0
        self._shipyards[shipyard.id] = shipyard

    def _delete_fleet(self: 'Board', fleet: Fleet) -> None:
        fleet.player.fleet_ids.remove(fleet.id)
        if fleet.cell.fleet_id == fleet.id:
            fleet.cell._fleet_id = None
        del self._fleets[fleet.id]

    def _delete_shipyard(self: 'Board', shipyard: Shipyard) -> None:
        shipyard.player.shipyard_ids.remove(shipyard.id)
        if shipyard.cell.shipyard_id == shipyard.id:
            shipyard.cell._shipyard_id = None
        del self._shipyards[shipyard.id]


    def print(self: 'Board') -> None:
        size = self.configuration.size
        player_chars = {
            pid: alpha
            for pid, alpha in  zip(self.players, "abcdef"[:len(self.players)])
        }
        print(self.configuration.size * "=")
        for i in range(size):
            row = ""
            for j in range(size):
                pos = Point(j, size - 1 - i)
                curr_cell = self.cells[pos]
                if curr_cell.shipyard is not None:
                    row += player_chars[curr_cell.shipyard.player_id].upper()
                elif curr_cell.fleet is not None:
                    row += player_chars[curr_cell.fleet.player_id]
                elif curr_cell.halite <= 50:
                    row += " "
                elif curr_cell.halite <= 250:
                    row += "."
                elif curr_cell.halite <= 400:
                    row += "*"
                elif curr_cell.halite > 400:
                    row += "o"
            print(row)
        print(self.configuration.size * "=")


    def next(self) -> 'Board':
        """
        Returns a new board with the current board's next actions applied.
        The current board is unmodified.
        This can form a halite interpreter, e.g.
            next_observation = Board(current_observation, configuration, actions).next().observation
        """
        # Create a copy of the board to modify so we don't affect the current board
        board = deepcopy(self)
        configuration = board.configuration
        convert_cost = configuration.convert_cost
        spawn_cost = configuration.spawn_cost
        uid_counter = 0

        # This is a consistent way to generate unique strings to form fleet and shipyard ids
        def create_uid():
            nonlocal uid_counter
            uid_counter += 1
            return f"{self.step + 1}-{uid_counter}"

        # Process actions and store the results in the fleets and shipyards lists for collision checking
        for player in board.players.values():
            for shipyard in player.shipyards:
                if shipyard.next_action == None:
                    pass
                elif (shipyard.next_action.action_type == ShipyardActionType.SPAWN 
                        and player.halite >= spawn_cost * shipyard.next_action.num_ships 
                        and shipyard.next_action.num_ships <= shipyard.max_spawn):
                    # Handle SPAWN actions
                    player._halite -= spawn_cost * shipyard.next_action.num_ships
                    shipyard._ship_count += shipyard.next_action.num_ships
                elif shipyard.next_action.action_type == ShipyardActionType.LAUNCH and shipyard.ship_count >= shipyard.next_action.num_ships:
                    shipyard._ship_count -= shipyard.next_action.num_ships
                    direction = Direction.from_char(shipyard.next_action.flight_plan[0])
                    board._add_fleet(Fleet(FleetId(create_uid()), shipyard.next_action.num_ships, direction, shipyard.position, 0, shipyard.next_action.flight_plan, player.id, board))
                
                # Clear the shipyard's action so it doesn't repeat the same action automatically
                shipyard.next_action = None
                shipyard._turns_controlled += 1

            def find_first_non_digit(candiate_str):
                for i in range(len(candiate_str)):
                    if not candiate_str[i].isdigit():
                        return i
                return 0

            player_fleet_moves = DefaultDict(list)
            for fleet in player.fleets:
                if fleet.flight_plan and fleet.flight_plan[0] == "C" and fleet.ship_count >= convert_cost and fleet.cell.shipyard_id is None:
                    player._halite += fleet.halite
                    board._add_shipyard(Shipyard(ShipyardId(create_uid()), fleet.ship_count - convert_cost, fleet.position, player.id, 0, board))
                    board._delete_fleet(fleet)
                    continue

                if fleet.flight_plan and fleet.flight_plan[0].isalpha():
                    fleet._direction = Direction.from_char(fleet.flight_plan[0])
                    fleet._flight_plan = fleet.flight_plan[1:]
                elif fleet.flight_plan:
                    idx = find_first_non_digit(fleet.flight_plan)
                    digits = int(fleet.flight_plan[:idx])
                    rest = fleet.flight_plan[idx:]
                    digits -= 1
                    if digits > 0:
                        fleet._flight_plan = str(digits) + rest
                    else:
                        fleet._flight_plan = rest

                # see if any allied fleets are 'passing eachother in the night'
                dest_idx = fleet.position.translate(fleet.direction.to_point(), configuration.size).to_index(configuration.size)
                source_idx = fleet.position.to_index(configuration.size)
                move_key = (dest_idx, source_idx) if dest_idx < source_idx else (source_idx, dest_idx)
                player_fleet_moves[move_key].append(fleet.id)
                # continue moving in the fleet's direction
                fleet.cell._fleet_id = None
                fleet._position = fleet.position.translate(fleet.direction.to_point(), configuration.size)
                fleet._halite *= (1 - board.configuration.move_cost)
                # We don't set the new cell's fleet_id here as it would be overwritten by another fleet in the case of collision.

            def combine_fleets(fid1: FleetId, fid2: FleetId) -> FleetId:
                f1 = board.fleets[fid1]
                f2 = board.fleets[fid2]
                if f1.less_than_other_allied_fleet(f2):
                    f1, f2 = f2, f1
                    fid1, fid2 = fid2, fid1
                f1._halite += f2.halite
                f1._ship_count += f2._ship_count
                board._delete_fleet(f2)
                return fid1

            # resolve any fleets are 'passing eachother in the night'
            for value in player_fleet_moves.values():
                if len(value) == 2:
                    combine_fleets(value[0], value[1])
                    
            # resolve any allied fleets that ended up in the same square
            fleets_by_loc = group_by(player.fleets, lambda fleet: fleet.position.to_index(configuration.size))
            for value in fleets_by_loc.values():
                fid = value[0].id
                for i in range (1, len(value)):
                    fid = combine_fleets(fid, value[i].id)

            # Lets just check and make sure.
            assert player.halite >= 0

        def resolve_collision(fleets: List[Fleet]) -> Tuple[Optional[Fleet], List[Fleet]]:
            """
            Accepts the list of fleets at a particular position (must not be empty).
            Returns the fleet with the most ships or None in the case of a tie along with all other fleets.
            """
            if len(fleets) == 1:
                return fleets[0], []
            fleets_by_ships = group_by(fleets, lambda fleet: fleet.ship_count)
            most_ships = max(fleets_by_ships.keys())
            largest_fleets = fleets_by_ships[most_ships]
            if len(largest_fleets) == 1:
                # There was a winner, return it
                winner = largest_fleets[0]
                return winner, [fleet for fleet in fleets if fleet != winner]
            # There was a tie for most ships, all are deleted
            return None, fleets

        # Check for fleet to fleet collisions
        fleet_collision_groups = group_by(board.fleets.values(), lambda fleet: fleet.position)
        for position, collided_fleets in fleet_collision_groups.items():
            winner, deleted = resolve_collision(collided_fleets)
            shipyard = group_by(board.shipyards.values(), lambda shipyard: shipyard.position).get(position)
            if winner is not None:
                winner.cell._fleet_id = winner.id
                max_enemy_size = max([fleet.ship_count for fleet in deleted]) if deleted else 0
                winner._ship_count -= max_enemy_size
            for fleet in deleted:
                board._delete_fleet(fleet)
                if winner is not None:
                    # Winner takes deleted fleets' halite
                    winner._halite += fleet.halite
                elif winner is None and shipyard and shipyard[0].player:
                    # Desposit the halite into the shipyard
                    player._halite += fleet.halite
                elif winner is None:
                    # Desposit the halite on the square
                    board.cells[position]._halite += fleet.halite


        # Check for fleet to shipyard collisions
        for shipyard in list(board.shipyards.values()):
            fleet = shipyard.cell.fleet
            if fleet is not None and fleet.player_id != shipyard.player_id:
                if fleet.ship_count > shipyard.ship_count:
                    count = fleet.ship_count - shipyard.ship_count
                    board._delete_shipyard(shipyard)
                    board._add_shipyard(Shipyard(ShipyardId(create_uid()), count, shipyard.position, fleet.player.id, 1, board))
                    fleet.player._halite += fleet.halite
                    board._delete_fleet(fleet)
                else:
                    shipyard._ship_count -= fleet.ship_count
                    shipyard.player._halite += fleet.halite
                    board._delete_fleet(fleet)

        # Deposit halite from fleets into shipyards
        for shipyard in list(board.shipyards.values()):
            fleet = shipyard.cell.fleet
            if fleet is not None and fleet.player_id == shipyard.player_id:
                shipyard.player._halite += fleet.halite
                shipyard._ship_count += fleet.ship_count
                board._delete_fleet(fleet)

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
                next_halite = round(cell.halite * (1 + configuration.regen_rate), 3)
                cell._halite = min(next_halite, configuration.max_cell_halite)
                # Lets just check and make sure.

        board._step += 1

        self.print()

        return board


def board_agent(agent: Callable[[Board], None]):
    """
    Decorator used to create an agent that modifies a board rather than an observation and a configuration
    Automatically returns the modified board's next actions

    @board_agent
    def my_agent(board: Board) -> None:
        ...
    """
    @wraps(agent)
    def agent_wrapper(obs, config) -> Dict[str, str]:
        board = Board(obs, config)
        agent(board)
        return board.current_player.next_actions

    if agent.__module__ is not None and agent.__module__ in sys.modules:
        setattr(sys.modules[agent.__module__], agent.__name__, agent_wrapper)
    return agent_wrapper
