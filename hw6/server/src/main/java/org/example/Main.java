package org.example;

import org.example.datastructures.LinkedList;
import org.example.datastructures.Queue;
import org.example.domain.Participant;
import org.example.request.Request;
import org.example.datastructures.Blacklist;
import org.example.request.RequestType;
import org.example.response.CountryResult;
import org.example.response.Response;
import org.example.response.ResponseType;
import org.example.utils.FileComparator;
import org.example.utils.SortPairsFromFile;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Main {
    private static final int PORT = 50000;

    private static final int readers = 4;
    private static final int writers = 8;
    private static final ExecutorService executor = Executors.newFixedThreadPool(readers); // thread pool for readers
    private static final Map<Integer, ReentrantLock> access = new HashMap<>(); // individual locks for each country

    private static final int dt = 4;

    private static final AtomicInteger noCountriesLeft = new AtomicInteger(5); // there are 5 countries in total - for partial ranking
    private static final AtomicInteger noCountriesLeftFinalResult = new AtomicInteger(5); // for final ranking
    private static final Map<String, Boolean> finishedCountries = new HashMap<>(); // countries that finished sending data

    private static final LinkedList ranking = new LinkedList();
    private static final Queue queue = new Queue(noCountriesLeft);
    private static final Blacklist blackList = new Blacklist();

    public static void main(String[] args) throws IOException {
        for (int i = 0; i < 1000; ++i) {
            access.put(i, new ReentrantLock());
        }

        Thread[] writerThreads = new Thread[writers];
        for (int i = 0; i < writers; ++i) {
            Thread thread = new Writer();
            writerThreads[i] = thread;
        }

        long start = System.currentTimeMillis();
        Arrays.stream(writerThreads).forEach(Thread::start);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started, listening on port " + PORT);
            System.out.println();

            while (noCountriesLeftFinalResult.get() != 0) {
                try {
                    final Socket clientSocket = serverSocket.accept();
                    executor.submit(new Reader(clientSocket)); // handling each client in a separate thread
                    Thread.sleep(500);
                } catch (IOException | InterruptedException e) {
                    System.err.println("Exception caught when trying to listen on port " + PORT + " or listening for a connection");
                    System.err.println(e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + PORT);
            System.err.println(e.getMessage());
        }

        System.out.println();

        Arrays.stream(writerThreads).forEach(thread -> {
            try {
                thread.join();
                System.out.println("Joined 1 writer thread");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println();

        blackList.getItemsAsList().forEach(item -> ranking.add(new Participant(item.getId(), item.getScore(), item.getCountry())));

        ranking.writeToFile("ranking.txt");

        Map<String, Integer> countryResults = new HashMap<>();
        ranking.getItemsAsList().forEach(participant -> {
            countryResults.merge(participant.getCountry(), participant.getScore(), Integer::sum);
        });

        Map<String, Integer> sortedCountryResults = countryResults.entrySet()
                .stream()
                .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        printMapToFile(sortedCountryResults, "country_ranking.txt");

        SortPairsFromFile.main("server/src/main/resources/output/ranking.txt");
        FileComparator.assertResults("server/src/main/resources/valid_ranking.txt", "server/src/main/resources/output/ranking.txt");

        System.out.print("Blacklist: ");
        blackList.print();

        System.out.println();

        long end = System.currentTimeMillis();
        System.out.println("Execution time: " + (end - start));
    }

    private static void printMapToFile(Map<String, Integer> countryResult, String filename) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("server/src/main/resources/output/" + filename));

            for (String key : countryResult.keySet()) {
                writer.write("country: " + key + " - score: " + countryResult.get(key));
                writer.newLine();
            }

            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Writer extends Thread {
        @Override
        public void run() {
            while (noCountriesLeft.get() != 0 || !queue.isFinished()) {
                Participant participant = queue.dequeue();

                if (participant == null)
                    continue;

                access.get(participant.getId()).lock();

                try {
                    if (blackList.contains(participant.getId())) continue;

                    if (participant.getScore() == -1)
                        blackList.add(participant.getId(), participant.getCountry());

                    ranking.addOrUpdate(participant);

                } finally {
                    access.get(participant.getId()).unlock();
                }
            }
        }
    }

    public static class Reader implements Runnable {
        private final Socket clientSocket;

        public Reader(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                 ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

                Request request = (Request) in.readObject();

                try {
                    if (request.getRequestType() == RequestType.SCORE_UPDATE) {
                        var data = request.getResults();

                        data.forEach(result -> {
                            var id = result.getId();
                            var country = request.getCountry();
                            var score = result.getScore();
                            var participant = new Participant(id, score, country);
                            queue.enqueue(participant);
                        });

                        System.out.println("Batch of " + data.size() + " results from country " + request.getCountry() + " was processed");
                        out.writeObject(new Response(ResponseType.SUCCESS, null));
                        out.flush();
                    } else if (request.getRequestType() == RequestType.PARTIAL_RANKING) {
                        var country = request.getCountry();
                        System.out.println("Request for partial ranking received from country " + country);
                        long start = 0, end = dt;

                        while (end - start >= dt) {
                            start = System.currentTimeMillis();

                            Future<Map<String, Integer>> partialFutureResult = executor.submit(() -> {
                                Map<String, Integer> result = new HashMap<>();
                                ranking.getItemsAsList().forEach(participant -> {
                                    result.merge(participant.getCountry(), participant.getScore(), Integer::sum);
                                });
                                return result;
                            });

                            Map<String, Integer> partialResult = partialFutureResult.get();
                            List<CountryResult> partialRanking = new ArrayList<>();

                            for (Map.Entry<String, Integer> entry : partialResult.entrySet()) {
                                partialRanking.add(new CountryResult(entry.getKey(), entry.getValue()));
                            }

                            end = System.currentTimeMillis();

                            if (end - start < dt) {
                                out.writeObject(new Response(ResponseType.SUCCESS, partialRanking));
                                out.flush();
                                System.out.println("Partial ranking sent to country " + country);
                                break;
                            }
                        }
                    } else if (request.getRequestType() == RequestType.FINAL_RANKING) {
                        var country = request.getCountry();
                        System.out.println("Request for final ranking received from country " + country);

                        if (finishedCountries.get(country) == null) {
                            noCountriesLeft.decrementAndGet();
                            finishedCountries.put(country, true);
                        }

                        if (noCountriesLeft.get() == 0) {
                            noCountriesLeftFinalResult.decrementAndGet();
                            Future<Map<String, Integer>> finalFutureResult = executor.submit(() -> {
                                Map<String, Integer> result = new HashMap<>();
                                ranking.getItemsAsList().forEach(participant -> {
                                    result.merge(participant.getCountry(), participant.getScore(), Integer::sum);
                                });
                                return result;
                            });

                            Map<String, Integer> finalResult = finalFutureResult.get();
                            List<CountryResult> finalRanking = new ArrayList<>();

                            for (Map.Entry<String, Integer> entry : finalResult.entrySet()) {
                                finalRanking.add(new CountryResult(entry.getKey(), entry.getValue()));
                            }

                            out.writeObject(new Response(ResponseType.SUCCESS, finalRanking));
                            out.flush();
                            queue.setFinished();
                            System.out.println("Final ranking sent to country " + country);
                        } else {
                            out.writeObject(new Response(ResponseType.FAILURE, null));
                            out.flush();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    out.writeObject(new Response(ResponseType.FAILURE, null));
                    out.flush();
                }

                clientSocket.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}