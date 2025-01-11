import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

public class Main {
    final static int p = 16;

    public static void main(String[] args) throws FileNotFoundException {
        File inputFile = new File("input.txt");

        generateMatrices(1000, 1000, 5, 5);

        Scanner reader = new Scanner(inputFile);

        int N = reader.nextInt();
        int M = reader.nextInt();
        int n = reader.nextInt();
        int m = reader.nextInt();

        int[][] F = new int[N][M];
        int[][] C = new int[n][m];

        readMatrix(reader, F, N, M);
        readMatrix(reader, C, n, m);

        // Convolution serial
        var t1 = System.nanoTime();
        int[][] VSerial = convolveSerial(F, C, N, M, n);
        var t2 = System.nanoTime();
        var time = t2 - t1;
        System.out.println("Serial time: " + (double) time / 1000000);
        writeMatrix("outputSerial.txt", VSerial, N, M);

        // Convolution parallel (horizontal)
        t1 = System.nanoTime();
        int[][] VParallelHorizontal = convolveParallelHorizontal(F, C, p);
        t2 = System.nanoTime();
        time = t2 - t1;
        System.out.println("Parallel Horizontal time: " + (double) time / 1000000);
        writeMatrix("outputParallelHorizontal.txt", VParallelHorizontal, N, M);

        // Convolution parallel (vertical)
        t1 = System.nanoTime();
        int[][] VParallelVertical = convolveParallelVertical(F, C, p);
        t2 = System.nanoTime();
        time = t2 - t1;
        System.out.println("Parallel Vertical time: " + (double) time / 1000000);
        writeMatrix("outputParallelVertical.txt", VParallelVertical, N, M);

        assertMatricesEqual(VSerial, VParallelHorizontal, VParallelVertical);
    }

    static void generateMatrices(int N, int M, int n, int m) {
        Random random = new Random();

        try (FileWriter writer = new FileWriter("input.txt")) {
            writer.write(N + " " + M + " " + n + " " + m + "\n");
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < M; j++) {
                    writer.write(random.nextInt(100) + " ");
                }
                writer.write("\n");
            }

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    writer.write(random.nextInt(3) + " ");
                }
                writer.write("\n");
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void readMatrix(Scanner reader, int[][] matrix, int rows, int cols) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (reader.hasNextInt()) {
                    matrix[i][j] = reader.nextInt();
                }
            }
        }
    }

    public static void writeMatrix(String filename, int[][] matrix, int rows, int cols) {
        try (FileWriter writer = new FileWriter(filename)) {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    writer.write(matrix[i][j] + " ");
                }
                writer.write("\n");
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static int[][] convolveSerial(int[][] F, int[][] C, int n, int m, int k) {
        int[][] V = new int[n][m];
        int offset = k / 2;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                int sum = 0;
                for (int x = 0; x < k; x++) {
                    for (int y = 0; y < k; y++) {
                        int u = Math.min(Math.max(i + x - offset, 0), n - 1);
                        int v = Math.min(Math.max(j + y - offset, 0), m - 1);
                        sum += F[u][v] * C[x][y];
                    }
                }
                V[i][j] = sum;
            }
        }

        return V;
    }

    static int[][] convolveParallelHorizontal(int[][] F, int[][] C, int p) {
        int n = F.length;
        int m = F[0].length;
        int[][] V = new int[n][m];
        Thread[] threads = new Thread[p];
        int rowsPerThread = n / p;
        int remainder = n % p;

        int start = 0;

        for (int i = 0; i < p; i++) {
            int end = start + rowsPerThread + (remainder > 0 ? 1 : 0);
            if (remainder > 0) remainder--;

            threads[i] = new Thread(new ConvolutionTask(F, C, V, start, end, 0, m));
            threads[i].start();
            start = end;
        }

        for (int i = 0; i < p; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return V;
    }

    static int[][] convolveParallelVertical(int[][] F, int[][] C, int p) {
        int n = F.length;
        int m = F[0].length;
        int[][] V = new int[n][m];
        Thread[] threads = new Thread[p];
        int colsPerThread = m / p;
        int remainder = m % p;

        int start = 0;

        for (int i = 0; i < p; i++) {
            int end = start + colsPerThread + (remainder > 0 ? 1 : 0);
            if (remainder > 0) remainder--;

            threads[i] = new Thread(new ConvolutionTask(F, C, V, 0, n, start, end));
            threads[i].start();
            start = end;
        }

        for (int i = 0; i < p; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return V;
    }

    public static void assertMatricesEqual(int[][] VSerial, int[][] VParallelHorizontal, int[][] VParallelVertical) {
        if (VSerial.length != VParallelHorizontal.length || VSerial.length != VParallelVertical.length) {
            throw new AssertionError("Matrices do not have the same number of rows.");
        }

        for (int i = 0; i < VSerial.length; i++) {
            if (VSerial[i].length != VParallelHorizontal[i].length || VSerial[i].length != VParallelVertical[i].length) {
                throw new AssertionError("Matrices do not have the same number of columns.");
            }

            for (int j = 0; j < VSerial[i].length; j++) {
                if (VSerial[i][j] != VParallelHorizontal[i][j]) {
                    throw new AssertionError("Mismatch found at row " + i + " column " + j + " between VSerial and VParallelHorizontal: " + VSerial[i][j] + " != " + VParallelHorizontal[i][j]);
                }

                if (VSerial[i][j] != VParallelVertical[i][j]) {
                    throw new AssertionError("Mismatch found at row " + i + " column " + j + " between VSerial and VParallelVertical: " + VSerial[i][j] + " != " + VParallelVertical[i][j]);
                }
            }
        }

        System.out.println("All matrices are equal.");
    }
}
