cmake_minimum_required(VERSION 3.24)
project(hw1_cpp)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -Wl,--stack,524288000")

add_executable(hw1_cpp main.cpp static/static.cpp dynamic/dynamic.cpp dynamic/dynamic_with_vector/dynamic_with_vector.cpp dynamic/dynamic.h dynamic/dynamic_with_vector/dynamic_with_vector.h static/static.h)
