package org.mtgpeasant.decks.infect;

import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.mana.Mana;
import org.mtgpeasant.perfectdeck.common.matchers.MulliganRules;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.Game;
import org.mtgpeasant.perfectdeck.goldfish.Permanent;
import org.mtgpeasant.perfectdeck.goldfish.Seer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.mtgpeasant.perfectdeck.goldfish.Permanent.*;

public class InfectDeckPilot extends DeckPilot<Game> implements Seer.SpellsPlayer {

    private static final Mana G = Mana.of("G");
    private static final Mana G1 = Mana.of("1G");
    private static final Mana GG = Mana.of("GG");
    private static final Mana TWO = Mana.of("2");

    // LANDS
    private static final String FOREST = "forest";
    private static final String PENDELHAVEN = "pendelhaven";

    // CREATURES
    private static final String ICHORCLAW_MYR = "ichorclaw myr";
    private static final String GLISTENER_ELF = "glistener elf";
    private static final String BLIGHT_MAMBA = "blight mamba";

    // BOOSTS
    private static final String RANCOR = "rancor"; //
    private static final String SEAL_OF_STRENGTH = "seal of strength"; // (enchant) G: sacrifice: +3/+3
    private static final String SCALE_UP = "scale up"; // G: crea become 6/4
    private static final String VINES_OF_VASTWOOD = "vines of vastwood"; // (instant) GG: +4/+4
    private static final String GIANT_GROWTH = "giant growth";
    private static final String LARGER_THAN_LIFE = "larger than life"; // 1G: +4/+4
    private static final String INVIGORATE = "invigorate"; // (free if forest on battlefield) +4/+4
    private static final String MUTAGENIC_GROWTH = "mutagenic growth"; // (-2 life): +2/+2
    private static final String GROUNDSWELL = "groundswell"; // G: +2/+2; landfall: +4/+4
    private static final String RANGER_S_GUILE = "ranger's guile"; // G: +1/+1
    private static final String MIGHT_OF_OLD_KROSA = "might of old krosa"; // G: +4/+4 on your turn
    private static final String BLOSSOMING_DEFENSE = "blossoming defense"; // G: +2/+2

    // FREE MANA
    private static final String LOTUS_PETAL = "lotus petal";

    // OTHERS
    private static final String GITAXIAN_PROBE = "gitaxian probe";
    private static final String MENTAL_MISSTEP = "mental misstep";
    private static final String APOSTLE_S_BLESSING = "apostle's blessing";
    private static final String SCALE_UP_TAG = "*scale up";

    private static String[] MANA_PRODUCERS = new String[]{LOTUS_PETAL, FOREST, PENDELHAVEN};
    private static String[] CREATURES = new String[]{GLISTENER_ELF, ICHORCLAW_MYR, BLIGHT_MAMBA};
    private static String[] BOOSTS = new String[]{RANCOR, SEAL_OF_STRENGTH, SCALE_UP, VINES_OF_VASTWOOD, GIANT_GROWTH, LARGER_THAN_LIFE, INVIGORATE, MUTAGENIC_GROWTH, GROUNDSWELL, MIGHT_OF_OLD_KROSA, BLOSSOMING_DEFENSE, RANGER_S_GUILE};

    private static final String TEMP_BOOST = "*+1/+1";

    private static MulliganRules rules;

    static {
        try {
            rules = MulliganRules.parse(new InputStreamReader(InfectDeckPilot.class.getResourceAsStream("/infect-rules.txt")));
        } catch (IOException e) {
            rules = null;
            System.err.println(e);
        }
    }

    public InfectDeckPilot(Game game) {
        super(game);
    }

    @Override
    public boolean keepHand(Cards hand) {
        if (game.getMulligans() >= 3) {
            return true;
        }
        return rules.firstMatch(hand).isPresent();
    }

    @Override
    public void start() {
        putOnBottomOfLibrary(game.getMulligans());
    }

