import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class ConvolutionTask implements Runnable {
    int[][] F, C;
    int start, end;
    int N, M, K;
    CyclicBarrier barrier;
    int i;

    public ConvolutionTask(int[][] F, int[][] C, int start, int end, CyclicBarrier barrier, int i) {
        this.F = F;
        this.C = C;
        this.start = start;
        this.end = end;
        this.barrier = barrier;
        this.N = F.length;
        this.M = F[0].length;
        this.K = C.length;
        this.i = i;
    }

    @Override
    public void run() {
        try {
            int[] previousRow = new int[M];
            int[] currentRow = new int[M];
            int[] lastRow = new int[M];

            System.arraycopy(F[Math.max(start - 1, 0)], 0, previousRow, 0, M);
            System.arraycopy(F[start], 0, currentRow, 0, M);
            System.arraycopy(F[Math.min(end, N - 1)], 0, lastRow, 0, M);

            barrier.await();

            for (int r = start; r < end; r++) {
                for (int c = 0; c < M; c++) {
                    int sum = 0;

                    sum += previousRow[Math.max(c - 1, 0)] * C[0][0] +
                            previousRow[c] * C[0][1] +
                            previousRow[Math.min(M - 1, c + 1)] * C[0][2];

                    sum += currentRow[Math.max(c - 1, 0)] * C[1][0] +
                            currentRow[c] * C[1][1] +
                            currentRow[Math.min(M - 1, c + 1)] * C[1][2];

                    if (r == end - 1) {
                        sum += lastRow[Math.max(c - 1, 0)] * C[2][0] +
                                lastRow[c] * C[2][1] +
                                lastRow[Math.min(M - 1, c + 1)] * C[2][2];
                    } else {
                        sum += F[r + 1][Math.max(c - 1, 0)] * C[2][0] +
                                F[r + 1][c] * C[2][1] +
                                F[r + 1][Math.min(M - 1, c + 1)] * C[2][2];
                    }

                    F[r][c] = sum;
                }

                System.arraycopy(currentRow, 0, previousRow, 0, M);
                System.arraycopy(F[Math.min(r + 1, N - 1)], 0, currentRow, 0, M);
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}
