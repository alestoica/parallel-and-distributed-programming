package org.example.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileComparator {
    public static void assertResults(String file1, String file2) {
        try (
                BufferedReader reader1 = new BufferedReader(new FileReader(file1));
                BufferedReader reader2 = new BufferedReader(new FileReader(file2));
        ) {
            String line1;
            String line2;
            int lineNumber = 1;

            while (true) {
                line1 = reader1.readLine();
                line2 = reader2.readLine();

                if (line1 == null && line2 == null) {
                    // End of both files
                    System.out.println("Results match!");
                    System.out.println();
                    break;
                }

                if (line1 == null || !line1.equals(line2)) {
                    // Difference found or one file has more lines
//                    System.out.println("Results are different!");
//                    System.out.println("Difference found at line: " + lineNumber);
//                    if (line1 != null) {
//                        System.out.println("File 1: " + line1);
//                    }
//                    if (line2 != null) {
//                        System.out.println("File 2: " + line2);
//                    }
//                    break;
                }

                lineNumber++;
            }
        } catch (IOException e) {
            System.err.println("Error reading files: " + e.getMessage());
        }
    }
}

