# Copyright 2020 Kaggle Inc
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

import copy
import json
import math
import numpy as np
from os import path
from random import choice, randint, randrange, sample, seed
from .helpers import board_agent, Board, ShipyardAction, Direction
from kaggle_environments import utils


def get_col_row(size, pos):
    return pos % size, pos // size


def get_to_pos(size, pos, direction):
    col, row = get_col_row(size, pos)
    if direction == "NORTH":
        return pos - size if pos >= size else size ** 2 - size + col
    elif direction == "SOUTH":
        return col if pos + size >= size ** 2 else pos + size
    elif direction == "EAST":
        return pos + 1 if col < size - 1 else row * size
    elif direction == "WEST":
        return pos - 1 if col > 0 else (row + 1) * size - 1


@board_agent
def random_agent(board):
    me = board.current_player
    remaining_halite = me.halite
    shipyards = me.shipyards
    # randomize shipyard order
    shipyards = sample(shipyards, len(shipyards))
    for shipyard in shipyards:
        # 20% chance to launch a fleet
        if randint(0, 4) == 0 and shipyard.ship_count > 10:
            dir_str = Direction.random_direction().to_char()
            dir2_str = Direction.random_direction().to_char()
            flight_plan = dir_str + str(randint(1, 10)) + dir2_str
            shipyard.next_action = ShipyardAction.launch_ships_in_direction(shipyard.ship_count, flight_plan)
        # else spawn if possible
        elif remaining_halite > board.configuration.spawn_cost * shipyard.max_spawn:
            remaining_halite -= board.configuration.spawn_cost
            shipyard.next_action = ShipyardAction.spawn_ships(shipyard.max_spawn)
        
agents = {"random": random_agent}


