
#ifndef __BEE_BLOBDATA_H__
#define __BEE_BLOBDATA_H__

#include <stdio.h>
#include <string>
#include <vector>
#include <functional>
#include <algorithm>

#include <opencv2/opencv.hpp>

#include "BEEImageData.h"
#include "BEEImageSegmentation.h"

using namespace std;
using namespace cv;

struct BEEBLOB{
	float area;				//total number of pixels
	CvRect bbox;			//bounding box [left upper width height]
	Point center;			//centroid_x, centroid_y
	float major, minor;		//major and minor axis length
	float orientation;	
	float eccentricity;
	float solidity;
	vector<Point> points;	//pixels belong to the blob
	Scalar meanColor;
	int groupID;
	bool isHighTexture;
	bool hasHole;
	int Overlapped;			// indicate which blob overlapped with
	vector< vector<Point> > hole_contours;
};

typedef vector<BEEBLOB> BEEBLOBS;
typedef vector<BEEBLOBS> BEEBLOBSGROUP;


class BEEBlob{
public:
	BEEBlob();
	~BEEBlob(){;}

	friend class BEEBlobMatcher;
	
	const BEEBLOBS GetTextureBlobs() {return m_texture_blobs;}
	const BEEBLOBS GetRawBlobs() {return m_raw_blobs;}

	void BlobFinder(const Mat label);
	void TextureBlobFinder(const Mat image);

	void BlobStatComp(const Mat image, const Mat edgeMask, const Mat bthreshedImage);
	
	float ComputeSolidity(vector<Point> cnt);

	void DrawContours(Mat &dst, bool hasID = false);
	void DrawBlobs(Mat &dst, BEEBLOBS blobs, bool is_show_blobs = true);

	const Mat WhiteOutImage(const Mat image, const BEEBLOBS blobs);

private:

	void Initialization(const Mat image);

	bool GetContourAttributes(const vector< vector<Point> > contours_one, 
		const vector<Vec4i> hierarchy_one, 
		const int index, const float size_thresh, 
		Moments & mu_one, Point2f & mc_one, 
		vector< vector<Point> > & hole_contours, bool & hasHole);

    int numBlobs;

	BEEBLOBS m_raw_blobs;
	BEEBLOBS m_texture_blobs;

	int thresh_brightColor;
	int thresh_size;
	float thresh_edge_ratio; 
	int thresh_small_blob;

	BEEImageData bImageData;

};

//  computeSquare returns an integer
static inline int computeSquare (Point pts) { return pts.x * pts.x + pts.y * pts.y; }

struct CmpSquareNorm {
	int operator()(const Point &pts) const
	{
		return pts.x * pts.x + pts.y * pts.y ;
	}
};

template<typename _Tp> inline bool
operator <= (const Rect_<_Tp>& r1, const Rect_<_Tp>& r2)
{
    return (r1 & r2) == r1;
}


#endif
