import java.util.ArrayList;
import java.util.Map.Entry;

import halite.*;

public class Bot {
  public static void main(final String[] args) throws Exception {
    //Agent agent = new Agent();
    // initialize
    //agent.initialize();
    while (true) {
      /** Do not edit! **/
      // wait for updates
      //agent.update();

      Board board = new Board("", "", "");

      Player me = board.currentPlayer();
      int turn = board.step;
      int spawnCost = board.configuration.spawnCost;
      int haliteLeft = me.halite;

      for (Shipyard shipyard : me.shipyards()) {
          if (shipyard.shipCount > 10) {
              Direction dir = Direction.fromIndex(turn % 4);
              ShipyardAction action = ShipyardAction.launchFleetWithFlightPlan(2, dir.toChar());
              shipyard.setNextAction(action);
          } else if (haliteLeft > spawnCost * shipyard.maxSpawn()) {
              ShipyardAction action = ShipyardAction.spawnShips(shipyard.maxSpawn());
              shipyard.setNextAction(action);
              haliteLeft -= spawnCost * shipyard.maxSpawn();
          } else if (haliteLeft > spawnCost) {
              ShipyardAction action = ShipyardAction.spawnShips(1);
              shipyard.setNextAction(action);
              haliteLeft -= spawnCost;
          }
      }


      /** AI Code Goes Above! **/

      /** Do not edit! **/
      StringBuilder commandBuilder = new StringBuilder("");
      boolean first = true;
      for (Entry<String, ShipyardAction> entry : board.currentPlayer().nextActions().entrySet()) {
          if (first) {
              first = false;
          } else {
              commandBuilder.append(",");
          }
          commandBuilder.append(String.format("%s:%s", entry.getKey(), entry.getValue().toString()));
      }
      System.out.println(commandBuilder.toString());
      // end turn
      //agent.endTurn();

    }
  }
}
