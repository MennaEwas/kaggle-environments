package halite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Observation {
    public final float[] halite;
    public final int[] playerHlt; 
    public final ArrayList<HashMap<String, int[]>> playerShipyards;
    public final ArrayList<HashMap<String, String[]>> playerFleets;
    public final int player;
    public final int step;
    public final int remainingOverageTime;

    private static String shortenFrontAndBack(String target, int n) {
        return target.substring(n, target.length() - n);
    }

    public Observation(String rawObservation) {
        // avoid importing json library? worth it?
        String haliteStr = "'hatlite': [";
        int haliteIdx = rawObservation.indexOf(haliteStr);
        int haliteEndIdx = rawObservation.substring(haliteIdx).indexOf("]");
        String haliteArray = rawObservation.substring(haliteIdx + haliteStr.length(), haliteEndIdx - 1);
        String[] parts = haliteArray.split(", ");
        Float[] haliteFlt = Arrays.stream(parts).map(str -> Float.parseFloat(str)).toArray(Float[]::new);
        halite =  new float[haliteFlt.length];
        for (var i = 0; i < halite.length; i++) {
            halite[i] = (float)haliteFlt[i];
        }

        String playerStr = "'player': ";
        int playerIdx = rawObservation.indexOf(playerStr);
        String playerId = rawObservation.substring(playerIdx + playerStr.length(), playerIdx + playerStr.length() + 1);
        this.player = Integer.parseInt(playerId);

        String stepStr = "'step': ";
        int stepIdx = rawObservation.indexOf(playerStr);
        int stepEndIdx = rawObservation.substring(stepIdx).indexOf(",");
        String step = rawObservation.substring(stepIdx + stepStr.length(), stepEndIdx);
        this.step = Integer.parseInt(step);

        String remainingOverTimeStr = "'remainingOverageTime': ";
        int remainingOverageTimeIdx = rawObservation.indexOf(remainingOverTimeStr);
        int remainingOverageTimeEndIdx = rawObservation.substring(remainingOverageTimeIdx).indexOf("}");
        String remainingOverageTime = rawObservation.substring(remainingOverageTimeIdx + remainingOverTimeStr.length(), remainingOverageTimeEndIdx);
        this.remainingOverageTime = Integer.parseInt(remainingOverageTime);
        
        int playersStart = rawObservation.indexOf("[[");
        int playersEnd = rawObservation.indexOf("]]");
        String players = rawObservation.substring(playersStart + 1, playersEnd + 1);
        String[] playerParts = players.split(", ");
        playerHlt = new int[playerParts.length];
        playerShipyards = new ArrayList<HashMap<String, int[]>>();
        playerFleets = new ArrayList<HashMap<String, String[]>>();

        for (int i = 0; i < playerParts.length; i ++) {
            String[] pParts = playerParts[i].split(", ");
            playerHlt[i] = Integer.parseInt(pParts[0]);

            HashMap<String, int[]> shipyards = new HashMap<String, int[]>();
            Arrays.stream(shortenFrontAndBack(pParts[1], 1).split(", ")).forEach(shipyardStr -> {
                String[] kvparts = shipyardStr.split(": ");
                String shipyardId = shortenFrontAndBack(kvparts[0], 1);
                String[] shipyardStrs = shortenFrontAndBack(kvparts[1], 1).split(", ");
                int[] shipyard = new int[shipyardStrs.length];
                Integer[] shipyardInts = Arrays.stream(shipyardStrs).map(s -> Integer.parseInt(s)).toArray(Integer[]::new);
                for(int j = 0; j < shipyard.length; j++) {
                    shipyard[j] = shipyardInts[j];
                }
                shipyards.put(shipyardId, shipyard);
            });
            playerShipyards.add(shipyards);

            HashMap<String, String[]> fleets = new HashMap<>();
            Arrays.stream(shortenFrontAndBack(pParts[1], 1).split(", ")).forEach(fleetStr -> {
                String[] kvparts = fleetStr.split(": ");
                String fleetId = shortenFrontAndBack(kvparts[0], 1);
                String[] fleet = shortenFrontAndBack(kvparts[1], 1).split(", ");
                fleets.put(fleetId, fleet);
            });
            playerFleets.add(fleets);
        }
    }

    
}