def populate_board(state, env):
    obs = state[0].observation
    config = env.configuration
    size = env.configuration.size
    uid_counter = 0

    # Set seed for random number generators
    if not hasattr(config, "randomSeed"):
        max_int_32 = (1 << 31) - 1
        config.randomSeed = randrange(max_int_32)

    np.random.seed(config.randomSeed)
    seed(config.randomSeed)

    # This is a consistent way to generate unique strings to form ship and shipyard ids
    def create_uid():
        nonlocal uid_counter
        uid_counter += 1
        return f"{obs.step}-{uid_counter}"

    # Distribute Halite evenly into quartiles.
    half = math.ceil(size / 2)
    grid = [[0] * half for _ in range(half)]

    # Randomly place a few halite "seeds".
    for i in range(half):
        # random distribution across entire quartile
        grid[randint(0, half - 1)][randint(0, half - 1)] = i ** 2

        # as well as a particular distribution weighted toward the center of the map
        grid[randint(half // 2, half - 1)][randint(half // 2, half - 1)] = i ** 2

    # Spread the seeds radially.
    radius_grid = copy.deepcopy(grid)
    for r in range(half):
        for c in range(half):
            value = grid[r][c]
            if value == 0:
                continue

            # keep initial seed values, but constrain radius of clusters
            radius = min(round((value / half) ** 0.5), 1)
            for r2 in range(r - radius + 1, r + radius):
                for c2 in range(c - radius + 1, c + radius):
                    if 0 <= r2 < half and 0 <= c2 < half:
                        distance = (abs(r2 - r) ** 2 + abs(c2 - c) ** 2) ** 0.5
                        radius_grid[r2][c2] += int(value / max(1, distance) ** distance)

    # add some random sprouts of halite
    radius_grid = np.asarray(radius_grid)
    add_grid = np.random.gumbel(0, 300.0, size=(half, half)).astype(int)
    sparse_radius_grid = np.random.binomial(1, 0.5, size=(half, half))
    add_grid = np.clip(add_grid, 0, a_max=None) * sparse_radius_grid
    radius_grid += add_grid

    # add another set of random locations to the center corner
    corner_grid = np.random.gumbel(0, 500.0, size=(half // 4, half // 4)).astype(int)
    corner_grid = np.clip(corner_grid, 0, a_max=None)
    radius_grid[half - (half // 4):, half - (half // 4):] += corner_grid

    # make it assomptocially symmetric
    for i in range(half):
        for j in range(half):
            if i + j < half:
                radius_grid[i][j] = radius_grid[j][i]

    # Normalize the available halite against the defined configuration starting halite.
    total = sum([sum(row) for row in radius_grid])
    obs.halite = [0] * (size ** 2)
    for r, row in enumerate(radius_grid):
        for c, val in enumerate(row):
            val = int(val * config.startingHalite / total / 4)
            obs.halite[size * r + c] = val
            obs.halite[size * r + (size - c - 1)] = val
            obs.halite[size * (size - 1) - (size * r) + c] = val
            obs.halite[size * (size - 1) - (size * r) + (size - c - 1)] = val

    # Distribute the starting shipyards evenly.
    num_agents = len(state)
    starting_positions = [0] * num_agents
    if num_agents == 1:
        starting_positions[0] = size * (size // 2) + size // 2
    elif num_agents == 2:
        starting_positions[0] = size * (size // 2 - 1) + size // 4
        starting_positions[1] = size * (size // 2 + 1) + math.ceil(3 * size / 4) - 1
        #starting_positions[0] = size * (size // 2) + size // 4
        #starting_positions[1] = size * (size // 2) + math.ceil(3 * size / 4)
    elif num_agents == 4:
        starting_positions[0] = size * (size // 4 + 1) + size // 4 - 1
        starting_positions[1] = size * (size // 4 - 1) + 3 * size // 4 - 1
        starting_positions[2] = size * (3 * size // 4 + 1) + size // 4 + 1
        starting_positions[3] = size * (3 * size // 4 - 1) + 3 * size // 4 + 1

    # Initialize the players.
    obs.players = []
    for i in range(num_agents):
        shipyards = {create_uid(): [starting_positions[i], 0, 0]}
        obs.players.append([state[0].reward, shipyards, {}])

    return state


def interpreter(state, env):
    obs = state[0].observation
    config = env.configuration

    # Initialize the board (place cell halite and starting ships).
    if env.done:
        return populate_board(state, env)

    # Interpreter invoked here
    actions = [agent.action for agent in state]
    board = Board(obs, config, actions)
    board = board.next()
    state[0].observation = obs = utils.structify(board.observation)

    # Remove players with invalid status or insufficient potential.
    for index, agent in enumerate(state):
        player_halite, shipyards, ships = obs.players[index]
        if agent.status == "ACTIVE" and len(ships) == 0 and (len(shipyards) == 0 or player_halite < config.spawnCost):
            # Agent can no longer gather any halite
            agent.status = "DONE"
            agent.reward = board.step - board.configuration.episode_steps - 1
        if agent.status != "ACTIVE" and agent.status != "DONE":
            obs.players[index] = [0, {}, {}]

    # Check if done (< 2 players and num_agents > 1)
    if len(state) > 1 and sum(1 for agent in state if agent.status == "ACTIVE") < 2:
        for agent in state:
            if agent.status == "ACTIVE":
                agent.status = "DONE"

    # Update Rewards.
    for index, agent in enumerate(state):
        if agent.status == "ACTIVE":
            agent.reward = obs.players[index][0]
        elif agent.status != "DONE":
            agent.reward = 0

    return state


def renderer(state, env):
    config = env.configuration
    size = config.size
    obs = state[0].observation

    board = [[h, -1, -1, -1] for h in obs.halite]
    for index, player in enumerate(obs.players):
        _, shipyards, fleets = player
        for shipyard in shipyards.values():
            shipyard_pos, _, _ = shipyard
            board[shipyard_pos][1] = index
        for fleet in fleets.values():
            fleet_pos, fleet_halite, ship_count, _, _ = fleet
            board[fleet_pos][2] = index
            board[fleet_pos][3] = ship_count

    col_divider = "|"
    row_divider = "+" + "+".join(["----"] * size) + "+\n"

    out = row_divider
    for row in range(size):
        for col in range(size):
            _, _, fleet, fleet_halite = board[col + row * size]
            out += col_divider + (
                f"{min(int(fleet_halite), 99)}S{fleet}" if fleet > -1 else ""
            ).ljust(4)
        out += col_divider + "\n"
        for col in range(size):
            halite, shipyard, _, _ = board[col + row * size]
            if shipyard > -1:
                out += col_divider + f"SY{shipyard}".ljust(4)
            else:
                out += col_divider + str(min(int(halite), 9999)).rjust(4)
        out += col_divider + "\n" + row_divider

    return out


dir_path = path.dirname(__file__)
json_path = path.abspath(path.join(dir_path, "halite_fleets.json"))
with open(json_path) as json_file:
    specification = json.load(json_file)


def html_renderer():
    js_path = path.abspath(path.join(dir_path, "halite_fleets.js"))
    with open(js_path) as js_file:
        return js_file.read()
