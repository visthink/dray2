
#ifndef __BEE_EXTRACTOR_H__
#define __BEE_EXTRACTOR_H__

#include <opencv2/opencv.hpp>
#include "BEEImageData.h"
#include "BEEBlobData.h"
#include "BEEImageSegmentation.h"

using namespace std;
using namespace cv;

class BEEExtractor{
public:

	BEEExtractor();
	BEEExtractor(bool bValue);
	~BEEExtractor();
	
	friend class BBEBlobMatcher;

	void BEEExtractionFromImage(string str_fname);

	void ShowClusteringResult(const Mat& labels, const Mat& centers, int width, int height);
	
	const Mat GetHSVLabel() {return m_hsv_label;}
	
	int GetUniqueBlobIDs();
	void WriteOutBlobs(string fname_file, string fname_out);

private:

	void SetWidth(int value) { m_W = value;}	
	void SetHeight(int value) {m_H = value;}
	void SetRemoveBGFlag(const bool bValue) {m_isRemoveBG = bValue;}

	void Initialization(BEEImageData & data);

	void HSVClustering(BEEImageData & data);

	void EnhanceLabeling(const Mat image, const Mat label_ind_img, 
		Mat &enhanced_label_img, const int numLabels);

private:

	bool m_isRemoveBG;
	int m_num_hsv_clusters;

	int m_W;
	int m_H;

	// label image from hsv clustering
	Mat m_hsv_label;

	// segmentation with full image
	Mat m_segment_full;
	Mat m_segment_full_relabeled;

	// the extracted blobs
	BEEBLOBS m_blobs_all;

	// extracted texture blobs
	BEEBLOBS m_blobs_texture;

	// unique blob IDs
	int m_num_uniqueIDs;

	Mat m_res_image;

};

#endif

