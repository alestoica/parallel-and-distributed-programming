package org.example.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

public class ContestDataGenerator {
    private static final int NUM_COUNTRIES = 5;
    private static final int NUM_PROBLEMS = 10;
    private static final int MIN_CONTESTANTS = 80;
    private static final int MAX_CONTESTANTS = 100;
    private static final double PROBABILITY_NOT_SOLVED = 0.10;
    private static final double PROBABILITY_FRAUD = 0.02;
    private static final Random random = new Random();

    public static void main(String[] args) throws IOException {
        generateContestData();
    }

    private static void generateContestData() throws IOException {
        int currentId = 1;  // Initialize the ID counter

        for (int country = 1; country <= NUM_COUNTRIES; country++) {
            int numContestants = MIN_CONTESTANTS + random.nextInt(MAX_CONTESTANTS - MIN_CONTESTANTS + 1);

            Set<Integer> countryIds = new TreeSet<>();
            for (int i = 0; i < numContestants; i++) {
                countryIds.add(currentId++);
            }

            for (int problem = 1; problem <= NUM_PROBLEMS; problem++) {
                String filename = "input/results_c" + country + "_p" + problem + ".txt";
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
                    for (int id : countryIds) {
                        if (random.nextDouble() < PROBABILITY_NOT_SOLVED) {
                            continue;
                        }

                        int score = random.nextDouble() < PROBABILITY_FRAUD ? -1 : random.nextInt(10) + 1;
                        writer.write(id + " " + score);
                        writer.newLine();
                    }
                }
            }
        }
    }
}
