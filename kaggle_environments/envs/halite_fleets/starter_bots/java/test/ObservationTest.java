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
    }
}