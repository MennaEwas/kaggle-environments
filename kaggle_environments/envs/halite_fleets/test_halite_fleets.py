from kaggle_environments import make
from .halite_fleets import random_agent
from .helpers import *


def test_halite_no_repeated_steps():
    step_count = 10
    actual_steps = []

    def step_appender_agent(obs, config):
        actual_steps.append(obs.step)
        return {}

    env = make("halite", configuration={"episodeSteps": step_count}, debug=True)
    env.run([step_appender_agent])
    assert actual_steps == list(range(step_count - 1))

def test_halite_helpers():
    env = make("halite_fleets", configuration={"size": 3})

    @board_agent
    def helper_agent(board):
        for shipyard in board.current_player.shipyards:
            shipyard.next_action = ShipyardAction.spawn_ships(1)

    env.run([helper_agent, helper_agent])

    json = env.toJSON()
    assert json["name"] == "halite"
    assert json["statuses"] == ["DONE", "DONE"]


def create_board(size=3, starting_halite=100, agent_count=2, random_seed=0):
    env = make("halite_fleets", configuration={
        "size": size,
        "startingHalite": starting_halite,
        "randomSeed": random_seed
    })
    return Board(env.reset(agent_count)[0].observation, env.configuration)

