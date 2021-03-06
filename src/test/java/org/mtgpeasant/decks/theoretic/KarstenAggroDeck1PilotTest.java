package org.mtgpeasant.decks.theoretic;

import org.junit.Test;
import org.mtgpeasant.perfectdeck.Tools;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;

import java.io.File;
import java.io.IOException;

public class KarstenAggroDeck1PilotTest {
    @Test
    public void karsten_aggro_deck1_goldfish() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/karsten-deck-1.txt"),
                KarstenAggroDeck1Pilot.class.getName(),
                50000,
                GoldfishSimulator.Start.BOTH,
                15,
                false,
                false);
    }


    @Test
    public void karsten_aggro_deck1_observe() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/karsten-deck-1.txt"),
                KarstenAggroDeck1Pilot.class.getName(),
                3,
                GoldfishSimulator.Start.BOTH,
                15,
                true,
                true);
    }

}
