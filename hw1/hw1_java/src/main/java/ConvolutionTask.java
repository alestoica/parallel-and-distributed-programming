public class ConvolutionTask implements Runnable {
    int[][] F, C, V;
    int rowStart, rowEnd, colStart, colEnd;
    int k, offset;

    public ConvolutionTask(int[][] F, int[][] C, int[][] V, int rowStart, int rowEnd, int colStart, int colEnd) {
        this.F = F;
        this.C = C;
        this.V = V;
        this.rowStart = rowStart;
        this.rowEnd = rowEnd;
        this.colStart = colStart;
        this.colEnd = colEnd;
        this.k = C.length;
        this.offset = k / 2;
    }

    @Override
    public void run() {
        int n = F.length, m = F[0].length;

        for (int i = rowStart; i < rowEnd; i++) {
            for (int j = colStart; j < colEnd; j++) {
                int sum = 0;
                for (int u = 0; u < k; u++) {
                    for (int v = 0; v < k; v++) {
                        int x = Math.min(Math.max(i + u - offset, 0), n - 1);
                        int y = Math.min(Math.max(j + v - offset, 0), m - 1);
                        sum += F[x][y] * C[u][v];
                    }
                }
                V[i][j] = sum;
            }
        }
    }
}
