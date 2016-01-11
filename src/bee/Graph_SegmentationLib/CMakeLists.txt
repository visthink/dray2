cmake_minimum_required(VERSION 2.8)

#   Gather all source files
set ( GRAPH_SEGMENTATION_LIB_SOURCES convolve.h disjoint-set.h filter.h image.h imconv.h imutil.h misc.h pnmfile.h segment-graph.h segment-image.h )

# Try a custom target
add_custom_target(Graph_SegmentationLib GRAPH_SEGMENTATION_LIB_SOURCES ${GRAPH_SEGMENTATION_LIB_SOURCES})

#   create a header only library -- Graph_SegmentationLib
#add_library ( Graph_SegmentationLib ${GRAPH_SEGMENTATION_LIB_SOURCES} )

#   specify the language used in the header only library
#set_target_properties ( Graph_SegmentationLib PROPERTIES LINKER_LANGUAGE CXX )
