package org.example.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class SortPairsFromFile {
    public static void main(String fileName) {
        List<String[]> pairs = readPairsFromFile(fileName);

        if (pairs != null) {
            // Sort primarily by score (second element) in descending order, then by id (first element) in ascending order if scores are equal
            pairs.sort((pair1, pair2) -> {
                int scoreComparison = Integer.compare(Integer.parseInt(pair2[1]), Integer.parseInt(pair1[1])); // Sort by score desc
                if (scoreComparison == 0) {
                    return Integer.compare(Integer.parseInt(pair1[0]), Integer.parseInt(pair2[0])); // Sort by id asc if scores are equal
                }
                return scoreComparison;
            });

            // Write sorted pairs back to the file
            writePairsToFile(fileName, pairs);
        }
    }

    // Reads pairs from the file
    private static List<String[]> readPairsFromFile(String fileName) {
        List<String[]> pairs = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("(") && line.endsWith(")")) {
                    String[] values = line.substring(1, line.length() - 1).split(",");
                    String id = values[0].trim();
                    String score = values[1].trim();
                    String country = values[2].trim();
                    pairs.add(new String[]{id, score, country});
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
            return null;
        }
        return pairs;
    }

    // Writes sorted pairs back to the file
    private static void writePairsToFile(String fileName, List<String[]> pairs) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            for (String[] pair : pairs) {
                bw.write("(" + pair[0] + ", " + pair[1] + ", " + pair[2] + ")");
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to the file: " + e.getMessage());
        }
    }
}
