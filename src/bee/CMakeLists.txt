cmake_minimum_required(VERSION 2.8)

#   create a project
project( beelibtester )

#   get the required packages (OpenCV)
find_package( OpenCV REQUIRED )

#   create the executable
add_executable( beelibtester BEELibTester.cpp )

#   include a library
include_directories ( "${PROJECT_SOURCE_DIR}/Graph_SegmentationLib" )
add_subdirectory ( Graph_SegmentationLib )

#   include a library
include_directories ( "${PROJECT_SOURCE_DIR}/BEELib" )
add_subdirectory ( BEELib )

#   link the libraries
set ( EXTRA_LIBS ${EXTRA_LIBS} ${OpenCV_LIBS} BEELib )
target_link_libraries( beelibtester ${EXTRA_LIBS} )

