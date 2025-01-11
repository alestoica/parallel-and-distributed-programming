package org.example;


import org.example.utils.ContestDataGenerator;
import org.example.utils.FileComparator;
import org.example.utils.SortPairsFromFile;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
//        ContestDataGenerator.main(args);
//        SequentialRanking.main(args);
        ParallelRanking.main(args);
//        FileComparator.main(args);
    }
}