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
        this.player = HaliteJson.getIntFromJson(rawObservation, "player");
        this.step = HaliteJson.getIntFromJson(rawObservation, "step");
        this.remainingOverageTime = HaliteJson.getFloatFromJson(rawObservation, "remainingOverageTime");
        String[] playerParts = HaliteJson.getStrArrFromJson(rawObservation, "player");
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
