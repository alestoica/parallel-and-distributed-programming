#include <iostream>
#include <vector>
#include <fstream>
#include <mpi.h>
#include <chrono>

const int MAX_DIGITS = 100005;


void readNumber(const std::string &filename, int *number, int &N) {
    std::ifstream input(filename);
    if (!input.is_open()) {
        std::cerr << "Error opening file: " << filename << std::endl;
        MPI_Abort(MPI_COMM_WORLD, 1);
    }
    input >> N;
    for (int i = 0; i < N; ++i) {
        char digit_char;
        input >> digit_char;
        int digit = digit_char - '0';
        number[N - 1 - i] = digit;
    }
    std::fill(number + N, number + MAX_DIGITS, 0);
}


void writeNumber(int *number, int &N, const std::string &filename) {
    std::ofstream file(filename);
    file << N << "\n";
    for (int i = N - 1; i >= 0; --i) {
        file << number[i];
    }
    file << "\n";
}

void generateRandomNumber(int *number, int N) {
    number[0] = std::rand() % 9 + 1;

    for (int i = 1; i < N; ++i) {
        number[i] = std::rand() % 10;
    }
}

// VAR 0 - Sequential
void sequentialAddition() {
    auto start_time = std::chrono::high_resolution_clock::now();

    int number1[MAX_DIGITS] = {0}, number2[MAX_DIGITS] = {0}, result[MAX_DIGITS + 1] = {0};
    int N1, N2;
    int N;

    readNumber("number1.txt", number1, N1);
    readNumber("number2.txt", number2, N2);

    N = std::max(N1, N2);

    int carry = 0;
    for (int i = 0; i < N; ++i) {
        int digit1 = (i < N1) ? number1[i] : 0;
        int digit2 = (i < N2) ? number2[i] : 0;
        int sum = digit1 + digit2 + carry;
        result[i] = sum % 10;
        carry = sum / 10;
    }
    if (carry > 0) {
        result[N] = carry;
        N++;
    }

    writeNumber(result, N, "number3_var0.txt");

    auto end_time = std::chrono::high_resolution_clock::now();
    double duration = std::chrono::duration<double, std::milli>(end_time - start_time).count();
    std::cout << "Time for VAR 0: " << duration << " milliseconds.\n";
}

// VAR 1 - MPI Send/Recv
void mpiAdditionVar1(int argc, char *argv[]) {
    MPI_Init(&argc, &argv);

    int world_size;
    MPI_Comm_size(MPI_COMM_WORLD, &world_size);
    int world_rank;
    MPI_Comm_rank(MPI_COMM_WORLD, &world_rank);

    const int MASTER = 0;
    int start, end, carry = 0;
    MPI_Status status;
    int number1[MAX_DIGITS] = {0}, number2[MAX_DIGITS] = {0}, result[MAX_DIGITS + 1] = {0};
    int N1, N2, N;
    int P = world_size - 1;

    if (world_rank == MASTER) {
        auto start_time = std::chrono::high_resolution_clock::now();

        readNumber("number1.txt", number1, N1);
        readNumber("number2.txt", number2, N2);
        N = std::max(N1, N2);
        int block = N / P;
        int remainder = N % P;
        start = 0;

        for (int i = 1; i < world_size; ++i) {
            int local_start = start;
            int local_end = local_start + block + (remainder > 0 ? 1 : 0) - 1;
            remainder--;

            MPI_Send(&local_start, 1, MPI_INT, i, 1, MPI_COMM_WORLD);
            MPI_Send(&local_end, 1, MPI_INT, i, 2, MPI_COMM_WORLD);
            MPI_Send(number1 + local_start, local_end - local_start + 1, MPI_INT, i, 3, MPI_COMM_WORLD);
            MPI_Send(number2 + local_start, local_end - local_start + 1, MPI_INT, i, 4, MPI_COMM_WORLD);

            start = local_end + 1;
        }

        for (int i = 1; i < world_size; ++i) {
            int temp_start, temp_end;

            MPI_Recv(&temp_start, 1, MPI_INT, i, 1, MPI_COMM_WORLD, &status);
            MPI_Recv(&temp_end, 1, MPI_INT, i, 2, MPI_COMM_WORLD, &status);
            MPI_Recv(result + temp_start, temp_end - temp_start + 1, MPI_INT, i, 5, MPI_COMM_WORLD, &status);
        }

        MPI_Recv(&carry, 1, MPI_INT, world_size - 1, 6, MPI_COMM_WORLD, &status);

        if (carry != 0) {
            result[N] = carry;
            N += 1;
        }

        writeNumber(result, N, "number3_var1.txt");

        auto end_time = std::chrono::high_resolution_clock::now();
        double duration = std::chrono::duration<double, std::milli>(end_time - start_time).count();
        std::cout << "Time for VAR 1: " << duration << " milliseconds.\n";
    } else {
        if (world_rank > 1)
            MPI_Recv(&carry, 1, MPI_INT, world_rank - 1, 6, MPI_COMM_WORLD, &status);
        MPI_Recv(&start, 1, MPI_INT, MASTER, 1, MPI_COMM_WORLD, &status);
        MPI_Recv(&end, 1, MPI_INT, MASTER, 2, MPI_COMM_WORLD, &status);
        MPI_Recv(number1 + start, end - start + 1, MPI_INT, MASTER, 3, MPI_COMM_WORLD, &status);
        MPI_Recv(number2 + start, end - start + 1, MPI_INT, MASTER, 4, MPI_COMM_WORLD, &status);

        for (int i = start; i <= end; ++i) {
            int sum = number1[i] + number2[i] + carry;
            result[i] = sum % 10;
            carry = sum / 10;
        }

        MPI_Send(&start, 1, MPI_INT, MASTER, 1, MPI_COMM_WORLD);
        MPI_Send(&end, 1, MPI_INT, MASTER, 2, MPI_COMM_WORLD);
        MPI_Send(result + start, end - start + 1, MPI_INT, MASTER, 5, MPI_COMM_WORLD);
        if (world_rank != world_size - 1)
            MPI_Send(&carry, 1, MPI_INT, world_rank + 1, 6, MPI_COMM_WORLD);
        else
            MPI_Send(&carry, 1, MPI_INT, MASTER, 6, MPI_COMM_WORLD);
    }

    MPI_Finalize();
}

