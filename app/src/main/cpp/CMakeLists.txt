cmake_minimum_required(VERSION 3.22.1)

project("tiktokstudio")

add_library(
    tiktokstudio
    SHARED
    native-downloader.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    tiktokstudio
    ${log-lib}
)
