//#include <iostream>
//#include <fstream>
//#include <vector>
//#include <thread>
//#include <cmath>
//#include <chrono>
//
//using namespace std;
//
//vector<vector<int>> readMatrix(ifstream& file, int rows, int cols) {
//    vector<vector<int>> matrix(rows, vector<int>(cols));
//    for (int i = 0; i < rows; ++i)
//        for (int j = 0; j < cols; ++j)
//            file >> matrix[i][j];
//    return matrix;
//}
//
//void writeMatrix(const string& filename, const vector<vector<int>>& matrix) {
//    ofstream file(filename);
//    for (const auto& row : matrix) {
//        for (const auto& elem : row)
//            file << elem << " ";
//        file << "\n";
//    }
//}
//
//vector<vector<int>> convolveSerial(const vector<vector<int>>& F, const vector<vector<int>>& C, int N, int M, int k) {
//    vector<vector<int>> V(N, vector<int>(M));
//    int offset = k / 2;
//
//    for (int i = 0; i < N; ++i) {
//        for (int j = 0; j < M; ++j) {
//            int sum = 0;
//            for (int x = 0; x < k; ++x) {
//                for (int y = 0; y < k; ++y) {
//                    int u = min(max(i + x - offset, 0), N - 1);
//                    int v = min(max(j + y - offset, 0), M - 1);
//                    sum += F[u][v] * C[x][y];
//                }
//            }
//            V[i][j] = sum;
//        }
//    }
//    return V;
//}
//
//void convolveHorizontal(const vector<vector<int>>& F, const vector<vector<int>>& C, vector<vector<int>>& V, int startRow, int endRow, int cols) {
//    int k = C.size();
//    int offset = k / 2;
//
//    for (int i = startRow; i < endRow; ++i) {
//        for (int j = 0; j < cols; ++j) {
//            int sum = 0;
//            for (int x = 0; x < k; ++x) {
//                for (int y = 0; y < k; ++y) {
//                    int u = min(max(i + x - offset, 0), static_cast<int>(F.size()) - 1);
//                    int v = min(max(j + y - offset, 0), cols - 1);
//                    sum += F[u][v] * C[x][y];
//                }
//            }
//            V[i][j] = sum;
//        }
//    }
//}
//
//void convolveVertical(const vector<vector<int>>& F, const vector<vector<int>>& C, vector<vector<int>>& V, int startCol, int endCol, int rows) {
//    int k = C.size();
//    int offset = k / 2;
//
//    for (int i = 0; i < rows; ++i) {
//        for (int j = startCol; j < endCol; ++j) {
//            int sum = 0;
//            for (int x = 0; x < k; ++x) {
//                for (int y = 0; y < k; ++y) {
//                    int u = min(max(i + x - offset, 0), rows - 1);
//                    int v = min(max(j + y - offset, 0), static_cast<int>(F[0].size()) - 1);
//                    sum += F[u][v] * C[x][y];
//                }
//            }
//            V[i][j] = sum;
//        }
//    }
//}
//
//void dynamic_with_vector() {
//    ifstream inputFile("input.txt");
//
//    int N, M, n, m;
//    inputFile >> N >> M >> n >> m;
//
//    auto F = readMatrix(inputFile, N, M);
//    auto C = readMatrix(inputFile, n, m);
//
//    auto t1 = chrono::high_resolution_clock::now();
//    auto VSerial = convolveSerial(F, C, N, M, n);
//    auto t2 = chrono::high_resolution_clock::now();
//    double serialTime = chrono::duration<double, milli>(t2 - t1).count();
//    cout << "Serial Time: " << serialTime << " ms\n";
//    writeMatrix("outputSerial.txt", VSerial);
//
//    int p = 4;
//    vector<thread> threads(p);
//    auto VParallelHorizontal = vector<vector<int>>(N, vector<int>(M));
//    auto VParallelVertical = vector<vector<int>>(N, vector<int>(M));
//
//    t1 = chrono::high_resolution_clock::now();
//    int rowsPerThread = N / p;
//    int remainder = N % p;
//    int start = 0;
//    for (int i = 0; i < p; ++i) {
//        int end = start + rowsPerThread + (remainder-- > 0 ? 1 : 0);
//        threads[i] = thread(convolveHorizontal, cref(F), cref(C), ref(VParallelHorizontal), start, end, M);
//        start = end;
//    }
//    for (auto& th : threads) th.join();
//    t2 = chrono::high_resolution_clock::now();
//    double parallelHorizontalTime = chrono::duration<double, milli>(t2 - t1).count();
//    cout << "Parallel Horizontal Time: " << parallelHorizontalTime << " ms\n";
//    writeMatrix("outputParallelHorizontal.txt", VParallelHorizontal);
//
//    t1 = chrono::high_resolution_clock::now();
//    int colsPerThread = M / p;
//    remainder = M % p;
//    start = 0;
//    for (int i = 0; i < p; ++i) {
//        int end = start + colsPerThread + (remainder-- > 0 ? 1 : 0);
//        threads[i] = thread(convolveVertical, cref(F), cref(C), ref(VParallelVertical), start, end, N);
//        start = end;
//    }
//    for (auto& th : threads) th.join();
//    t2 = chrono::high_resolution_clock::now();
//    double parallelVerticalTime = chrono::duration<double, milli>(t2 - t1).count();
//    cout << "Parallel Vertical Time: " << parallelVerticalTime << " ms\n";
//    writeMatrix("outputParallelVertical.txt", VParallelVertical);
//}
