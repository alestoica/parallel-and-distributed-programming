#include <iostream>
#include <fstream>
#include "dynamic/dynamic.h"
#include "static/static.h"
#include "dynamic/dynamic_with_vector/dynamic_with_vector.h"

using namespace std;

void generateMatrixFile(int N, int M, int n, int m) {
    ofstream writer("input.txt");
    if (!writer) {
        cerr << "Error while opening the next file: " << "input.txt" << endl;
        return;
    }

    writer << N << " " << M << " " << n << " " << m << "\n";

    ::srand(::time(0));
    for (int i = 0; i < N; i++) {
        for (int j = 0; j < M; j++) {
            int randomValue = rand() % 100;
            writer << randomValue << " ";
        }
        writer << "\n";
    }

    for (int i = 0; i < n; i++) {
        for (int j = 0; j < m; j++) {
            int randomValue = rand() % 3;
            writer << randomValue << " ";
        }
        writer << "\n";
    }

    writer.close();
}

int main() {
    generateMatrixFile(10, 10, 5, 5);

    solve_dynamic();

    return 0;
}
