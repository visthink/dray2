
README for bee

Updated: September 3, 2014

The folder "bee" contains all files needed to build and run the project "BEELibTester".  It is created as a cmake-based project. To build and run it in Windows (7 or later), Ubuntu (14.04 or later), and Mac OSX, make sure the following steps are followed.

For Windows 7:
————————————-
Before starting the build:

  1. Install cmake if needed (version 2.8.12 is available at http://www.cmake.org/cmake/resources/software.html).
  2. Install OpenCV if needed (version 2.4.9 is available at http://opencv.org/downloads.html).

To build the BEELibTester executable: 

  1. Copy the folder "bee" into your workspace. For this example: C:/home/pi/bee.
  2. Make a dir "bee_build" at the same level as that of "bee". For this example: C:/home/pi/bee_build.
  3. Start the cmake gui. Choose the appropriate compiler for building the project. We have successfully built the system using Microsoft Visual Studio 10 (MSVC) and the GNU C++ compiler (gcc) version 4.8.2.
  4. At the top of the cmake gui window, in "Where is the source code", type in the source path (e.g., “C:/home/pi/bee" — without quotes) or select the path use the file selector at the right.
  5. Similarly, in the same window, in "Where to build the binaries", type in the newly-created build directory (e.g., “C:/home/pi/bee_build" — without quotes) or select the path using the file selector.
  6. Click the "configure" button at the bottom. If red "name/value" lines show up, correct the "value" entry to appropriate path or file names. Repeat the "configure" step until no red colored line, and "configure" is completed.
  7. Click at "generate" button at the bottom, and wait until generation is complete. 
  8. Change the current folder to C:/home/pi/bee_build (or your build folder). If the compiler is MSVC, the solution file beelibtester.sln is created. Use the beelibtester.sln to build the project "BEELibTester".
  9. After the project "BEELibTester" is successfully built, open the command window. Change directory to the Release subdirectory (assuming a release version is built).
 10. To run the system, enter the command
 
       beelibtester Diagram_Image_Filename Diagram_Image_Filename_FullPath Output_Blob_Filename isRemoveBG(optional)

   where:
    - Diagram_Image_Filename is the file name for the diagram image in the local disk
    - Diagram_Image_Filename_FullPath is the (relative) file path/name for the diagram image to put in the output blob text file
    - Output_Blob_Filename is the path for the output file
    - isRemoveBG is 1 to use the optional background removal routine, and 0 otherwise


For Ubuntu 14.04 or later:
——————————————————————————
Before starting the build:

  1. Install cmake if needed (typically standard on Ubutu 14.04 LTS).
  2. Install OpenCV if needed (available at https://help.ubuntu.com/community/OpenCV, or http://www.sysads.co.uk/2014/05/install-opencv-2-4-9-ubuntu-14-04-13-10/). Important note: if you don't have an Nvidia card on your ubuntu system, you must install the package ocl-icd-libopencl1 (sudo apt-get install ocl-icd-libopencl1) before installing the opencv package.

To build the BEELibTester executable:

  1. Copy the folder "bee" into your workspace. For this example: /home/pi/bee.
  2. Make a new "bee_build" directory at the same level as that of "bee". Example this example: /home/pi/bee_build.
  3. Change current directory to the newly created build directory (e.g., "cd /home/pi/bee_build”).
  4. Run cmake. Example: “cmake ../bee" (no quotes)
  5. Run make (e.g., “make” — without the double quotes).
  6. To run the system, enter the command
 
       ./beelibtester Diagram_Image_Filename Diagram_Image_Filename_FullPath Output_Blob_Filename isRemoveBG

   where:
    - Diagram_Image_Filename is the file name for the diagram image in the local disk
    - Diagram_Image_Filename_FullPath is the (relative) file path/name for the diagram image to put in the output blob text file
    - Output_Blob_Filename is the path for the output file
    - isRemoveBG is 1 to use the optional background removal routine, and 0 otherwise


For Mac OS X (10.9 or later):
————————————————————————————

In general, follow the same general instructions as for Ubuntu. 

1. Make sure that you have cmake and OpenCV installed. 
   - cmake comes with the MacOS, can be installed with brew ("brew install cmake"), or 
     can be downloaded from http://www.cmake.org
   - OpenCV can be installed with brew as well ("brew install opencv")

2. To install bee:
   - Create a copy of the source (this distribution) and make a new sibling folder for the build.
   - Change directory to the build directory.
   - While in that directory, run cmake ("cmake ../bee”)
   - Make the project ("make")
   
3. To test bee:
   - Run the command "./beelibtester inputfile outputfile textfile 0"
   

******************************************************************************************************** 

An example command to run bee is as follows:

.\BEELibTester 008_11_pv_i-000.png images-in-039_04_pv.xml_data/image-1.jpg blob_008_11_pv_i-000_Sept03-2014.txt 0

Alternatively, a shell script for running this example is in the bee/testdata directory. To run with the new build (from the build directory):

  > cp -r ../bee/testdata .  # Copy over the test data
  > cd testdata             # Go into the directory
  > ./run-example.sh         # Run the script on the test data

******************************************************************************************************** 

