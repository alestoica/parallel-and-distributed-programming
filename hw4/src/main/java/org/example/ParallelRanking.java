package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ParallelRanking {
    private static final int p = 4;
    private static final int NUM_READERS = 1;
    private static final int NUM_WORKERS = p - NUM_READERS;

    public static void main(String[] args) throws IOException {
        long startTime = System.nanoTime();

        Queue queue = new Queue();
        LinkedList ranking = new LinkedList();
        Set<Integer> blacklist = Collections.synchronizedSet(new HashSet<>());

        String[] countries = {"c1", "c2", "c3", "c4", "c5"};
        Thread[] readers = new Thread[NUM_READERS];
        Thread[] workers = new Thread[NUM_WORKERS];

        for (int i = 0; i < NUM_READERS; i++) {
            int readerId = i;
            readers[i] = new Thread(() -> {
                try {
                    for (int countryIndex = readerId; countryIndex < countries.length; countryIndex += NUM_READERS) {
                        String country = countries[countryIndex];
                        for (int problem = 1; problem <= 10; problem++) {
                            String filename = "input/results_" + country + "_p" + problem + ".txt";
                            try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
                                String line;
                                while ((line = br.readLine()) != null) {
                                    String[] parts = line.split(" ");
                                    int id = Integer.parseInt(parts[0]);
                                    int score = Integer.parseInt(parts[1]);

                                    queue.enqueue(id, score);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    queue.setFinished();
                }
            });
        }

        for (int i = 0; i < NUM_WORKERS; i++) {
            workers[i] = new Thread(() -> {
                while (!queue.isFinished()) {
                    Pair pair = queue.dequeue();
                    if (pair == null) {
                        break;
                    }

                    int id = pair.id;
                    int score = pair.score;

                    if (blacklist.contains(id)) continue;

                    synchronized (ranking) {
                        if (score == -1) {
                            blacklist.add(id);
                            ranking.remove(id);
                        } else {
                            ranking.add(id, score);
                        }
                    }
                }
            });
        }

        for (Thread reader : readers) reader.start();
        for (Thread worker : workers) worker.start();

        for (Thread reader : readers) {
            try {
                reader.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        ranking.writeToFile("output/ranking_parallel.txt");
        System.out.println("Blacklist: " + String.join(", ", blacklist.stream()
                .map(String::valueOf)
                .toArray(String[]::new)));

        long endTime = System.nanoTime(); // End timing
        System.out.println("Parallel execution time (p = " + p + ", p_r = " + NUM_READERS + ") (ms): " +
                (endTime - startTime) / 1_000_000.00);
    }
}
