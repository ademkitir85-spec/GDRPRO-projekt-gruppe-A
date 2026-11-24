import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import itumulator.executable.DisplayInformation;
import itumulator.executable.Program;
import itumulator.world.Location;
import itumulator.world.World;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        // Find alle .txt-filer i data-mappen
        File dataFolder = new File("data");
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".txt"));

        if (files == null || files.length == 0) {
            System.out.println("Fandt ingen .txt-filer i: " + dataFolder.getAbsolutePath());
            return;
        }

        // Vis filerne som en nummereret liste
        Arrays.sort(files);
        for (int i = 0; i < files.length; i++) {
            System.out.println(i + ": " + files[i].getName());
        }

        // Lad brugeren vælge en fil
        System.out.print("Vælg fil (skriv nummeret): ");
        Scanner input = new Scanner(System.in);
        int choice = input.nextInt();

        // Læs den valgte fil
        Scanner sc = new Scanner(files[choice]);
        int size = Integer.parseInt(sc.nextLine().trim()); // første linje = størrelsen
        Program p = new Program(size, 800, 75);

        World w = p.getWorld();

        // Læs resten af filen og placér tingene tilfældigt
        Random r = new Random();
        while (sc.hasNextLine()) {
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");   // fx ["rabbit", "3-7"]
            String type = parts[0].toLowerCase();
            int amount;
            if (parts[1].contains("-")) {
                String[] mm = parts[1].split("-");
                int min = Integer.parseInt(mm[0]);
                int max = Integer.parseInt(mm[1]);
                amount = min + r.nextInt(max - min + 1);
            } else {
                amount = Integer.parseInt(parts[1]);
            }

            for (int i = 0; i < amount; i++) {
                Location l;
                if (type.equals("grass")) {
                    do {
                        l = new Location(r.nextInt(size), r.nextInt(size));
                    } while (w.containsNonBlocking(l));
                    w.setTile(l, new Grass());
                } else if (type.equals("rabbit")) {
                    do {
                        l = new Location(r.nextInt(size), r.nextInt(size));
                    } while (!w.isTileEmpty(l));
                    w.setTile(l, new Rabbit());
                } else if (type.equals("burrow")) {
                    do {
                        l = new Location(r.nextInt(size), r.nextInt(size));
                    } while (w.containsNonBlocking(l));
                    w.setTile(l, new Burrow());
                } else {
                    System.out.println("Ukendt type i filen: " + type);
                    break;
                }
            }
        }
        sc.close();

        p.setDisplayInformation(Grass.class, new DisplayInformation(Color.green, "grass"));
        p.setDisplayInformation(Rabbit.class, new DisplayInformation(Color.gray, "rabbit-large"));
        p.setDisplayInformation(Burrow.class, new DisplayInformation(Color.black, "hole"));

        p.show();
    }
}