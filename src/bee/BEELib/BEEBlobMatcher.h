#ifndef __BEE_BLOBMATCHER_H__
#define __BEE_BLOBMATCHER_H__

#include <opencv2/opencv.hpp>
#include "BEEImageData.h"

using namespace std;
using namespace cv;

typedef struct ThreshParams{
	int size_thresh;
	float shape_thresh;
	float color_thresh;
	float dratio_thresh;
	float ecc_thresh;
} TParams;

class BEEBlobMatcher{
public:

	BEEBlobMatcher();
	~BEEBlobMatcher(){;}

	void BlobClustering(const Mat image, BEEBlob & bblob, int & start_new_label);

	void UpdateBlobID(const Mat point_labels, BEEBlob & bblob, int & start_new_label);

	void BlobAccumulating(const BEEBlob bblob, BEEBLOBS & blobs_all);
	int BlobMerging(const Mat image, BEEBLOBS & blobs_all);

	int TextBlobMerging(const Mat image, BEEBLOBS & blobs_all);
	void MergeAdjcentTextBlobs(const Mat image, BEEBLOBS & blobs_all);

	void CheckOverlaps(const Mat image, BEEBLOBS & blobs_all);
	void CheckIntersect(const Mat image, BEEBLOBS & blobs_all);

	void GetDiffValues(const Mat points, const int dimensions,
		const int i, const int j,
		vector<float> & ddiff,  vector<float> & mmean, vector<float> &  dratio);

	bool isMergeS_Shape(const vector<float> dratio);
	bool isMergeS_Color(const vector<float> dratio, const int diff_pos = 0);
	bool isMergeN_Shape(const vector<float> dratio);
	bool isMergeN_Color(const vector<float> dratio, const int diff_pos = 0);

	bool isMergeS(const vector<float> dratio, const int diff_pos);
	bool isMergeN(const vector<float> dratio, const vector<float> ddiff);

	bool isSmallBlob(const BEEBLOB blob);
	bool isHugeBlob(const BEEBLOB blob);

	int GetStartGroupID(const BEEBLOBS blobs);
	int GetEndGroupID(const BEEBLOBS blobs);
	int UpdateBlobGroupIDs(BEEBLOBS & blobs);

private:

	void SetClusterParams();
	void SetMergeParams();

	void FillSamplePoints5(const BEEBLOBS blobs_all, Mat & points);	
	void FillSamplePoints7(const BEEBLOBS blobs_all, Mat & points);

	int huge_blob_size;
	int diff_pos_thresh;

	TParams m_clusterS_params;	// for small blobs
	TParams m_clusterN_params;	// for normal blobs
	
	TParams m_mergeS_params;	// for small blobs
	TParams m_mergeN_params;	// for normal blobs

	Mat m_points;
};

#endif