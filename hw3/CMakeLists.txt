cmake_minimum_required(VERSION 3.24)
project(hw3)

set(CMAKE_CXX_STANDARD 17)

if(DEFINED ENV{MPI_INCLUDE} AND DEFINED ENV{MPI_LIB})
    set(MPI_INCLUDE_PATH $ENV{MPI_INCLUDE})
    set(MPI_LIBRARY_PATH $ENV{MPI_LIB})
else()
    message(FATAL_ERROR "MPI_INCLUDE or MPI_LIB environment variables are not set")
endif()

include_directories(${MPI_INCLUDE_PATH})

link_directories(${MPI_LIBRARY_PATH})

add_executable(hw3 main.cpp)

target_link_libraries(hw3 msmpi.lib msmpifec.lib)

