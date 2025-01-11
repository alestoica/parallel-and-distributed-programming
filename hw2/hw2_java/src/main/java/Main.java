import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CyclicBarrier;

public class Main {
    static int p = 4;
    static CyclicBarrier barrier = new CyclicBarrier(p);

    static void generateMatrices(int N, int M, int k) {
        Random random = new Random();

        try (FileWriter writer = new FileWriter("input.txt")) {
            writer.write(N + " " + M + " " + k + "\n");
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < M; j++) {
                    writer.write(random.nextInt(10) + " ");
                }
                writer.write("\n");
            }

            for (int i = 0; i < k; i++) {
                for (int j = 0; j < k; j++) {
                    writer.write(random.nextInt(2) + " ");
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


    public static void main(String[] args) throws FileNotFoundException {
//        p = Integer.parseInt(args[0]);
        File inputFile = new File("input.txt");

        generateMatrices(10, 10, 3);

        Scanner reader = new Scanner(inputFile);

        int N = reader.nextInt();
        int M = reader.nextInt();
        int k = reader.nextInt();

        int[][] F = new int[N][M];
        int[][] C = new int[k][k];

        readMatrix(reader, F, N, M);
        readMatrix(reader, C, k, k);

        // Convolution serial with constraints
        var t1 = System.nanoTime();
        convolveInPlaceSerial(F, C, N, M, k);
        var t2 = System.nanoTime();
        var time = t2 - t1;
        System.out.println("Serial time: " + (double) time / 1000000);
        writeMatrix("outputSerial.txt", F, N, M);

        // Reload the initial matrix from input
        reader = new Scanner(inputFile);
        reader.nextInt();
        reader.nextInt();
        reader.nextInt();
        readMatrix(reader, F, N, M);

        // Convolution parallel with constraints
        t1 = System.nanoTime();
        convolveInPlaceParallel(F, C, p);
        t2 = System.nanoTime();
        time = t2 - t1;
        System.out.println("Parallel time: " + (double) time / 1000000);
        writeMatrix("outputParallel.txt", F, N, M);

        inputFile = new File("outputSerial.txt");
        reader = new Scanner(inputFile);
        int[][] VSerial = new int[N][M];
        readMatrix(reader, VSerial, N, M);

        inputFile = new File("outputParallel.txt");
        reader = new Scanner(inputFile);
        int[][] VParallel = new int[N][M];
        readMatrix(reader, VParallel, N, M);

        assertMatricesEqual(VSerial, VParallel);
    }

    public static void convolveInPlaceSerial(int[][] F, int[][] C, int N, int M, int k) {
        int[] previousRow = new int[M];
        int[] currentRow = new int[M];

        int currentElem = 0, lastRowElem = 0;

        System.arraycopy(F[0], 0, previousRow, 0, M);
        System.arraycopy(F[0], 0, currentRow, 0, M);

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                int sum = 0;

                if (i == N - 1)
                    currentElem = F[i][j];

                sum += previousRow[Math.max(j - 1, 0)] * C[0][0] +
                        previousRow[j] * C[0][1] +
                        previousRow[Math.min(M - 1, j + 1)] * C[0][2];

                sum += currentRow[Math.max(j - 1, 0)] * C[1][0] +
                        currentRow[j] * C[1][1] +
                        currentRow[Math.min(M - 1, j + 1)] * C[1][2];

                if (i == N - 1 && j > 0) {
                    sum += lastRowElem * C[2][0] +
                            F[Math.min(i + 1, N - 1)][j] * C[2][1] +
                            F[Math.min(i + 1, N - 1)][Math.min(M - 1, j + 1)] * C[2][2];
                } else {
                    sum += F[Math.min(i + 1, N - 1)][Math.max(j - 1, 0)] * C[2][0] +
                            F[Math.min(i + 1, N - 1)][j] * C[2][1] +
                            F[Math.min(i + 1, N - 1)][Math.min(M - 1, j + 1)] * C[2][2];
                }

                F[i][j] = sum;

                if (i == N - 1)
                    lastRowElem = currentElem;
            }

            System.arraycopy(currentRow, 0, previousRow, 0, M);
            System.arraycopy(F[Math.min(i + 1, N - 1)], 0, currentRow, 0, M);
        }
    }

    public static void convolveInPlaceParallel(int[][] F, int[][] C, int p) {
        int n = F.length;
        int m = F[0].length;

        int rowsPerThread = n / p;
        int remainder = n % p;

        barrier = new CyclicBarrier(p);
        Thread[] threads = new Thread[p];

        int start = 0;

        for (int i = 0; i < p; i++) {
            int end = start + rowsPerThread + (remainder > 0 ? 1 : 0);
            if (remainder > 0) remainder--;

            threads[i] = new Thread(new ConvolutionTask(F, C, start, end, barrier, i));

            threads[i].start();
            start = end;
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void assertMatricesEqual(int[][] VSerial, int[][] VParallel) {
        if (VSerial.length != VParallel.length) {
            throw new AssertionError("Matrices do not have the same number of rows.");
        }

        for (int i = 0; i < VSerial.length; i++) {
            if (VSerial[i].length != VParallel[i].length) {
                throw new AssertionError("Matrices do not have the same number of columns.");
            }

            for (int j = 0; j < VSerial[i].length; j++) {
                if (VSerial[i][j] != VParallel[i][j]) {
                    throw new AssertionError("Mismatch found at row " + i + " column " + j + " between VSerial and VParallel: " + VSerial[i][j] + " != " + VParallel[i][j]);
                }
            }
        }

        System.out.println("The matrices are equal.");
    }
}