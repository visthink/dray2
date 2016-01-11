
#ifndef __BEE_IMAGEDATA_H__
#define __BEE_IMAGEDATA_H__

#include <string>
#include <vector>

#include <opencv2/opencv.hpp>

using namespace std;
using namespace cv;

class BEEImageData{
public:
	BEEImageData();
	BEEImageData(string str_fname, bool isRemoveBG = false);
	~BEEImageData();

	friend class BEEBlob;

	void RemoveBG();
	void Gradient_Initialization(const Mat image);

	void DisplayImage(Mat image);

	const Mat GetRGBImage() {return m_rgb_image;}
	const Mat GetHSVImage() {return m_hsv_image;}

	const Mat GetEdgeMask() {return m_edgeMask;}
	const Mat GetGradient() {return m_grad; }

	const Mat GetRGBChannel(int index);
	const Mat GetHSVChannel(int index);

	const int GetImageWidth() {return m_W;}
	const int GetImageHeight() {return m_H;}
	
	void ThreshImage(const Mat image, Mat & bthreshedImage);
	const Mat GetThreshedImage() {return m_bthreshedImage;}

	void ComputeEdgeMask(const Mat image);
	void ComputeEdgeMask(const Mat image, Mat & edgeMask);
	
	double GetMaskArea(const Mat mask);
	double GetMaskSolidity(const Mat label_ind_img);

	void RawImageGradient(const Mat image);
	void RawImageGradient(const Mat image, Mat & grad);

	void FiltedImageGradient(const Mat image, Mat & fG);
	void GrayImageGradient(const Mat src_gray, Mat & grad);

	void GradientMaskMask(const Mat gMaskGrad, const Mat gMask, Mat & gMaskGrad_mask);

	void FilterImageMedian(const Mat image, Mat &filtedImage, const int ITER = 10, const int fSize = 5);
	void DilateErodeImage(const Mat input, Mat & output, const int dsize = 2, const int esize = 2);
	void ErodeDilateImage(const Mat input, Mat & output, const int dsize = 2, const int esize = 2);

	void GetGMask(int val) { m_gMask = m_grad > val; }

	int CreateLabeledImage(Mat image, Mat label, int indL, Mat & label_ind_img);

	void ReLabelImage(const Mat in, Mat & out);

	template<typename T>
		vector<T> unique_label(const Mat & input, const Mat mask = cv::Mat());

	vector<float> unique_label_float(const Mat & input, const Mat mask = cv::Mat());

protected:
	
	bool m_isRemoveBG;

	Mat m_grad;				// Image gradient
	Mat m_fG;				// Filtered image gradient

	Mat m_gMask;			// Mask after thresholding m_grad
	Mat m_gMaskGrad;		// Gradient on the masked image
	Mat m_gMaskGrad_mask;	// Mask after thresholding m_gMaskGrad

	Mat m_masked_image;		// Masked image with m_gMaskGrad_mask
	
	Scalar m_meanGrad;
	Scalar m_meanGMaskGrad;
	
	// segmentation for texture classification
	Mat m_segment_texture;
	Mat m_segment_texture_relabeled;

private:
	
	void SetImageWidth(int value) {m_W = value;}
	void SetImageHeight(int value) {m_H = value;}
	
	void ConvertRGB2HSV(const Mat & rgb, Mat & hsv);
	void SplitChannels(const Mat &image, vector<Mat> &planes);


private:
	int m_W, m_H; 
	
	Mat m_rgb_image;
	Mat m_hsv_image;
	Mat m_mask;

	Mat m_bthreshedImage;

	vector<Mat> m_bgr_planes;
	vector<Mat> m_hsv_planes;

	// mask from edge pixels
	Mat m_edgeMask;

	// residue image
	Mat m_res_image;
};

#endif