    @Override
    public void firstMainPhase() {
        // whichever the situation, if I have a probe in hand: play it
        while (playOneOf(GITAXIAN_PROBE).isPresent()) {
        }

        // land
        // pendelhaven if no invigorate in hand and no forest on battlefield
        boolean needToLandForestForInvigorate = game.getHand().contains(INVIGORATE) && !game.getBattlefield().findFirst(withName(FOREST)).isPresent();
        if (needToLandForestForInvigorate) {
            playOneOf(FOREST);
        }
        // else land pendelhaven in priority
        playOneOf(PENDELHAVEN, FOREST);

        // play all petals
        while (playOneOf(LOTUS_PETAL).isPresent()) {
        }

        if (game.getCurrentTurn() > 1) {
            Optional<Seer.VictoryRoute> victoryRoute = Seer.findRouteToVictory(this, BOOSTS);
            if (victoryRoute.isPresent()) {
                game.log(">> I can rush now with: " + victoryRoute);
                victoryRoute.get().play(this);
                return;
            }
        }

        // else play boosts in best order
        List<Permanent> creatures = game.getBattlefield().find(creaturesThatCanBeTapped());
        if (creatures.isEmpty()) {
            return;
        }
        Collection<String> boostsToPlay = game.isLanded() ?
                game.getHand().findAll(MUTAGENIC_GROWTH, INVIGORATE, SCALE_UP, RANCOR, GROUNDSWELL, MIGHT_OF_OLD_KROSA, GIANT_GROWTH, SEAL_OF_STRENGTH, BLOSSOMING_DEFENSE, LARGER_THAN_LIFE, VINES_OF_VASTWOOD, RANGER_S_GUILE)
                : game.getHand().findAll(MUTAGENIC_GROWTH, INVIGORATE, SCALE_UP, RANCOR, MIGHT_OF_OLD_KROSA, GIANT_GROWTH, SEAL_OF_STRENGTH, BLOSSOMING_DEFENSE, LARGER_THAN_LIFE, VINES_OF_VASTWOOD, GROUNDSWELL, RANGER_S_GUILE);
        boostsToPlay.forEach(card -> {
            if (canPlay(card)) {
                play(card);
            }
        });
    }

    @Override
    public void combatPhase() {
        List<Permanent> creatures = game.getBattlefield().find(creaturesThatCanBeTapped());
        if (creatures.isEmpty()) {
            return;
        }

        // use untapped pendelhavens to boost
        game.getBattlefield().find(withName(PENDELHAVEN).and(untapped())).forEach(card -> {
            game.tap(card);
            creatures.get(0).addCounter(TEMP_BOOST, 1);
        });

        // sacrifice all seals
        game.getBattlefield().find(withName(SEAL_OF_STRENGTH)).forEach(card -> {
            game.sacrifice(card);
            creatures.get(0).addCounter(TEMP_BOOST, 3);
        });

        // attach with all creatures
        creatures.forEach(card -> {
            int strength = strength(card);
            game.tapForAttack(card, strength);
            game.poisonOpponent(strength);
        });
    }

    @Override
    public void secondMainPhase() {
        // cast 1 creature if none on battlefield
        if (game.getBattlefield().count(withType(Game.CardType.creature)) == 0) {
            playOneOf(CREATURES);
        }

        // play all rancors & seals
        while (playOneOf(RANCOR, SEAL_OF_STRENGTH).isPresent()) {
        }

        // cast extra creatures
        while (playOneOf(CREATURES).isPresent()) {
        }
    }

    @Override
    public void endingPhase() {
        if (game.getHand().size() > 7) {
            discard(game.getHand().size() - 7);
        }
    }

    boolean canPay(Mana cost) {
        // potential mana pool is current pool + untapped lands + petals on battlefield
        Mana potentialPool = game.getPool()
                .plus(Mana.of(0, 0, game.getBattlefield().count(withName(MANA_PRODUCERS).and(untapped())), 0, 0, 0));
        return potentialPool.contains(cost);
    }

    void produce(Mana cost) {
        while (!game.canPay(cost)) {
            Optional<Permanent> producer = game.getBattlefield().findFirst(withName(FOREST, PENDELHAVEN, LOTUS_PETAL).and(untapped()));
            if (producer.isPresent()) {
                if (producer.get().getCard().equals(LOTUS_PETAL)) {
                    game.sacrifice(producer.get());
                    game.add(G);
                } else {
                    // a land
                    game.tapLandForMana(producer.get(), G);
                }
            } else {
                // can't preparePool !!!
                return;
            }
        }
    }

