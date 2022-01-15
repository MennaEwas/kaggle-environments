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
    public final float remainingOverageTime;

    private static String shortenFrontAndBack(String target, int n) {
        return target.substring(n, target.length() - n);
    }

    public Observation(String rawObservation) {
        // avoid importing json library? worth it?
        this.halite = HaliteJson.getFloatArrFromJson(rawObservation, "halite");
        this.player = HaliteJson.getPlayerIdxFromJson(rawObservation);
        this.step = HaliteJson.getIntFromJson(rawObservation, "step");
        this.remainingOverageTime = HaliteJson.getFloatFromJson(rawObservation, "remainingOverageTime");
        String[] playerParts = HaliteJson.getPlayerPartsFromJson(rawObservation);
        playerHlt = new int[playerParts.length];
        playerShipyards = new ArrayList<HashMap<String, int[]>>();
        playerFleets = new ArrayList<HashMap<String, String[]>>();

        for (int i = 0; i < playerParts.length; i ++) {
            String playerPart = playerParts[i];
            playerHlt[i] = Integer.parseInt(playerPart.split(", ")[0]);

            int startShipyards = playerPart.indexOf("{");
            int endShipyards = playerPart.indexOf("}");
            String shipyardsStr = playerPart.substring(startShipyards + 1, endShipyards - 1);
            HashMap<String, int[]> shipyards = new HashMap<String, int[]>();
            Arrays.stream(shipyardsStr.split("], ")).forEach(shipyardStr -> {
                if (shipyardStr.length() == 0) {
                    return;
                }
                String[] kvparts = shipyardStr.split(": \\[");
                String shipyardId = shortenFrontAndBack(kvparts[0], 1);
                String[] shipyardStrs = kvparts[1].split(", ");
                int[] shipyard = new int[shipyardStrs.length];
                Integer[] shipyardInts = Arrays.stream(shipyardStrs).map(s -> Integer.parseInt(s)).toArray(Integer[]::new);
                for(int j = 0; j < shipyard.length; j++) {
                    shipyard[j] = shipyardInts[j];
                }
                shipyards.put(shipyardId, shipyard);
            });
            playerShipyards.add(shipyards);

            int startFleets = playerPart.indexOf("}, ");
            String fleetsStr = playerPart.substring(startFleets + 4, playerPart.length() - 1);
            HashMap<String, String[]> fleets = new HashMap<>();
            Arrays.stream(fleetsStr.split("], ")).forEach(fleetStr -> {
                if (fleetStr.length() == 0) {
                    return;
                }
                String[] kvparts = fleetStr.split(": ");
                String fleetId = shortenFrontAndBack(kvparts[0], 1);
                String[] fleet = shortenFrontAndBack(kvparts[1], 1).split(", ");
                fleets.put(fleetId, fleet);
            });
            playerFleets.add(fleets);
        }
    }

    
}
