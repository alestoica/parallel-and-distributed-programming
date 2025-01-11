package org.example;

import org.example.request.Request;
import org.example.request.RequestType;
import org.example.request.Result;
import org.example.response.Response;
import org.example.response.ResponseType;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final int dx = 2;
    private static final int batch = 20;
    private static final List<String> files = new ArrayList<>();

    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        if (args.length < 1) {
            System.out.println("You have to provide the country code");
            System.exit(1);
        }

        var countryCode = Integer.parseInt(args[0]);
        generateFileNames(countryCode);
        List<Result> buffer = new ArrayList<>();

        for (String fileName : files) {
            File file = new File("client/src/main/resources/input/" + fileName);
            Scanner scanner = new Scanner(file);

            while (scanner.hasNextLine()) {
                String[] data = scanner.nextLine().split(" ");

                buffer.add(new Result(Integer.parseInt(data[0]), Integer.parseInt(data[1])));

                if (buffer.size() == batch) {
                    Request request = new Request(RequestType.SCORE_UPDATE, buffer, "c" + countryCode);
                    sendRequest(request);
                    buffer.clear();
                    Thread.sleep(dx * 1000);
                }
            }

            Request request = new Request(RequestType.PARTIAL_RANKING, null, "c" + countryCode);
            Response partialRankingResponse = sendRequest(request);
            if (partialRankingResponse != null) {
                var data = partialRankingResponse.getCountryResults();
                System.out.println("Partial Ranking: " + data);
            }
        }

        Request request = new Request(RequestType.FINAL_RANKING, null, "c" + countryCode);
        Response finalRankingResponse;
        int maxRetries = 5, retries = 0;

        do {
            finalRankingResponse = sendRequest(request);

            if (finalRankingResponse != null && finalRankingResponse.getResponseType() == ResponseType.SUCCESS) {
                break;
            }

            retries++;
            Thread.sleep(10000);
        } while (retries < maxRetries);

        if (finalRankingResponse != null && finalRankingResponse.getResponseType() == ResponseType.SUCCESS) {
            var data = finalRankingResponse.getCountryResults();
            System.out.println("Final Ranking: " + data);
        } else {
            System.out.println("Max retries reached for final ranking!");
        }
    }

    public static Response sendRequest(Request request) {
        try (Socket socket = new Socket("127.0.0.1", 50000);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(request);
            out.flush();

            return (Response) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static void generateFileNames(int countryCode) {
        for (int i = 1; i <= 10; ++i) {
            files.add("results_c" + countryCode + "_p" + i + ".txt");
        }
    }
}