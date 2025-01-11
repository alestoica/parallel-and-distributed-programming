#include <iostream>
#include <fstream>
#include <thread>
#include <vector>
#include <cmath>
#include <chrono>

using namespace std;

const int MAX_N = 10005;
const int MAX_M = 15;

void readMatrixStatic(ifstream &file, int matrix[MAX_N][MAX_M], int rows, int cols) {
    for (int i = 0; i < rows; ++i) {
        for (int j = 0; j < cols; ++j) {
            file >> matrix[i][j];
        }
    }
}

void writeMatrixStatic(const string &filename, int matrix[MAX_N][MAX_M], int rows, int cols) {
    ofstream file(filename);
    for (int i = 0; i < rows; ++i) {
        for (int j = 0; j < cols; ++j) {
            file << matrix[i][j] << " ";
        }
        file << "\n";
    }
}

void convolveSerialStatic(int F[MAX_N][MAX_M], int C[MAX_N][MAX_M], int V[MAX_N][MAX_M], int N, int M, int k) {
    int offset = k / 2;

    for (int i = 0; i < N; ++i) {
        for (int j = 0; j < M; ++j) {
            int sum = 0;
            for (int x = 0; x < k; ++x) {
                for (int y = 0; y < k; ++y) {
                    int u = min(max(i + x - offset, 0), N - 1);
                    int v = min(max(j + y - offset, 0), M - 1);
                    sum += F[u][v] * C[x][y];
                }
            }
            V[i][j] = sum;
        }
    }
}

void
convolveHorizontalStatic(int F[MAX_N][MAX_M], int C[MAX_N][MAX_M], int V[MAX_N][MAX_M], int startRow, int endRow,
                         int rows,
                         int cols, int k) {
    int offset = k / 2;

    for (int i = startRow; i < endRow; ++i) {
        for (int j = 0; j < cols; ++j) {
            int sum = 0;
            for (int x = 0; x < k; ++x) {
                for (int y = 0; y < k; ++y) {
                    int u = min(max(i + x - offset, 0), rows - 1);
                    int v = min(max(j + y - offset, 0), cols - 1);
                    sum += F[u][v] * C[x][y];
                }
            }
            V[i][j] = sum;
        }
    }
}

void convolveVerticalStatic(int F[MAX_N][MAX_M], int C[MAX_N][MAX_M], int V[MAX_N][MAX_M], int startCol, int endCol,
                            int rows,
                            int cols, int k) {
    int offset = k / 2;

    for (int i = 0; i < rows; ++i) {
        for (int j = startCol; j < endCol; ++j) {
            int sum = 0;
            for (int x = 0; x < k; ++x) {
                for (int y = 0; y < k; ++y) {
                    int u = min(max(i + x - offset, 0), rows - 1);
                    int v = min(max(j + y - offset, 0), cols - 1);
                    sum += F[u][v] * C[x][y];
                }
            }
            V[i][j] = sum;
        }
    }
}

void assertMatricesEqual(int (&VSerial)[MAX_N][MAX_M], int (&VParallelHorizontal)[MAX_N][MAX_M],
                         int (&VParallelVertical)[MAX_N][MAX_M], int rows, int cols) {
    for (int i = 0; i < rows; ++i) {
        for (int j = 0; j < cols; ++j) {
            if (VSerial[i][j] != VParallelHorizontal[i][j]) {
                throw std::runtime_error("Mismatch found at row " + std::to_string(i) + " column " + std::to_string(j) +
                                         " between VSerial and VParallelHorizontal: " + std::to_string(VSerial[i][j]) +
                                         " != " + std::to_string(VParallelHorizontal[i][j]));
            }

            if (VSerial[i][j] != VParallelVertical[i][j]) {
                throw std::runtime_error("Mismatch found at row " + std::to_string(i) + " column " + std::to_string(j) +
                                         " between VSerial and VParallelVertical: " + std::to_string(VSerial[i][j]) +
                                         " != " + std::to_string(VParallelVertical[i][j]));
            }
        }
    }

    std::cout << "All matrices are equal." << std::endl;
}

void solve_static() {
    ifstream inputFile("input.txt");

    int N, M, n, m;
    inputFile >> N >> M >> n >> m;

    int F[MAX_N][MAX_M];
    int C[MAX_N][MAX_M];
    int VSerial[MAX_N][MAX_M];
    int VParallelHorizontal[MAX_N][MAX_M];
    int VParallelVertical[MAX_N][MAX_M];
    int j = 16;

    readMatrixStatic(inputFile, F, N, M);
    readMatrixStatic(inputFile, C, n, m);

    auto t1 = chrono::high_resolution_clock::now();
    convolveSerialStatic(F, C, VSerial, N, M, n);
    auto t2 = chrono::high_resolution_clock::now();
    double serialTime = chrono::duration<double, milli>(t2 - t1).count();
    cout << "Serial Time: " << serialTime << " ms\n";
    writeMatrixStatic("outputSerial.txt", VSerial, N, M);

    int p = 4;
    vector<thread> threads;

    t1 = chrono::high_resolution_clock::now();
    int rowsPerThread = N / j;
    int remainder = N % j;
    int start = 0;
    for (int i = 0; i < j; ++i) {
        int end = start + rowsPerThread + (remainder-- > 0 ? 1 : 0);
        threads.emplace_back(convolveHorizontalStatic, F, C, VParallelHorizontal, start, end, N, M, n);
        start = end;
    }
    for (auto &th: threads) th.join();
    t2 = chrono::high_resolution_clock::now();
    double parallelHorizontalTime = chrono::duration<double, milli>(t2 - t1).count();
    cout << "Parallel Horizontal Time: " << parallelHorizontalTime << " ms\n";
    writeMatrixStatic("outputParallelHorizontal.txt", VParallelHorizontal, N, M);

    threads.clear();


    t1 = chrono::high_resolution_clock::now();
    int colsPerThread = M / j;
    remainder = M % j;
    start = 0;
    for (int i = 0; i < j; ++i) {
        int end = start + colsPerThread + (remainder-- > 0 ? 1 : 0);
        threads.emplace_back(convolveVerticalStatic, F, C, VParallelVertical, start, end, N, M, n);
        start = end;
    }
    for (auto &th: threads) th.join();
    t2 = chrono::high_resolution_clock::now();
    double parallelVerticalTime = chrono::duration<double, milli>(t2 - t1).count();
    cout << "Parallel Vertical Time: " << parallelVerticalTime << " ms\n";
    writeMatrixStatic("outputParallelVertical.txt", VParallelVertical, N, M);

    assertMatricesEqual(VSerial, VParallelHorizontal, VParallelVertical, N, M);
}

