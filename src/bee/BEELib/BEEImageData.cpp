
#include <opencv2/opencv.hpp>

#include "BEEImageSegmentation.h"
#include "BEEImageData.h"
#include "BEEBlobData.h"

using namespace std;

BEEImageData::BEEImageData()
{
}

BEEImageData::BEEImageData(string str_fname, bool isRemoveBG)
{
	m_rgb_image = imread(str_fname);

	if(m_rgb_image.empty())
	{
		cout << "Image Read Error with " << str_fname << endl;
	}

	SetImageWidth(m_rgb_image.cols);
	SetImageHeight(m_rgb_image.rows);

	SplitChannels(m_rgb_image, m_bgr_planes);

	ConvertRGB2HSV(m_rgb_image, m_hsv_image);
	SplitChannels(m_hsv_image, m_hsv_planes);

	m_isRemoveBG = isRemoveBG;
	if (m_isRemoveBG)	
		RemoveBG(); 

	ThreshImage(m_rgb_image, m_bthreshedImage);

#if 0
	//DisplayImage(m_rgb_image);
	//DisplayImage(m_hsv_image);
	Point pts(136, 346);
	Vec3b hsv = m_hsv_image.at<Vec3b>(pts);
#endif

}

BEEImageData::~BEEImageData()
{
	if (!m_rgb_image.empty())
		m_rgb_image.release();
	
	if (!m_hsv_image.empty())
		m_hsv_image.release();

	// TBD: clear m_bgr_planes
}

void BEEImageData::RemoveBG()
{
	int hbins = 10, sbins = 1;
    int histSize[] = {hbins, sbins};
    // hue varies from 0 to 179, see cvtColor
    float hranges[] = { 0, 180 };
    // saturation varies from 0 (black-gray-white) to 255 (pure spectrum color)
    float sranges[] = { 0, 256 };
    const float* ranges[] = { hranges, sranges };
    MatND hist;

    // we compute the histogram from the 0-th and 1-st channels
	int channels[] = {0, 1};

    calcHist(&m_hsv_image, 1, channels, Mat(), // do not use mask
             hist, 2, histSize, ranges,
             true, // the histogram is uniform
             false );

	double maxVal=0;
	Point max_loc;

	// Get the index for the bin maximum number of pixles
	minMaxLoc(hist, 0, &maxVal, 0, &max_loc);

	// hist has 1 column (for saturation sbin) and 10 rows (for hue hbin)
	int Imax = max_loc.y;
	float offset = 3.0f;

	float delta = 180.0f / hbins;
	float LIndex = (Imax-offset > 0) ? (Imax-offset):0;
	float RIndex = (Imax+offset < hbins-1) ? (Imax+offset):(hbins-1);

	float LValue = LIndex * delta + delta/2.0f; 
	float RValue = RIndex * delta + delta/2.0f; 

	// Perform color segmentation on the image
	Mat image = this->GetRGBImage();
	float large_size_thresh = image.rows * image.cols *0.02;
	float huge_size_thresh = image.rows * image.cols * 0.1;

	Mat segment, segment_relabeled;

	SET::SegmentFullImage(image, segment, 
		SET::SetSegmentationParams(1.0, 400, 200));

	ReLabelImage(segment, segment_relabeled);

	vector<float> unique_color = unique_label<float>(segment_relabeled);

	Mat mask = Mat::zeros(image.rows, image.cols, CV_8UC1);

	for (size_t i = 0; i < unique_color.size(); i++)
	{
		Mat mask_seg_color, mask_seg;

		compare(segment_relabeled, unique_color[i], mask_seg_color, CMP_EQ);

		//cvtColor(mask_seg_color, mask_seg, CV_BGR2GRAY);
		mask_seg_color.convertTo(mask_seg, CV_8UC1);

		Scalar meanColor = mean(m_hsv_planes[0], mask_seg);
  
		if ( (meanColor[0] > LValue && meanColor[0] < RValue && GetMaskArea(mask_seg) > large_size_thresh) ||
				(GetMaskArea(mask_seg) > huge_size_thresh))
			image.setTo(Scalar(255, 255, 255), mask_seg);

	}

	//imwrite("ttt0.jpg", image);
	
	Mat hsv_image;

	vector<Mat> hsv_planes;
	ConvertRGB2HSV(image, hsv_image);
	SplitChannels(hsv_image, hsv_planes);

	Mat matV = hsv_planes[2];

	for (int iter = 0; iter < 2; iter++)
	{	
		ErodeDilateImage(matV, matV, 3, 2);
		FilterImageMedian(matV, matV, 5, 5);
		//DilateErodeImage(matV, matV, 3, 2);
		//FilterImageMedian(matV, matV, 3, 3);
	}

#if 0
	vector<Mat> img_channels;
	img_channels.push_back(hsv_planes[0]);	
	img_channels.push_back(hsv_planes[1]);
	img_channels.push_back(matV); 

	merge(img_channels, hsv_image);
	cvtColor(hsv_image, image, CV_HSV2BGR);
#endif

	m_hsv_planes[2] = matV;

	imwrite("ttt1.jpg", image);

	// reset the original
	m_rgb_image = image;

}

