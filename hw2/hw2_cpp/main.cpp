#include <iostream>
#include <fstream>
#include <vector>
#include <cmath>
#include <chrono>
#include <barrier>
#include <mutex>

using namespace std;

void generateMatrices(int N, int M, int k) {
    ofstream writer(R"(C:\Users\alest\CLionProjects\ppd\hw2_cpp\cmake-build-debug\input.txt)");
    if (!writer) {
        cerr << "Error while opening the next file: " << "input.txt" << endl;
        return;
    }

    writer << N << " " << M << " " << k << "\n";

    ::srand(::time(0));
    for (int i = 0; i < N; i++) {
        for (int j = 0; j < M; j++) {
            int randomValue = rand() % 10;
            writer << randomValue << " ";
        }
        writer << "\n";
    }

    for (int i = 0; i < k; i++) {
        for (int j = 0; j < k; j++) {
            int randomValue = rand() % 2;
            writer << randomValue << " ";
        }
        writer << "\n";
    }

    writer.close();
}

int **readMatrix(ifstream &file, int rows, int cols) {
    int **matrix = new int *[rows];

    for (int i = 0; i < rows; ++i) {
        matrix[i] = new int[cols];

        for (int j = 0; j < cols; ++j) {
            file >> matrix[i][j];
        }
    }

    return matrix;
}

void writeMatrix(const string &filename, int **matrix, int rows, int cols) {
    ofstream file(filename);

    for (int i = 0; i < rows; ++i) {
        for (int j = 0; j < cols; ++j) {
            file << matrix[i][j] << " ";
        }
        file << "\n";
    }
}

void convolveInPlaceSerial(int **F, int **C, int N, int M, int k) {
    int *previousRow = new int[M];
    int *currentRow = new int[M];

    int currentElem = 0, lastRowElem = 0;

    std::copy(F[0], F[0] + M, previousRow);
    std::copy(F[0], F[0] + M, currentRow);

    for (int i = 0; i < N; ++i) {
        for (int j = 0; j < M; ++j) {
            int sum = 0;

            if (i == N - 1)
                currentElem = F[i][j];

            sum += previousRow[std::max(j - 1, 0)] * C[0][0] +
                   previousRow[j] * C[0][1] +
                   previousRow[std::min(M - 1, j + 1)] * C[0][2];

            sum += currentRow[std::max(j - 1, 0)] * C[1][0] +
                   currentRow[j] * C[1][1] +
                   currentRow[std::min(M - 1, j + 1)] * C[1][2];

            if (i == N - 1 && j > 0) {
                sum += lastRowElem * C[2][0] +
                       F[std::min(i + 1, N - 1)][j] * C[2][1] +
                       F[std::min(i + 1, N - 1)][std::min(M - 1, j + 1)] * C[2][2];
            } else {
                sum += F[std::min(i + 1, N - 1)][std::max(j - 1, 0)] * C[2][0] +
                       F[std::min(i + 1, N - 1)][j] * C[2][1] +
                       F[std::min(i + 1, N - 1)][std::min(M - 1, j + 1)] * C[2][2];
            }

            F[i][j] = sum;

            if (i == N - 1)
                lastRowElem = currentElem;
        }

        std::copy(currentRow, currentRow + M, previousRow);
        std::copy(F[std::min(i + 1, N - 1)], F[std::min(i + 1, N - 1)] + M, currentRow);
    }

    delete[] previousRow;
    delete[] currentRow;
}


void convolveInPlaceParallel(int **F, int **C, int start, int end, int N, int M, int k, barrier<> &barrier) {
    int *previousRow = new int[M];
    int *currentRow = new int[M];
    int *lastRow = new int[M];

    std::copy(F[std::max(start - 1, 0)], F[std::max(start - 1, 0)] + M, previousRow);
    std::copy(F[start], F[start] + M, currentRow);
    std::copy(F[std::min(end, N - 1)], F[std::min(end, N - 1)] + M, lastRow);

    barrier.arrive_and_wait();

    for (int r = start; r < end; ++r) {
        for (int c = 0; c < M; ++c) {
            int sum = 0;

            sum += previousRow[std::max(c - 1, 0)] * C[0][0] +
                   previousRow[c] * C[0][1] +
                   previousRow[std::min(M - 1, c + 1)] * C[0][2];

            sum += currentRow[std::max(c - 1, 0)] * C[1][0] +
                   currentRow[c] * C[1][1] +
                   currentRow[std::min(M - 1, c + 1)] * C[1][2];

            if (r == end - 1) {
                sum += lastRow[std::max(c - 1, 0)] * C[2][0] +
                       lastRow[c] * C[2][1] +
                       lastRow[std::min(M - 1, c + 1)] * C[2][2];
            } else {
                sum += F[r + 1][std::max(c - 1, 0)] * C[2][0] +
                       F[r + 1][c] * C[2][1] +
                       F[r + 1][std::min(M - 1, c + 1)] * C[2][2];
            }

            F[r][c] = sum;
        }

        std::copy(currentRow, currentRow + M, previousRow);
        std::copy(F[std::min(r + 1, N - 1)], F[std::min(r + 1, N - 1)] + M, currentRow);
    }

    delete[] previousRow;
    delete[] currentRow;
    delete[] lastRow;
}

