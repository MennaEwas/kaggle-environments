package test;

import java.nio.file.Files;

import halite.Observation;

public class ObservationTest {
    
    @Test
    public void givenValidObservation_createSuccessful() {
        String rawObservation = Files.readString("observation.json");        
        
        Observation ob = new Observation(rawObservation);

        Assert.assertEquals(ob.player, 0);
        Assert.assertEquals(ob.step, 16);
    }
}