void BEEImageData::Gradient_Initialization(const Mat image)
{
	this->RawImageGradient(image);
	this->ComputeEdgeMask(image);
	this->FiltedImageGradient(image, m_fG);

	this->GetGMask(20);

	this->GrayImageGradient(m_gMask, m_gMaskGrad);

	/*
	imwrite("grad.jpg", m_grad);
	imwrite("fG.jpg", m_fG);
	imwrite("gMask.jpg", m_gMask);
	imwrite("gMaskGrad.jpg", m_gMaskGrad);
	*/

	m_meanGrad = mean(m_grad, m_gMask);
	m_meanGMaskGrad = mean(m_gMaskGrad, m_gMask);

	this->GradientMaskMask(m_gMaskGrad, m_gMask, m_gMaskGrad_mask);

	image.copyTo(m_masked_image, m_gMaskGrad_mask);

	SET::SegmentFullImage(m_masked_image, m_segment_texture,
		SET::SetSegmentationParams(1.5, 300, 400));
	
	ReLabelImage(m_segment_texture, m_segment_texture_relabeled);
}

void BEEImageData::DisplayImage(Mat image)
{
	string imageName("Test Image");
	namedWindow(imageName, CV_WINDOW_AUTOSIZE );
	imshow(imageName, image);
	waitKey(0);
}

void BEEImageData::ConvertRGB2HSV(const Mat & rgb, Mat & hsv)
{
	cvtColor(rgb, hsv, CV_BGR2HSV);
}

void BEEImageData::SplitChannels(const Mat &image, vector<Mat> &planes)
{
	split(image, planes);
}

const Mat BEEImageData::GetRGBChannel(const int index)
{
	return(m_bgr_planes[index]);
}

const Mat BEEImageData::GetHSVChannel(const int index)
{
	return(m_hsv_planes[index]);
}

void BEEImageData::ThreshImage(const Mat image, Mat & bthreshedImage)
{
	Mat filtedImage = Mat::zeros(image.rows, image.cols, CV_8UC3);
	medianBlur(image, filtedImage, 5);

	Mat threshedImage;
	threshold(filtedImage, threshedImage, 250, 255, THRESH_TOZERO_INV);
	
	//imwrite("threshedImage.jpg", threshedImage);

	threshedImage = threshedImage > 1;

	cvtColor(threshedImage, bthreshedImage, CV_BGR2GRAY);
	bthreshedImage = bthreshedImage > 1;

}

void BEEImageData::DilateErodeImage(const Mat input, Mat & output, int dsize, int esize)
{
	if (output.empty()) output = Mat::zeros(input.rows, input.cols, input.type());

	if (dsize > 0)
	{
		Mat delement = getStructuringElement(MORPH_ELLIPSE, Size( 2*dsize + 1, 2*dsize+1 ), Point( dsize, dsize ) );
		dilate(input, output, delement);
	}

	if (esize > 0)
	{
		Mat eelement = getStructuringElement(MORPH_ELLIPSE, Size( 2*esize + 1, 2*esize+1 ), Point(esize, esize ) );
		erode(output, output, eelement );
	}
}


void BEEImageData::ErodeDilateImage(const Mat input, Mat & output, int esize, int dsize)
{
	if (output.empty()) output = Mat::zeros(input.rows, input.cols, input.type());

	if (esize > 0)
	{
		Mat eelement = getStructuringElement(MORPH_ELLIPSE, Size( 2*esize + 1, 2*esize+1 ), Point(esize, esize ) );
		erode(output, output, eelement );
	}

	if (dsize > 0)
	{
		Mat delement = getStructuringElement(MORPH_ELLIPSE, Size( 2*dsize + 1, 2*dsize+1 ), Point( dsize, dsize ) );
		dilate(input, output, delement);
	}
}

