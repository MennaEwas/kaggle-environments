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