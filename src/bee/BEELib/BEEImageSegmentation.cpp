
#include <cstdio>
#include <cstdlib>

#include <image.h>
#include <misc.h>
#include <pnmfile.h>
#include "segment-image.h"

#include "BEEImageSegmentation.h"

using namespace SET;
using namespace cv;

void SET::SegmentFullImage(string fname_input, string fname_output, SParams params)
{
	float sigma = params.sigma;
	float k = params.k;
	int min_size = params.min_size;
	
	image<rgb> *input = loadPPM(fname_input.c_str());

	printf("processing\n");
	int num_ccs; 
	image<rgb> *seg = segment_image(input, sigma, k, min_size, &num_ccs); 
	savePPM(seg, fname_output.c_str());
}

void SET::SegmentFullImage(const Mat input, Mat & output, SParams params)
{
	float sigma = params.sigma;
	float k = params.k;
	int min_size = params.min_size;
	
	image<rgb> *input_PPM = new image<rgb>(input.cols, input.rows);
	MatToPPM(input, input_PPM);

	printf("processing\n");
	int num_ccs; 
	image<rgb> *seg = segment_image(input_PPM, sigma, k, min_size, &num_ccs); 

	if (output.empty())
		output = Mat::zeros(input.rows, input.cols, CV_8UC3);

	PPMToMat(seg, output);

	delete input_PPM;
	delete seg;
}

const SegmentationParams SET::SetSegmentationParams(float sigma, float k, int min_size)
{
	SegmentationParams params;
	params.sigma = sigma;
	params.k = k;
	params.min_size = min_size;

	return params;
}

void SET::MatToPPM(Mat mat, image<rgb> *im)
{
  int width = mat.cols;
  int height = mat.rows;

  for (int y = 0; y < height; y++) {
	  for (int x = 0; x < width; x++) {
		  imRef(im, x, y).r = mat.data[mat.step * y + x * 3 + 2];
		  imRef(im, x, y).g = mat.data[mat.step * y + x * 3 + 1];
		  imRef(im, x, y).b = mat.data[mat.step * y + x * 3 + 0];
	  }
  }
}


void SET::PPMToMat(image<rgb> *im, Mat & mat)
{
  int width = mat.cols;
  int height = mat.rows;

  for (int y = 0; y < height; y++) {
	  for (int x = 0; x < width; x++) {
		  mat.data[mat.step * y + x * 3 + 2] = imRef(im, x, y).r;
		  mat.data[mat.step * y + x * 3 + 1] = imRef(im, x, y).g;
		  mat.data[mat.step * y + x * 3 + 0] = imRef(im, x, y).b;
	  }
  }
}