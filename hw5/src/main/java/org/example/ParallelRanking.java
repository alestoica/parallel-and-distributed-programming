package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ParallelRanking {
    private static final int p = 16;
    private static final int NUM_READERS = 4;
    private static final int NUM_WORKERS = p - NUM_READERS;

    public static void main(String[] args) throws IOException {
        long startTime = System.nanoTime();

        Queue queue = new Queue(100);
        LinkedList ranking = new LinkedList();
        Blacklist blacklist = new Blacklist();

        String[] countries = {"c1", "c2", "c3", "c4", "c5"};
        ExecutorService readerPool = Executors.newFixedThreadPool(NUM_READERS);

        for (String country : countries) {
            for (int i = 1; i <= 10; i++) {
                String fileName = "input/results_" + country + "_p" + i + ".txt";
                readerPool.execute(() -> {
                    try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            String[] parts = line.split(" ");
                            int id = Integer.parseInt(parts[0]);
                            int score = Integer.parseInt(parts[1]);
                            queue.enqueue(id, score, country);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        readerPool.shutdown();
        new Thread(() -> {
            try {
                readerPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            queue.setFinished();
        }).start();

        Thread[] workers = new Thread[NUM_WORKERS];
        for (int i = 0; i < NUM_WORKERS; i++) {
            workers[i] = new Thread(() -> {
                while (!queue.isFinished()) {
                    Pair pair = queue.dequeue();
                    if (pair == null) continue;

                    if (blacklist.contains(pair.id)) continue;

                    if (pair.score == -1)
                        blacklist.add(pair.id);


                    ranking.addOrUpdate(pair.id, pair.score, pair.country);
                }
            });
            workers[i].start();
        }

        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        ranking.writeToFile("output/ranking_parallel.txt");
        System.out.print("Blacklist: ");
        blacklist.print();

        long endTime = System.nanoTime();
        System.out.println("Execution time: " + (endTime - startTime) / 1_000_000.0 + " ms");
    }
}

