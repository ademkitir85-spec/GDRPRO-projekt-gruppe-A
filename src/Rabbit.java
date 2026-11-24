import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import itumulator.simulator.Actor;
import itumulator.world.Location;
import itumulator.world.World;

/**
 * En kanin (K1-2a til K1-2g).
 *
 * Designvalg (skriv dem gerne i dagbogen):
 * - Kaninen spiser græs, den står OVEN PÅ.
 * - Energi falder med 1 hvert skridt om dagen. Om natten sover kaninen og bruger ikke energi.
 * - Max-energien falder, jo ældre kaninen bliver (K1-2d).
 * - Kaninen får unger, når en anden kanin står ved siden af, og den har nok energi.
 * - Om natten går kaninen mod sit hul. Står den på eller lige ved hullet,
 *   bliver den fjernet fra kortet med world.remove(this) og "sover" i hullet.
 *   Om morgenen sættes den tilbage på (eller ved siden af) hullet.
 */
public class Rabbit implements Actor {

    // Konstanter - justér dem, hvis simulationen opfører sig mærkeligt
    private static final int MAX_AGE = 150;             // skridt før kaninen dør af alderdom
    private static final int START_ENERGY = 20;
    private static final int GRASS_ENERGY = 6;          // energi pr. spist græs
    private static final int ADULT_AGE = 10;            // alder før den kan få unger
    private static final int REPRODUCE_ENERGY = 12;     // min. energi for at få unger
    private static final int REPRODUCE_COST = 6;
    private static final double REPRODUCE_CHANCE = 0.1;
    private static final double DIG_CHANCE = 0.1;
    private static final int DIG_COST = 2;

    private final Random random = new Random();
    private int age = 0;
    private int energy = START_ENERGY;
    private Burrow burrow = null;   // K1-2f: en kanin har højst ét hul

    @Override
    public void act(World world) {
        // K1-2g: om natten søger kaninen mod sit hul og sover
        if (world.isNight()) {
            actAtNight(world);
            return;
        }

        // Om morgenen: kom op af hullet igen
        if (!world.isOnTile(this)) {
            wakeUp(world);
            if (!world.isOnTile(this)) {
                return; // ingen plads til at komme op endnu
            }
        }

        // K1-2c + K1-2d: alder og energi
        age++;
        energy--;
        if (energy > getMaxEnergy()) {
            energy = getMaxEnergy();
        }

        // K1-2b: kaninen dør af sult eller alderdom
        if (energy <= 0 || age >= MAX_AGE) {
            die(world);
            return;
        }

        eat(world);              // K1-2c
        findOrDigBurrow(world);  // K1-2f
        tryReproduce(world);     // K1-2e
        moveRandomly(world);
    }

    // ---------- K1-2b: død ----------

    public void die(World world) {
        world.delete(this);
    }

    // ---------- K1-2d: alder bestemmer energi ----------

    /** Jo ældre kaninen er, jo mindre energi kan den have. */
    public int getMaxEnergy() {
        return Math.max(5, 30 - age / 10);
    }

    // ---------- K1-2c: spis græs ----------

    private void eat(World world) {
        Location here = world.getLocation(this);
        if (world.containsNonBlocking(here)
                && world.getNonBlocking(here) instanceof Grass
                && energy < getMaxEnergy()) {
            world.delete(world.getNonBlocking(here));
            energy = Math.min(getMaxEnergy(), energy + GRASS_ENERGY);
        }
    }

    // ---------- K1-2f: find et hul at dele, eller grav et nyt ----------

