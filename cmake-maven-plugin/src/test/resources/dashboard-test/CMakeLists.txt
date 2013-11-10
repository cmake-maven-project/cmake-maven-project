cmake_minimum_required(VERSION 2.6.4)
project(DashboardTest)
add_executable(dashboard-test main.c)

# Turn on testing
enable_testing()
include(CTest)

# Tell ctest where to find its tests
add_subdirectory(tests)