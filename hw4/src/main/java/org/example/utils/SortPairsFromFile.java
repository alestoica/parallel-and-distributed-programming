package org.example.utils;

import java.io.*;
import java.util.*;

public class SortPairsFromFile {
    public static void main(String fileName) {
        List<int[]> pairs = readPairsFromFile(fileName);

        if (pairs != null) {
            // Sort primarily by the second element, then by the first element if the second elements are equal
            pairs.sort((pair1, pair2) -> {
                if (pair1[1] == pair2[1]) {
                    return Integer.compare(pair1[0], pair2[0]); // Sort by the first element if second elements are equal
                }
                return Integer.compare(pair1[1], pair2[1]); // Default sorting by the second element
            });

            // Write sorted pairs back to the file
            writePairsToFile(fileName, pairs);
        }
    }

    // Reads pairs from the file
    private static List<int[]> readPairsFromFile(String fileName) {
        List<int[]> pairs = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("(") && line.endsWith(")")) {
                    String[] numbers = line.substring(1, line.length() - 1).split(",");
                    int first = Integer.parseInt(numbers[0].trim());
                    int second = Integer.parseInt(numbers[1].trim());
                    pairs.add(new int[]{first, second});
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error reading the file: " + e.getMessage());
            return null;
        }
        return pairs;
    }

    // Writes sorted pairs back to the file
    private static void writePairsToFile(String fileName, List<int[]> pairs) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            for (int[] pair : pairs) {
                bw.write("(" + pair[0] + ", " + pair[1] + ")");
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing to the file: " + e.getMessage());
        }
    }
}
