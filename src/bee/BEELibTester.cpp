
#include <stdio.h>

#include "BEEExtractor.h"

using namespace std;
using namespace cv;


int Usage()
{
	printf("Usage: BEELibTester.exe Diagram_Image_Filename Diagram_Image_Filename_FullPath Output_Blob_Filename isRemoveBG(optional) \n");
	printf("Example: BEELibTester.exe 008_11_pv_i-000.png  images-in-039_04_pv.xml_data/image-1.jpg tmp_blobs.txt 0 \n");
	return -1;
}

int main(int argc, char** argv)
{
bool isRemoveBG = 0;

	if (argc < 2)
	{
		return Usage();
	}

	if (argc >= 5)
	{
		isRemoveBG = atoi(argv[4]);
	}

	BEEExtractor bExt(isRemoveBG);

	bExt.BEEExtractionFromImage(argv[1]);

	bExt.WriteOutBlobs(argv[2], argv[3]);

	return 0;
}