void BEEImageData::ComputeEdgeMask(const Mat image, Mat & edgeMask)
{
	Mat src_gray;
	Mat grad;

	cvtColor(image, src_gray, CV_RGB2GRAY );

	GrayImageGradient(src_gray, grad);

	//imwrite("grad.jpg", m_grad);

	Scalar meanGrad = mean(grad);
	edgeMask = grad > 10*meanGrad[0];

	DilateErodeImage(edgeMask, edgeMask, 2, 1);

	edgeMask = edgeMask > 0;

	//imwrite("edgeMask.jpg", m_edgeMask);
}


void BEEImageData::ComputeEdgeMask(const Mat image)
{
	ComputeEdgeMask(image, this->m_edgeMask);
}

void BEEImageData::GradientMaskMask(const Mat gMaskGrad, const Mat gMask, Mat & gMaskGrad_mask)
{

	Scalar meanGMaskGrad = mean(gMaskGrad, gMask);

	gMaskGrad_mask = gMaskGrad > meanGMaskGrad[0]/2;
	
	int morph_size = 5;
	for (int iter = 0; iter < 3; iter++)
	{
		DilateErodeImage(gMaskGrad_mask, gMaskGrad_mask, morph_size, morph_size);
	}

	DilateErodeImage(gMaskGrad_mask, gMaskGrad_mask, 0, morph_size);
	DilateErodeImage(gMaskGrad_mask, gMaskGrad_mask, 0, morph_size);

}

double BEEImageData::GetMaskArea(const Mat mask)
{
	double area	= 0.0f;

	for (int x = 0; x < mask.cols; x++)
		for (int y = 0; y < mask.rows; y++)
			if (mask.data[mask.step * y + x] > 0) area++;
		
	return area;
}


double BEEImageData::GetMaskSolidity(const Mat mask)
{
	double solidity	= 0.0f, weighted_solidity = 0.0f;
	double weights = 0.0f;

	vector<vector<Point> > contours_one;
	vector<Vec4i> hierarchy_one;

	findContours(mask, contours_one, hierarchy_one, CV_RETR_CCOMP, CV_CHAIN_APPROX_SIMPLE );

	if (contours_one.empty()) return 0.0f;

	BEEBlob blob;

	for (size_t iC = 0; iC < contours_one.size(); iC++)
	{		
		weighted_solidity += (blob.ComputeSolidity(contours_one[iC])) * contours_one[iC].size();
		weights += contours_one[iC].size();
	}

	solidity = weighted_solidity/weights;

	return solidity;
}


void BEEImageData::GrayImageGradient(const Mat src_gray, Mat & grad)
{
	Mat grad_x, grad_y;
	Mat abs_grad_x, abs_grad_y;

	int scale = 1;
	int delta = 0;
	int ddepth = CV_16S;

	/// Gradient X
	Sobel(src_gray, grad_x, ddepth, 1, 0, 3, scale, delta, BORDER_DEFAULT );
	convertScaleAbs( grad_x, abs_grad_x );

	/// Gradient Y
	Sobel(src_gray, grad_y, ddepth, 0, 1, 3, scale, delta, BORDER_DEFAULT );
	convertScaleAbs(grad_y, abs_grad_y );

	/// Total Gradient (approximate)
	addWeighted(abs_grad_x, 0.5, abs_grad_y, 0.5, 0, grad);

}

void BEEImageData::RawImageGradient(const Mat image, Mat & grad)
{
	Mat src_gray;
	cvtColor(image, src_gray, CV_RGB2GRAY );

	GrayImageGradient(src_gray, grad);
}

void BEEImageData::RawImageGradient(const Mat image)
{
	RawImageGradient(image, this->m_grad);
}


void BEEImageData::FiltedImageGradient(const Mat image, Mat & fG)
{
	Mat src_gray;
	cvtColor(image, src_gray, CV_RGB2GRAY );

	GaussianBlur(src_gray, src_gray, Size(3,3), 0, 0, BORDER_DEFAULT );

	GrayImageGradient(src_gray, fG);
}