    /**
     * Casts the first possible card from the list
     *
     * @param cards cards ordered by preference
     * @return the successfully cast card
     */
    Optional<String> playOneOf(String... cards) {
        for (String card : cards) {
            if (canPlay(card)) {
                play(card);
                return Optional.of(card);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean canPlay(String card) {
        // first check card is in hand
        if (!game.getHand().contains(card)) {
            return false;
        }
        switch (card) {
            case FOREST:
            case PENDELHAVEN:
                return !game.isLanded();
            // FREE MANA
            case LOTUS_PETAL:
                return true;

            case GITAXIAN_PROBE:
                return true;

            // creatures
            case GLISTENER_ELF:
                return canPay(G);
            case ICHORCLAW_MYR:
                return canPay(TWO);
            case BLIGHT_MAMBA:
                return canPay(G1);

            // BOOSTS
            case MUTAGENIC_GROWTH: // (-2 life): +2/+2
                // need a target creature ready to attack
                return game.getBattlefield().findFirst(creaturesThatCanBeTapped()).isPresent();
            case INVIGORATE: // (free if forest on battlefield) +4/+4
                // need a target creature ready to attack and a forest
                return game.getBattlefield().findFirst(creaturesThatCanBeTapped()).isPresent() && game.getBattlefield().findFirst(withName(FOREST)).isPresent();
            case SCALE_UP: // G: crea become 6/4
                // need a target creature ready to attack & G
                return game.getBattlefield().findFirst(creaturesThatCanBeTapped().and(notWithTag(SCALE_UP_TAG))).isPresent() && canPay(G);
            case GIANT_GROWTH: // G: +3/+3
            case GROUNDSWELL: // G: +2/+2; landfall: +4/+4
            case RANGER_S_GUILE: // G: +1/+1
            case MIGHT_OF_OLD_KROSA: // G: +4/+4 on your turn
            case BLOSSOMING_DEFENSE: // G: +2/+2
                // need a target creature ready to attack & G
                return game.getBattlefield().findFirst(creaturesThatCanBeTapped()).isPresent() && canPay(G);
            case LARGER_THAN_LIFE: // 1G: +4/+4
                // need a target creature ready to attack & 1G
                return game.getBattlefield().findFirst(creaturesThatCanBeTapped()).isPresent() && canPay(G1);
            case VINES_OF_VASTWOOD: // (instant) GG: +4/+4
                // need a target creature && GG
                return game.getBattlefield().findFirst(creaturesThatCanBeTapped()).isPresent() && canPay(GG);

            // enchantments
            case RANCOR:
                // need a target creature (any)
                return game.getBattlefield().findFirst(withType(Game.CardType.creature)).isPresent() && canPay(G);
            case SEAL_OF_STRENGTH: // (enchant) G: sacrifice: +3/+3
                return canPay(G);
        }
        game.log("oops, unsupported card [" + card + "]");
        return false;
    }

    @Override
    public boolean play(String card) {
        switch (card) {
            case FOREST:
            case PENDELHAVEN:
                game.land(card);
                return true;

            case LOTUS_PETAL:
                game.castArtifact(card, Mana.zero());
                return true;

            case GITAXIAN_PROBE:
                game.castSorcery(card, Mana.zero());
                game.draw(1);
                return true;

            // creatures
            case GLISTENER_ELF:
                produce(G);
                game.castCreature(card, G);
                return true;
            case ICHORCLAW_MYR:
                produce(TWO);
                game.castCreature(card, TWO);
                return true;
            case BLIGHT_MAMBA:
                produce(G1);
                game.castCreature(card, G1);
                return true;

            // BOOSTS
            case MUTAGENIC_GROWTH: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get();
                game.castInstant(card, Mana.zero());
                targetCreature.addCounter(TEMP_BOOST, 2);
                return true;
            }
            case INVIGORATE: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get();
                game.castInstant(card, Mana.zero());
                targetCreature.addCounter(TEMP_BOOST, 4);
                return true;
            }
            case SCALE_UP: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped().and(notWithTag(SCALE_UP_TAG))).get();
                produce(G);
                game.castSorcery(card, G);
                targetCreature.tag(SCALE_UP_TAG);
                return true;
            }
            case GIANT_GROWTH: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get();
                produce(G);
                game.castInstant(card, G);
                targetCreature.addCounter(TEMP_BOOST, 3);
                return true;
            }
            case GROUNDSWELL: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get();
                produce(G);
                game.castInstant(card, G);
                targetCreature.addCounter(TEMP_BOOST, game.isLanded() ? 4 : 3);
                return true;
            }
            case RANGER_S_GUILE: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get();
                produce(G);
                game.castInstant(card, G);
                targetCreature.addCounter(TEMP_BOOST, 1);
                return true;
            }
            case MIGHT_OF_OLD_KROSA: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get();
                produce(G);
                game.castInstant(card, G);
                targetCreature.addCounter(TEMP_BOOST, 4);
                return true;
            }
            case BLOSSOMING_DEFENSE: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get();
                produce(G);
                game.castInstant(card, G);
                targetCreature.addCounter(TEMP_BOOST, 2);
                return true;
            }
            case LARGER_THAN_LIFE: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get();
                produce(G1);
                game.castSorcery(card, G1);
                targetCreature.addCounter(TEMP_BOOST, 4);
                return true;
            }
            case VINES_OF_VASTWOOD: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get();
                produce(GG);
                game.castSorcery(card, GG);
                targetCreature.addCounter(TEMP_BOOST, 4);
                return true;
            }

            // enchantments
            case RANCOR: {
                // target preferably a creature ready to attack, or else any creature
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped())
                        .orElseGet(() -> game.getBattlefield().findFirst(withType(Game.CardType.creature)).get());
                produce(G);
                game.castEnchantment(card, G).tag("on:" + targetCreature.getCard());
                targetCreature.incrCounter(RANCOR);
                return true;
            }
            case SEAL_OF_STRENGTH: {
                produce(G);
                game.castEnchantment(card, G);
                return true;
            }
        }
        game.log("oops, unsupported card [" + card + "]");
        return false;
    }

    int strength(Permanent creature) {
        // strength is base strength + temporary boosts + 2 * rancors
        return baseStrength(creature)
                + creature.getCounter(TEMP_BOOST)
                + 2 * creature.getCounter(RANCOR);
    }

    int baseStrength(Permanent creature) {
        switch (creature.getCard()) {
            case GLISTENER_ELF:
            case ICHORCLAW_MYR:
            case BLIGHT_MAMBA:
                return creature.hasTag(SCALE_UP_TAG) ? 6 : 1;

            default:
                return 0;
        }
    }

    void putOnBottomOfLibrary(int number) {
        for (int i = 0; i < number; i++) {
            if (game.putOnBottomOfLibraryOneOf(MENTAL_MISSTEP, APOSTLE_S_BLESSING, GITAXIAN_PROBE).isPresent()) {
                continue;
            }
            // discard extra lands
            if (game.getHand().count(MANA_PRODUCERS) > 2 && game.putOnBottomOfLibraryOneOf(FOREST, LOTUS_PETAL, PENDELHAVEN).isPresent()) {
                continue;
            }
            // discard extra creatures
            if (game.getHand().count(CREATURES) >= 2 && game.putOnBottomOfLibraryOneOf(BLIGHT_MAMBA, ICHORCLAW_MYR, GLISTENER_ELF).isPresent()) {
                continue;
            }
            // discard a boost
            if (game.putOnBottomOfLibraryOneOf(
                    RANGER_S_GUILE,
                    VINES_OF_VASTWOOD,
                    LARGER_THAN_LIFE,
                    BLOSSOMING_DEFENSE,
                    GIANT_GROWTH,
                    GROUNDSWELL,
                    MUTAGENIC_GROWTH,
                    SEAL_OF_STRENGTH,
                    MIGHT_OF_OLD_KROSA,
                    RANCOR,
                    INVIGORATE,
                    SCALE_UP
            ).isPresent()) {
                continue;
            }
            game.putOnBottomOfLibrary(game.getHand().getFirst());
        }
    }

    void discard(int number) {
        for (int i = 0; i < number; i++) {
            if (game.discardOneOf(MENTAL_MISSTEP, APOSTLE_S_BLESSING).isPresent()) {
                continue;
            }
            // discard extra lands
            if (game.getBattlefield().count(withName(MANA_PRODUCERS)) + game.getHand().count(MANA_PRODUCERS) > 3 && game.discardOneOf(MANA_PRODUCERS).isPresent()) {
                continue;
            }
            // discard extra creatures
            if (game.getBattlefield().count(withType(Game.CardType.creature)) + game.getHand().count(CREATURES) >= 2 && game.discardOneOf(BLIGHT_MAMBA, ICHORCLAW_MYR, GLISTENER_ELF).isPresent()) {
                continue;
            }
            // discard a boost
            if (game.discardOneOf(
                    RANGER_S_GUILE,
                    VINES_OF_VASTWOOD,
                    LARGER_THAN_LIFE,
                    BLOSSOMING_DEFENSE,
                    GIANT_GROWTH,
                    GROUNDSWELL,
                    MUTAGENIC_GROWTH,
                    SEAL_OF_STRENGTH,
                    MIGHT_OF_OLD_KROSA,
                    RANCOR,
                    INVIGORATE,
                    SCALE_UP
            ).isPresent()) {
                continue;
            }
            game.discard(game.getHand().getFirst());
        }
    }
}
