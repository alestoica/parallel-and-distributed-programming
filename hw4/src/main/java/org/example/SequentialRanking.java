package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SequentialRanking {
    public static void main(String[] args) throws IOException {
        long startTime = System.nanoTime();

        LinkedList ranking = new LinkedList();
        Set<Integer> blacklist = new HashSet<>();
        String[] countries = {"c1", "c2", "c3", "c4", "c5"};

        for (String country : countries) {
            for (int i = 1; i <= 10; i++) {
                String filename = "input/results_" + country + "_p" + i + ".txt";
                try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split(" ");
                        int id = Integer.parseInt(parts[0]);
                        int score = Integer.parseInt(parts[1]);

                        if (blacklist.contains(id)) continue;

                        if (score == -1) {
                            blacklist.add(id);
                            ranking.remove(id);
                        } else {
                            ranking.add(id, score);
                        }
                    }
                }
            }
        }

        ranking.writeToFile("output/ranking_sequential.txt");
        System.out.println("Blacklist: " + String.join(", ", blacklist.stream()
                .map(String::valueOf)
                .toArray(String[]::new)));

        long endTime = System.nanoTime();
        System.out.println("Sequential execution time (ms): " + (endTime - startTime) / 1_000_000.00);
    }
}

