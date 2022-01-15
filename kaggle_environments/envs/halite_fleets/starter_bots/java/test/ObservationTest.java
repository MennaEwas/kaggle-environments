package test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import halite.Observation;

public class ObservationTest {
    
    @Test
    public void givenValidObservation_createSuccessful() throws IOException {
        Path observation = Paths.get("bin", "test", "observation.json");
        String rawObservation = Files.readString(observation);        
        
        Observation ob = new Observation(rawObservation);

        Assert.assertEquals(ob.player, 0);
        Assert.assertEquals(ob.step, 16);
        Assert.assertEquals(ob.playerHlt.length, 4);
        Assert.assertEquals(ob.playerFleets.size(), 4);
        Assert.assertEquals(ob.playerFleets.get(0).size(), 0);
        Assert.assertEquals(ob.playerShipyards.size(), 4);
        Assert.assertEquals(ob.playerShipyards.get(0).size(), 1);
    }

    @Test
    public void givenFullObservation_createSuccessful() throws IOException {
        Path observation = Paths.get("bin", "test", "fullob.json");
        String rawObservation = Files.readString(observation);        
        
        Observation ob = new Observation(rawObservation);

        Assert.assertEquals(ob.player, 0);
        Assert.assertEquals(ob.step, 16);
        Assert.assertEquals(ob.playerHlt.length, 4);
        Assert.assertEquals(ob.playerFleets.size(), 4);
        Assert.assertEquals(ob.playerFleets.get(0).size(), 0);
        Assert.assertEquals(ob.playerShipyards.size(), 4);
        Assert.assertEquals(ob.playerShipyards.get(0).size(), 1);
    }
}