// VAR 2 - MPI Scatter/Gather
void mpiAdditionVar2(int argc, char *argv[]) {
    MPI_Init(&argc, &argv);

    int world_size;
    MPI_Comm_size(MPI_COMM_WORLD, &world_size);
    int world_rank;
    MPI_Comm_rank(MPI_COMM_WORLD, &world_rank);

    const int MASTER = 0;
    int N1, N2, N;
    int number1[MAX_DIGITS] = {0}, number2[MAX_DIGITS] = {0}, result[MAX_DIGITS + 1] = {0};
    int chunkSize;
    auto start_time = std::chrono::high_resolution_clock::now();
    int carry = 0;

    if (world_rank == MASTER) {
        start_time = std::chrono::high_resolution_clock::now();

        readNumber("number1.txt", number1, N1);
        readNumber("number2.txt", number2, N2);
        N = std::max(N1, N2);

        if (N % world_size != 0) {
            int padding = world_size - (N % world_size);
            std::fill(number1 + N, number1 + N + padding, 0);
            std::fill(number2 + N, number2 + N + padding, 0);
            N += padding;
        }
    }

    MPI_Bcast(&N, 1, MPI_INT, MASTER, MPI_COMM_WORLD);
    chunkSize = N / world_size;

    int* auxNumber1 = new int[chunkSize]();
    int* auxNumber2 = new int[chunkSize]();
    int* auxResult = new int[chunkSize + 1]();

    MPI_Scatter(number1, chunkSize, MPI_INT, auxNumber1, chunkSize, MPI_INT, 0, MPI_COMM_WORLD);
    MPI_Scatter(number2, chunkSize, MPI_INT, auxNumber2, chunkSize, MPI_INT, 0, MPI_COMM_WORLD);

    if (world_rank != MASTER)
        MPI_Recv(&carry, 1, MPI_INT, world_rank - 1, 0, MPI_COMM_WORLD, MPI_STATUS_IGNORE);

    for (int i = 0; i < chunkSize; ++i) {
        int sum = auxNumber1[i] + auxNumber2[i] + carry;
        auxResult[i] = sum % 10;
        carry = sum / 10;
    }

    if (world_rank != world_size - 1) {
        MPI_Send(&carry, 1, MPI_INT, world_rank + 1, 0, MPI_COMM_WORLD);
    }

    MPI_Gather(auxResult, chunkSize, MPI_INT, result, chunkSize, MPI_INT, 0, MPI_COMM_WORLD);

    if (world_rank == MASTER) {
        if (carry != 0) {
            result[N] = carry;
            N += 1;
        }

        while (N > std::max(N1, N2) + 1) {
            N--;
        }

        if (result[N - 1] == 0)
            N--;

        writeNumber(result, N, "number3_var2.txt");

        auto end_time = std::chrono::high_resolution_clock::now();
        double duration = std::chrono::duration<double, std::milli>(end_time - start_time).count();
        std::cout << "Time for VAR 2: " << duration << " milliseconds.\n";
    }

    delete[] auxNumber1;
    delete[] auxNumber2;
    delete[] auxResult;

    MPI_Finalize();
}

void verifyResults() {
    int resultSequential[MAX_DIGITS + 1] = {0};
    int resultVar1[MAX_DIGITS + 1] = {0};
    int resultVar2[MAX_DIGITS + 1] = {0};
    int N;

    readNumber("number3_var0.txt", resultSequential, N);
    readNumber("number3_var1.txt", resultVar1, N);
    readNumber("number3_var2.txt", resultVar2, N);

    for (int i = 0; i < N; ++i) {
        if (resultSequential[i] != resultVar1[i]) {
            std::cout << "Results do not match! -> VAR0 != VAR1" << std::endl;
            return;
        }
        if (resultSequential[i] != resultVar2[i]) {
            std::cout << "Results do not match! -> VAR0 != VAR2" << std::endl;
            return;
        }
    }
    std::cout << "Results match!" << std::endl;
}

int main(int argc, char* argv[]) {
//    std::srand(std::time(0));
//    int N1 = 100, N2 = 100;
//    int number1[MAX_DIGITS] = {0}, number2[MAX_DIGITS] = {0};
//
//    generateRandomNumber(number1, N1);
//    generateRandomNumber(number2, N2);
//
//    writeNumber(number1, N1, "number1.txt");
//    writeNumber(number2, N2, "number2.txt");

    // VAR 0 - Suma secventială
//    sequentialAddition();  // Variant 0

    // VAR 1 - Suma paralelă cu MPI
//    mpiAdditionVar1(argc, argv);  // Variant 1

    // VAR 2 - Suma paralelă cu MPI (carry propagation)
//    mpiAdditionVar2(argc, argv);  // Variant 2

    verifyResults();

    return 0;
}
