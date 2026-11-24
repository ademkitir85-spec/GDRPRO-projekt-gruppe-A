import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import itumulator.simulator.Actor;
import itumulator.world.Location;
import itumulator.world.NonBlocking;
import itumulator.world.World;

public class Grass implements Actor, NonBlocking {
    private static final double SPREAD_CHANCE = 0.1; // 10% chance pr. skridt
    private final Random random = new Random();

    @Override
    public void act(World world) {
        if (random.nextDouble() >= SPREAD_CHANCE) {
            return;
        }

        Location here = world.getLocation(this);
        Set<Location> neighbours = world.getSurroundingTiles(here);

        List<Location> free = new ArrayList<>();
        for (Location l : neighbours) {
            if (!world.containsNonBlocking(l)) {
                free.add(l);
            }
        }

        if (free.isEmpty()) {
            return;
        }

        Location target = free.get(random.nextInt(free.size()));
        world.setTile(target, new Grass());
    }
}

