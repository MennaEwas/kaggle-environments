from kaggle_environments import make
from .halite_fleets import random_agent
from .helpers import *


def test_shipyard_action_class_serialization():
    launch = ShipyardAction.launch_ships_in_direction(10, Direction.NORTH)
    assert str(launch) == "LAUNCH_10_NORTH", "Launch: " + str(launch)

    parsed = ShipyardAction.from_str(str(launch))
    assert launch.action_type == parsed.action_type, "type"
    assert launch.num_ships == parsed.num_ships, "num_ships"
    assert launch.direction == parsed.direction, "direction"

    spawn = ShipyardAction.spawn_ships(1)
    assert str(spawn) == "SPAWN_1", "Spawn: " + str(launch)

    parsed = ShipyardAction.from_str(str(spawn))
    assert spawn.action_type == parsed.action_type, "type"
    assert spawn.num_ships == parsed.num_ships, "num_ships"

def test_halite_no_repeated_steps():
    step_count = 10
    actual_steps = []

    def step_appender_agent(obs, config):
        actual_steps.append(obs.step)
        return {}

    env = make("halite_fleets", configuration={"episodeSteps": step_count}, debug=True)
    env.run([step_appender_agent])
    assert actual_steps == list(range(step_count - 1))

def test_halite_helpers():
    env = make("halite_fleets", configuration={"size": 5, "episodeSteps": 100}, debug=True)

    @board_agent
    def helper_agent(board):
        for shipyard in board.current_player.shipyards:
            if shipyard.ship_count >= 10:
                shipyard.next_action = ShipyardAction.launch_ships_in_direction(10, Direction.NORTH)
            else:
                shipyard.next_action = ShipyardAction.spawn_ships(1)

    env.run([helper_agent, helper_agent])

    json = env.toJSON()
    assert json["name"] == "halite_fleets"
    assert json["statuses"] == ["DONE", "DONE"]


def create_board(size=3, starting_halite=100, agent_count=2, random_seed=0):
    env = make("halite_fleets", configuration={
        "size": size,
        "startingHalite": starting_halite,
        "randomSeed": random_seed
    })
    return Board(env.reset(agent_count)[0].observation, env.configuration)