void assertMatricesEqual(int **VSerial, int **VParallel, int N, int M) {
    for (int i = 0; i < N; ++i) {
        for (int j = 0; j < M; ++j) {
            if (VSerial[i][j] != VParallel[i][j]) {
                std::cerr << "Mismatch found at row " << i << " column " << j << " between VSerial and VParallel: "
                          << VSerial[i][j] << " != " << VParallel[i][j] << std::endl;
                return;
            }
        }
    }

    std::cout << "The matrices are equal." << std::endl;
}

int main(int argc, char *argv[]) {
    if (argc < 2) {
        cerr << "Error: Missing number of threads argument.\n";
        return 1;
    }
    int p;

//    p = stoi(argv[1]);
    generateMatrices(10, 10, 3);

    ifstream inputFile(R"(C:\Users\alest\CLionProjects\ppd\hw2_cpp\cmake-build-debug\input.txt)");
    if (!inputFile) {
        cerr << "Error opening input file." << endl;
        return 1;
    }

    int N, M, k;
    inputFile >> N >> M >> k;

    int **F = readMatrix(inputFile, N, M);
    int **C = readMatrix(inputFile, k, k);

    auto t1 = chrono::high_resolution_clock::now();
    convolveInPlaceSerial(F, C, N, M, k);
    auto t2 = chrono::high_resolution_clock::now();
    double serialTime = chrono::duration<double, milli>(t2 - t1).count();
    cout << "Serial Time: " << serialTime << " ms\n";
    writeMatrix(R"(C:\Users\alest\CLionProjects\ppd\hw2_cpp\cmake-build-debug\outputSerial.txt)", F, N, M);

    p = 4;
    vector<thread> threads;

    inputFile.clear();
    inputFile.seekg(0, ios::beg);
    inputFile >> N >> M >> k;
    F = readMatrix(inputFile, N, M);

    t1 = chrono::high_resolution_clock::now();
    int rowsPerThread = N / p;
    int remainder = N % p;
    int start = 0;
    barrier barrier{p};

    for (int i = 0; i < p; ++i) {
        int end = start + rowsPerThread + (remainder > 0 ? 1 : 0);
        if (remainder > 0) remainder--;

        threads.emplace_back(convolveInPlaceParallel, F, C, start, end, N, M, k, ref(barrier));
        start = end;
    }
    for (auto &th: threads) th.join();
    t2 = chrono::high_resolution_clock::now();
    double parallelTime = chrono::duration<double, milli>(t2 - t1).count();
    cout << "Parallel Time: " << parallelTime << " ms\n";
    writeMatrix(R"(C:\Users\alest\CLionProjects\ppd\hw2_cpp\cmake-build-debug\outputParallel.txt)", F, N, M);

    for (int i = 0; i < N; ++i) {
        delete[] F[i];
    }

    delete[] F;

    for (int i = 0; i < k; ++i) {
        delete[] C[i];
    }

    delete[] C;

    inputFile.close();

    ifstream inputFileSerial(R"(C:\Users\alest\CLionProjects\ppd\hw2_cpp\cmake-build-debug\outputSerial.txt)");

    int **VSerial = new int *[N];

    for (int i = 0; i < N; ++i) {
        VSerial[i] = new int[M];

        for (int j = 0; j < M; ++j) {
            inputFileSerial >> VSerial[i][j];
        }
    }

    inputFileSerial.close();

    ifstream inputFileParallel(R"(C:\Users\alest\CLionProjects\ppd\hw2_cpp\cmake-build-debug\outputParallel.txt)");

    int **VParallel = new int *[N];

    for (int i = 0; i < N; ++i) {
        VParallel[i] = new int[M];

        for (int j = 0; j < M; ++j) {
            inputFileParallel >> VParallel[i][j];
        }
    }

    inputFileParallel.close();

    assertMatricesEqual(VSerial, VParallel, N, M);

    for (int i = 0; i < N; ++i) {
        delete[] VSerial[i];
    }

    delete[] VSerial;

    for (int i = 0; i < N; ++i) {
        delete[] VParallel[i];
    }

    delete[] VParallel;

    return 0;
}
