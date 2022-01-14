package test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import halite.HaliteJson;

public class HaliteJsonTest {
    @Test
    public void containsKey() throws IOException {
        Path observation = Paths.get("bin", "test", "observation.json");
        String raw = Files.readString(observation);        
        
        Assert.assertTrue(HaliteJson.containsKey(raw, "halite"));
        Assert.assertFalse(HaliteJson.containsKey(raw, "notThere"));
    }

    @Test
    public void getIntFromJson() throws IOException {
        Path observation = Paths.get("bin", "test", "observation.json");
        String raw = Files.readString(observation);        
        
        Assert.assertEquals(HaliteJson.getIntFromJson(raw, "step"), 16);
    }

    @Test
    public void getStrFromJson() {
        Assert.assertTrue(HaliteJson.getStrFromJson("{'test': 'foo'}", "test").equals("foo"));
    }

    @Test
    public void getFloatArrFromJson() throws IOException {
        Path observation = Paths.get("bin", "test", "observation.json");
        String raw = Files.readString(observation);        

        float[] halite = HaliteJson.getFloatArrFromJson(raw, "halite");
        Assert.assertEquals(halite[3], 1.372, 0.0001);
    }

    @Test
    public void getPlayerPartsFromJson() throws IOException {
        Path observation = Paths.get("bin", "test", "observation.json");
        String raw = Files.readString(observation);        

        String[] players = HaliteJson.getPlayerPartsFromJson(raw);
        Assert.assertEquals(players.length, 4);
        Assert.assertEquals(players[0].substring(0, 3), "500");
    }

    @Test
    public void getPlayerIdxFromJson() throws IOException {
        Path observation = Paths.get("bin", "test", "observation.json");
        String raw = Files.readString(observation);        

        Assert.assertEquals(HaliteJson.getPlayerIdxFromJson(raw), 0);
    }
}