    private void findOrDigBurrow(World world) {
        if (burrow != null) {
            return; // har allerede et hul
        }
        Location here = world.getLocation(this);

        // Del et eksisterende hul, hvis der er et her eller lige ved siden af
        List<Location> nearby = new ArrayList<>(world.getSurroundingTiles(here));
        nearby.add(here);
        for (Location l : nearby) {
            if (world.containsNonBlocking(l) && world.getNonBlocking(l) instanceof Burrow) {
                burrow = (Burrow) world.getNonBlocking(l);
                return;
            }
        }

        // Ellers: grav et nyt hul med en vis chance
        if (random.nextDouble() < DIG_CHANCE && energy > DIG_COST) {
            if (world.containsNonBlocking(here)) {
                Object thing = world.getNonBlocking(here);
                if (!(thing instanceof Grass)) {
                    return; // her ligger noget andet end græs
                }
                world.delete(thing); // der kan kun være én NonBlocking pr. felt
            }
            burrow = new Burrow();
            world.setTile(here, burrow);
            energy -= DIG_COST;
        }
    }

    // ---------- K1-2e: reproduktion ----------

    private void tryReproduce(World world) {
        if (age < ADULT_AGE || energy < REPRODUCE_ENERGY) {
            return;
        }
        if (random.nextDouble() >= REPRODUCE_CHANCE) {
            return;
        }

        Location here = world.getLocation(this);

        // Er der en anden kanin ved siden af?
        boolean partnerNearby = false;
        for (Location l : world.getSurroundingTiles(here)) {
            if (!world.isTileEmpty(l) && world.getTile(l) instanceof Rabbit) {
                partnerNearby = true;
                break;
            }
        }
        if (!partnerNearby) {
            return;
        }

        // Ungen placeres på et ledigt nabofelt
        List<Location> empty = new ArrayList<>(world.getEmptySurroundingTiles(here));
        if (empty.isEmpty()) {
            return;
        }
        Location babySpot = empty.get(random.nextInt(empty.size()));
        world.setTile(babySpot, new Rabbit());
        energy -= REPRODUCE_COST;
    }

    // ---------- K1-2g: nat og søvn ----------

    private void actAtNight(World world) {
        if (!world.isOnTile(this)) {
            return; // sover allerede i hullet
        }
        if (burrow == null || !world.contains(burrow)) {
            return; // intet hul: kaninen bliver bare stående
        }

        Location here = world.getLocation(this);
        Location home = world.getLocation(burrow);

        if (distance(here, home) <= 1) {
            world.remove(this); // kryber ned i hullet og sover
            return;
        }
        moveTowards(world, home); // det er ikke et krav, at den når frem
    }

    private void wakeUp(World world) {
        if (burrow == null || !world.contains(burrow)) {
            return;
        }
        Location home = world.getLocation(burrow);
        if (world.isTileEmpty(home)) {
            world.setTile(home, this);
            return;
        }
        // Hullet er optaget af en anden kanin: kom op ved siden af
        List<Location> empty = new ArrayList<>(world.getEmptySurroundingTiles(home));
        if (!empty.isEmpty()) {
            world.setTile(empty.get(random.nextInt(empty.size())), this);
        }
    }

    // ---------- Bevægelse ----------

    private void moveRandomly(World world) {
        List<Location> empty = new ArrayList<>(world.getEmptySurroundingTiles(world.getLocation(this)));
        if (empty.isEmpty()) {
            return;
        }
        world.move(this, empty.get(random.nextInt(empty.size())));
    }

    /** Går ét skridt mod target (hvis det bringer kaninen tættere på). */
    private void moveTowards(World world, Location target) {
        Location here = world.getLocation(this);
        Location best = null;
        int bestDist = distance(here, target);
        for (Location l : world.getEmptySurroundingTiles(here)) {
            int d = distance(l, target);
            if (d < bestDist) {
                best = l;
                bestDist = d;
            }
        }
        if (best != null) {
            world.move(this, best);
        }
    }

    /** Antal skridt mellem to felter (diagonale skridt tæller som ét). */
    private int distance(Location a, Location b) {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
    }

    // ---------- Getters (praktiske til unit tests) ----------

    public int getAge() {
        return age;
    }

    public int getEnergy() {
        return energy;
    }

    public Burrow getBurrow() {
        return burrow;
    }
}