void BEEImageData::ReLabelImage(const Mat in, Mat & out)
{
	int width = in.cols;
	int height = in.rows;

	out = Mat::zeros(in.rows, in.cols, CV_32FC1);

	if (in.channels() == 1)
	{
		in.convertTo(out, CV_32FC1, 1.0); 
		return;
	}

	float* ptr = (float*) out.data;
	size_t elem_step = out.step / sizeof(float);

	for (int y = 0; y < height; y++) 
	{
		for (int x = 0; x < width; x++) 
		{
			float valR = (float)in.data[in.step * y + x * 3 + 2];
			float valG = (float)in.data[in.step * y + x * 3 + 1];
			float valB = (float)in.data[in.step * y + x * 3 + 0];

			ptr[y * elem_step + x] = (float)(valB * 256*256 + valG * 256 + valR);			
		}
	}

}

void BEEImageData::FilterImageMedian(const Mat image, Mat &filtedImage, const int ITER, const int fSize)
{
	//int fSize = 5;

	Mat input = image, tmp;
	for (int iter = 0; iter < ITER; iter++) 
	{
		medianBlur(input, tmp, fSize);
		input = tmp;
	}

	filtedImage = input; 
}

int BEEImageData::CreateLabeledImage(Mat image, Mat label, int indL, Mat & label_ind_img)
{
	// create images with label == indL
	Mat mask = (label == indL);	
	Mat maskedImage = Mat::zeros(image.rows, image.cols, CV_8UC3);
	image.copyTo(maskedImage, mask);

#if 1
	char fname_masked[20];
	sprintf(fname_masked, "masked_%3.3d.jpg", indL);
	//imwrite(fname_masked, maskedImage);
#endif

	int iter_filt = 10;
	Mat filtedImage;
	FilterImageMedian(maskedImage, maskedImage, iter_filt);

	// Perform color segmentation on the current labeled/masked image
	Mat segment, segment_relabeled;
	SET::SegmentFullImage(maskedImage, segment, 
		SET::SetSegmentationParams(0.8, 500, 150));
	
	ReLabelImage(segment, segment_relabeled);

	vector<float> unique_color = unique_label<float>(segment_relabeled, mask);

	label_ind_img = Mat::zeros(mask.rows, mask.cols, CV_8UC1);

	int count = 0;
	for (size_t i = 0; i < unique_color.size(); i++)
	{
		Mat mask_seg_color, mask_seg;

		compare(segment_relabeled, unique_color[i], mask_seg_color, CMP_EQ);
		mask_seg_color.convertTo(mask_seg, CV_8UC1);
		
		//cvtColor(mask_seg_color, mask_seg, CV_BGR2GRAY);

		Scalar meanColor = mean(image, mask_seg);

		// ignore background pixels 
		if (255 - meanColor[0] < 20 && 255 - meanColor[1] < 20 && 255 - meanColor[2] < 20 )				
			continue;

		// make sure not to include irrelevant pixels
		bitwise_and(mask, mask_seg, mask_seg);

		count = count + 1; 
		label_ind_img.setTo(count, mask_seg);	
	}

	FilterImageMedian(label_ind_img, label_ind_img, iter_filt);
	return (unique_color.size());
}



template<typename T>
vector<T> BEEImageData::unique_label(const Mat & input, const Mat mask)
{
	vector<T> out;

	// uchar *ptr = (uchar *) input.data;
	// size_t elem_step = input.step / sizeof(uchar);

	for (int y = 0; y < input.rows; ++y)
	{
		for (int x = 0; x < input.cols; ++x)
		{
			if (mask.empty() || mask.at<uchar>(y, x) > 0 )
			{
				T value = input.at<T>(y, x);
				//T value = ptr[y * elem_step + x] ;

				if (find(out.begin(), out.end(), value) == out.end() )
					out.push_back(value);
			}
		}
	}

	return out;
}


vector<float> BEEImageData::unique_label_float(const Mat & input, const Mat mask)
{
	vector<float> out;

	for (int y = 0; y < input.rows; ++y)
	{
		for (int x = 0; x < input.cols; ++x)
		{
			if (mask.empty() || mask.at<uchar>(y, x) > 0 )
			{
				float value = input.at<float>(y, x);

				if (find(out.begin(), out.end(), value) == out.end() )
					out.push_back(value);
			}
		}
	}

	return out;
}



