cmake_minimum_required(VERSION 2.8)

#	include files of dependency -- Graph_SegmentationLib
include_directories ( "../Graph_SegmentationLib" )
#include_directories ( "${${PROJECT_SOURCE_DIR}/Graph_SegmentationLib}" )

#	Gather all source files for BEELib
set ( BEE_LIB_SOURCES BEEBlobData.h BEEBlobData.cpp BEEBlobMatcher.h BEEBlobMatcher.cpp BEEExtractor.h BEEExtractor.cpp BEEImageData.h BEEImageData.cpp BEEImageSegmentation.h BEEImageSegmentation.cpp ${Graph_SegmentationLib} )

#	create a lib file "BEELib"
add_library ( BEELib ${BEE_LIB_SOURCES} )

#	find and link the thrid-paty lib "OpenCV"
find_package( OpenCV REQUIRED )
target_link_libraries( BEELib ${OpenCV_LIBS})
