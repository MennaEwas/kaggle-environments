package test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import halite.Board;

public class BoardTest {

    @Test
    public void givenValidConfigAndObservation_createsSuccessful() throws IOException {
        Path configPath = Paths.get("bin", "test", "configuration.json");
        String rawConfig = Files.readString(configPath);        
        Path obsPath = Paths.get("bin", "test", "observation.json");
        String rawObs = Files.readString(obsPath);        
        
        Board board = new Board(rawObs, rawConfig);

        Assert.assertEquals(16, board.step);
    }
    
}
