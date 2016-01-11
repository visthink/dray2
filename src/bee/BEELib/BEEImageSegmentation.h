#ifndef __BEE_IMAGESEGMENTATION_H__
#define __BEE_IMAGESEGMENTATION_H__

#include "image.h"
#include "misc.h"

#include <opencv2/opencv.hpp>

using namespace cv;
using namespace std;

typedef struct SegmentationParams
{
	float sigma;
	float k;
	int min_size;
} SParams;

namespace SET 
{
	void SegmentFullImage(string fname_input, string fname_output, SParams params);
	void SegmentFullImage(const Mat input, Mat & output, SParams params);
	const SegmentationParams SetSegmentationParams(float sigma, float k, int min_size);

	void MatToPPM(Mat mat, image<rgb> *im);
	void PPMToMat(image<rgb> *im, Mat & mat);
}

#endif
