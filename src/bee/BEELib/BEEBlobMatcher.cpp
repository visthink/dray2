

#include <numeric>
#include <iterator>

#include "BEEExtractor.h"
#include "BEEBlobData.h"
#include "BEEBlobMatcher.h"


BEEBlobMatcher::BEEBlobMatcher()
{
	SetClusterParams();
	SetMergeParams();
}

void BEEBlobMatcher::SetClusterParams()
{
	m_clusterS_params.size_thresh = 250;
	m_clusterS_params.dratio_thresh = 0.4;
	
	m_clusterN_params.dratio_thresh = 0.22;
	m_clusterN_params.color_thresh = 0.08;
}

void BEEBlobMatcher::SetMergeParams()
{
	diff_pos_thresh = 20;
	huge_blob_size = 10000;				// 100x100 blobs

	m_mergeS_params.size_thresh = 600;		// 20x30 blobs
	m_mergeS_params.shape_thresh = 0.35;
	m_mergeS_params.color_thresh = 0.4;	
		
	m_mergeN_params.dratio_thresh = 0.1;
	m_mergeN_params.ecc_thresh = 0.5;
	m_mergeN_params.shape_thresh = 0.25;
	m_mergeN_params.color_thresh = 0.1;	

}

void BEEBlobMatcher::BlobClustering(const Mat image, BEEBlob & bblob, int & start_new_label)
{
	SetClusterParams();

	int sampleCount = 1;
	int clusterCount = 3;	
	int dimensions = 5;

	for (int i = 0; i < bblob.numBlobs; i++)
	{		
		sampleCount = max(bblob.m_raw_blobs[i].groupID+1, sampleCount);
	}

	// Number of cluster cannot exceed number of blobs
	clusterCount = (sampleCount >= clusterCount) ? clusterCount: sampleCount;
	
	// Use more clusters if the initial number of blobs is large
	clusterCount = (sampleCount <= 8) ? clusterCount: 5;
	
	Mat points(sampleCount, dimensions, CV_32F,Scalar(10));
	Mat point_labels;
	Mat centers(clusterCount, 1, points.type());

	FillSamplePoints5(bblob.m_raw_blobs, points);
	
	kmeans(points, clusterCount, point_labels, 
		TermCriteria( CV_TERMCRIT_EPS+CV_TERMCRIT_ITER, 10, 1.0), 3, KMEANS_PP_CENTERS, centers);

	// now merge similar blobs
	vector<float> ddiff, mmean, dratio; 

	for (int i = 0; i < bblob.numBlobs; i++)
	{
		int ind_i = bblob.m_raw_blobs[i].groupID;
		if (ind_i < 0) continue;

		int ind_label_i = point_labels.at<int>(ind_i, 0);

		for (int j = i+1; j < bblob.numBlobs; j++)
		{
			int ind_j = bblob.m_raw_blobs[j].groupID;
			if (ind_j < 0) continue;

			int ind_label_j = point_labels.at<int>(ind_j, 0);
			
			if (ind_label_i == ind_label_j) continue;

			GetDiffValues(points, dimensions, ind_i, ind_j, ddiff, mmean, dratio);

			float max_dratio = *(std::max_element(dratio.begin(), dratio.end()));

			// for small blobs
			if (centers.at<float>(ind_label_i,0) < m_clusterS_params.size_thresh &&
				centers.at<float>(ind_label_j,0) < m_clusterS_params.size_thresh)				
			{
				if (max_dratio < m_clusterS_params.dratio_thresh)					
					point_labels.at<int>(ind_j, 0) = point_labels.at<int>(ind_i, 0);	
			}

			// for normal blobs
			if (max_dratio < m_clusterN_params.dratio_thresh)
			{
				point_labels.at<int>(ind_j, 0) = point_labels.at<int>(ind_i, 0);;
			}
		}
	}

	UpdateBlobID(point_labels, bblob, start_new_label);
}

void BEEBlobMatcher::UpdateBlobID(const Mat point_labels, BEEBlob & bblob, int & start_new_label)
{
	// prune the blobs and create new list
	int count = 0, maxID = 0;

	map<int, bool> isIdUsed;
	for (int i = 0; i < bblob.numBlobs; i++) isIdUsed[i] = false;

	for(int i = 0; i < bblob.numBlobs; i++)
	{
		int ind = bblob.m_raw_blobs[i].groupID;
		if (ind < 0) continue;

		int label_ind = point_labels.at<int>(ind, 0);
		isIdUsed[label_ind] = true;
	}
	
	map<int, bool>::iterator iter;
	vector<int> isSetGroupID(bblob.m_raw_blobs.size(), 0);

	for (iter = isIdUsed.begin(); iter != isIdUsed.end(); iter++)
	{
		if (iter->second) 
		{	
			for(int i = 0; i < bblob.numBlobs; i++)
			{
				if (isSetGroupID[i]) continue;

				int ind = bblob.m_raw_blobs[i].groupID;
				if (ind < 0) continue;

				int label_ind = point_labels.at<int>(ind, 0);

				if (label_ind == iter->first)
				{
					bblob.m_raw_blobs[i].groupID = maxID + start_new_label;
					isSetGroupID[i] = 1;
				}
			}
			maxID++;
		}
	}
	
	start_new_label += maxID;
}

void BEEBlobMatcher::BlobAccumulating(const BEEBlob bblob, BEEBLOBS & blobs_all)
{
	for (int i = 0; i < bblob.numBlobs; i++)
	{
		int ind = bblob.m_raw_blobs[i].groupID;
		if (ind < 0) continue;
		blobs_all.push_back(bblob.m_raw_blobs[i]);
	}
}


bool BEEBlobMatcher::isMergeS_Shape(const vector<float> dratio)
{
	bool isMerge = (*std::max_element(dratio.begin(), dratio.begin()+3) < m_mergeS_params.shape_thresh );	
	return (isMerge);
}

bool BEEBlobMatcher::isMergeS_Color(const vector<float> dratio, const int diff_pos)
{
	bool isMerge = ( (*std::max_element(dratio.begin()+4, dratio.end()) < m_mergeS_params.color_thresh) ||
					 (*std::min_element(dratio.begin()+4, dratio.end()) < m_mergeS_params.color_thresh - 0.2 &&
					  *std::max_element(dratio.begin()+4, dratio.end()) < m_mergeS_params.color_thresh + 0.2 &&
					  diff_pos < diff_pos_thresh) 
				   ); 	
	return (isMerge);
}

bool BEEBlobMatcher::isMergeS(const vector<float> dratio, const int diff_pos)
{
	bool isMerge = isMergeS_Shape(dratio) && isMergeS_Color(dratio, diff_pos);			
	return (isMerge);
}

bool BEEBlobMatcher::isMergeN_Shape(const vector<float> dratio)
{
	bool isMerge = (*std::max_element(dratio.begin(), dratio.begin()+3) < m_mergeN_params.shape_thresh);	
	return (isMerge);
}

bool BEEBlobMatcher::isMergeN_Color(const vector<float> dratio, const int diff_pos)
{
	bool isMerge = (*std::max_element(dratio.begin()+4, dratio.end()) < m_mergeN_params.color_thresh);	
	return (isMerge);
}


bool BEEBlobMatcher::isMergeN(const vector<float> dratio, const vector<float> ddiff)
{
	bool isMerge = isMergeN_Shape(dratio) && isMergeN_Color(dratio) && 
		(ddiff[3] < m_mergeN_params.ecc_thresh);

	return (isMerge);
}

bool BEEBlobMatcher::isSmallBlob(const BEEBLOB blob)
{
	return (blob.area < m_mergeS_params.size_thresh);
}


bool BEEBlobMatcher::isHugeBlob(const BEEBLOB blob)
{
	return (blob.area > huge_blob_size);
}

int BEEBlobMatcher::BlobMerging(const Mat image, BEEBLOBS & blobs_all)
{
	SetMergeParams();

	size_t Nblobs = blobs_all.size();

	vector<int> new_label;
	for (int i = 0; i < Nblobs; i++)
	{
		new_label.push_back(blobs_all[i].groupID);
	}
	int sampleCount = Nblobs;
	int dimensions = 7;

	Mat points(sampleCount, dimensions, CV_32F,Scalar(10));

	FillSamplePoints7(blobs_all, points);

	vector<float> ddiff, mmean, dratio; 

	for (int i = 0; i < Nblobs; i++)
	{
		for (int j = i+1; j < Nblobs; j++)
		{
			if (new_label[i] == new_label[j]) continue;

			GetDiffValues(points, dimensions, i, j, ddiff, mmean, dratio);

			// for small blobs
			if ( isSmallBlob(blobs_all[i]) && isSmallBlob(blobs_all[j]) )
			{
				int diff_pos = 100;

				Point center_i = blobs_all[i].center;
				Point center_j = blobs_all[j].center;

				diff_pos = std::max(abs(center_i.x - center_j.x), abs(center_i.y - center_j.y));

				if (isMergeS(dratio, diff_pos))					 
				{						
					new_label[j] = new_label[i];
					//printf("Small: changing i = %d j = %d \n", i, j);
				}

			}
			// one small blob, and the other one is big
			if ( ( isSmallBlob(blobs_all[i]) && !isSmallBlob(blobs_all[j]) ) ||
				 (!isSmallBlob(blobs_all[i]) &&  isSmallBlob(blobs_all[j]) ) )			
				continue;

			// for normal blobs
			if (isMergeN(dratio, ddiff))
			{
				new_label[j] = new_label[i];
				//printf("Normal: changing i = %d j = %d \n", i, j);
			}

			// for huge blobs (normally from texture blobs)
			if ( isHugeBlob(blobs_all[i]) && isHugeBlob(blobs_all[j]) )
			{
				if (isMergeN_Color(dratio))
				{
					new_label[j] = new_label[i];
					//printf("Huge: changing i = %d j = %d \n", i, j);
				}
			}
		}
	}

	for (int i = 0; i < Nblobs; i++)
	{
		blobs_all[i].groupID = new_label[i];
	}

	int end_ID = UpdateBlobGroupIDs(blobs_all);

	// Get the points so that we don't need to compute them again
	m_points = points;

	CheckOverlaps(image, blobs_all);
	CheckIntersect(image, blobs_all);

	return(end_ID);
}

int BEEBlobMatcher::TextBlobMerging(const Mat image, BEEBLOBS & blobs_all)
{
	SetMergeParams();

	size_t Nblobs = blobs_all.size();

	vector<int> new_label;
	for (int i = 0; i < Nblobs; i++)
	{
		new_label.push_back(blobs_all[i].groupID);
	}
	int sampleCount = Nblobs;
	int dimensions = 7;

	Mat points(sampleCount, dimensions, CV_32F,Scalar(10));

	FillSamplePoints7(blobs_all, points);

	vector<float> ddiff, mmean, dratio; 

	for (int i = 0; i < Nblobs; i++)
	{
		for (int j = i+1; j < Nblobs; j++)
		{
			if (new_label[i] == new_label[j]) continue;

			GetDiffValues(points, dimensions, i, j, ddiff, mmean, dratio);

			// for small blobs
			if ( isSmallBlob(blobs_all[i]) && isSmallBlob(blobs_all[j]) )
			{
				if (isMergeS_Color(dratio))						 
				{						
					new_label[j] = new_label[i];
					//printf("Small: changing i = %d j = %d \n", i, j);
				}
			}
			// one small blob, and the other one is big
			if ( ( isSmallBlob(blobs_all[i]) && !isSmallBlob(blobs_all[j]) ) ||
				 (!isSmallBlob(blobs_all[i]) &&  isSmallBlob(blobs_all[j]) ) )			
				continue;

			// for normal blobs
			if (isMergeN_Color(dratio))
			{
				new_label[j] = new_label[i];
				//printf("Normal: changing i = %d j = %d \n", i, j);
			}

			// for huge blobs (normally from texture blobs)
			if ( isHugeBlob(blobs_all[i]) && isHugeBlob(blobs_all[j]))
			{
				if (isMergeN_Color(dratio))
				{
					new_label[j] = new_label[i];
					//printf("Huge: changing i = %d j = %d \n", i, j);
				}
			}
		}
	}

	MergeAdjcentTextBlobs(image, blobs_all);

	for (int i = 0; i < Nblobs; i++)
	{
		blobs_all[i].groupID = new_label[i];
	}

	int end_ID = UpdateBlobGroupIDs(blobs_all);

	return(end_ID);
}

void BEEBlobMatcher::MergeAdjcentTextBlobs(const Mat image, BEEBLOBS & blobs_all)
{
	size_t Nblobs = blobs_all.size();
	int dist_thresh = 20;

	for (int i = 0; i < Nblobs; i++)
	{
		for (int j = i+1; j < Nblobs; j++)
		{
			if (blobs_all[i].groupID != blobs_all[i].groupID) continue;

			bool isMerge = false;
			// only merge blobs with the same ID

			float dx = fabs((float)(blobs_all[i].center.x - blobs_all[j].center.x));
			float dy = fabs((float)(blobs_all[i].center.y - blobs_all[j].center.y));
		
			if (dx > dist_thresh && dy > dist_thresh) continue; 

			if (dx <= dist_thresh)
			{
				int ind_U = (blobs_all[i].bbox.y < blobs_all[j].bbox.y) ? i:j;
				int ind_D = (ind_U == i) ? j:i;

				int gap = max(0, blobs_all[ind_D].bbox.y - (blobs_all[ind_U].bbox.y + blobs_all[ind_U].bbox.height));
				if (gap <= dist_thresh) isMerge = true;				
			}

			if (dy <= dist_thresh)
			{
				int ind_L = (blobs_all[i].bbox.x < blobs_all[j].bbox.x) ? i:j;
				int ind_R = (ind_L == i) ? j:i;

				int gap = max(0, blobs_all[ind_R].bbox.x - (blobs_all[ind_L].bbox.x + blobs_all[ind_L].bbox.width));
				if (gap <= dist_thresh) isMerge = true;
			}

			if (isMerge)
			{				
				vector<Point> mpoints = blobs_all[i].points;
				copy(blobs_all[j].points.begin(), blobs_all[j].points.end(), back_inserter(mpoints));

				// color should be similar any way, take simple average
				blobs_all[i].meanColor += blobs_all[j].meanColor; 
				blobs_all[i].meanColor /= 2.0f;

				Moments mu_one;
				Point2f mc_one;

				mu_one = moments(Mat(mpoints, false));	
				mc_one = Point2f(mu_one.m10/mu_one.m00, mu_one.m01/mu_one.m00);

				blobs_all[i].points = mpoints;
				blobs_all[i].center = mc_one;
				blobs_all[i].area = mu_one.m00;
				blobs_all[i].bbox = boundingRect(Mat(mpoints));
				
				// These attributes remain the same, no need to update

				//blobs_all[i].hasHole = hasHole;
				//blobs_all[i].groupID = count;
				//blobs_all[i].isHighTexture = true;
				//blobs_all[i].Overlapped = 0;

				RotatedRect minEllipse;
				if (mpoints.size() > 5)
				{					
					minEllipse = fitEllipse(mpoints);
				} else
				{
					minEllipse.size.width = 1;
					minEllipse.size.height = 1;
					minEllipse.angle = 0;
				}

				float aa, bb;
				blobs_all[i].major = minEllipse.size.width;
				blobs_all[i].minor = minEllipse.size.height;
				blobs_all[i].orientation = minEllipse.angle;

				aa = std::max(minEllipse.size.width, minEllipse.size.height);
				bb = std::min(minEllipse.size.width, minEllipse.size.height);	

				blobs_all[i].eccentricity = sqrt(1-(bb*bb)/(aa*aa));

				// invalidate blobs j
				blobs_all[j].Overlapped = i;
			}
		}
	}

}

void BEEBlobMatcher::CheckOverlaps(const Mat image, BEEBLOBS & blobs_all)
{

	size_t Nblobs = blobs_all.size();
	
	int sampleCount = Nblobs;
	int dimensions = 7;

	Mat points(sampleCount, dimensions, CV_32F,Scalar(10));

	FillSamplePoints7(blobs_all, points);

	vector<float> ddiff, mmean, dratio; 

	for (int i = 0; i < Nblobs; i++)
	{
		for (int j = i+1; j < Nblobs; j++)
		{

			GetDiffValues(points, dimensions, i, j, ddiff, mmean, dratio);

			float area_diff = fabs(blobs_all[i].area - blobs_all[j].area) / (blobs_all[i].area + blobs_all[j].area);
			float dx = fabs((float)(blobs_all[i].center.x - blobs_all[j].center.x));
			dx /= (float) (blobs_all[i].center.x + blobs_all[j].center.x);
			float dy = fabs((float)(blobs_all[i].center.y - blobs_all[j].center.y));
			dy /= (float) (blobs_all[i].center.y + blobs_all[j].center.y);
			
			if ( area_diff < 0.15 && dx < 0.08 && dy < 0.08)
			{
				// for normal blobs
				// dratio(0) is the area difference, should be small already
				if (isMergeN_Shape(dratio) && isMergeN_Color(dratio) &&
					(ddiff[3] < max(m_mergeN_params.ecc_thresh - 0.4, 0.1)))
				{
					blobs_all[j].Overlapped = i;

					if (blobs_all[i].groupID != blobs_all[i].groupID) 
					{
						// TBD: Further processing
					}
				}
			}

		}
	}
}

void BEEBlobMatcher::CheckIntersect(const Mat image, BEEBLOBS & blobs_all)
{
	size_t Nblobs = blobs_all.size();
	
	int sampleCount = Nblobs;
	int dimensions = 7;

	Mat points(sampleCount, dimensions, CV_32F,Scalar(10));

	FillSamplePoints7(blobs_all, points);

	vector<float> ddiff, mmean, dratio; 

	for (int i = 0; i < Nblobs; i++)
	{
		Rect_<int> rectI = blobs_all[i].bbox;

		vector< Rect_<int> > in_rects;
		vector<int> in_labels;

		rectI = rectI - Point_<int>(10, 10);
		rectI = rectI + Size_<int>(20,20);

		for (int j = 0; j < Nblobs; j++)
		{
			if (i == j) continue;

			if (blobs_all[j].Overlapped == i) continue;

			Rect_<int> rectJ = blobs_all[j].bbox;

			if (rectJ <= rectI) 
			{
				in_rects.push_back(rectJ);
				in_labels.push_back(j);
			}
		}
		
		if (in_rects.empty()) continue;

		Rect_<int> rect_union;
		for (size_t ind_R = 0; ind_R < in_rects.size(); ind_R++)
		{
			rect_union = rect_union | in_rects[ind_R];
		}

		Rect_<int> rect_all = rect_union & rectI;

		if ((float)rect_all.area() / (float) rectI.area() > 0.75)
			blobs_all[i].Overlapped = 1000;
	}
}

int BEEBlobMatcher::GetStartGroupID(const BEEBLOBS blobs)
{
	int start_ID = 1000;
	for (size_t i = 0; i < blobs.size(); i++)
	{
		start_ID = min(blobs[i].groupID, start_ID);
	}

	return start_ID;
}

int BEEBlobMatcher::GetEndGroupID(const BEEBLOBS blobs)
{
	int end_ID = 0;
	for (size_t i = 0; i < blobs.size(); i++)
	{
		end_ID = max(blobs[i].groupID, end_ID);
	}

	return end_ID;
}

int BEEBlobMatcher::UpdateBlobGroupIDs(BEEBLOBS & blobs)
{
	if (blobs.empty()) return(0);

	int start_ID = GetStartGroupID(blobs);
	int end_ID = GetEndGroupID(blobs);
	
	vector<int> isUsed(end_ID - start_ID + 1, 0);
	vector<int> newID(end_ID - start_ID + 1, 0);

	/*
	FILE *fp;
	fp = fopen("dum1.txt", "w");
	
	for (int i = 0; i < blobs.size(); i++)
	{
		fprintf(fp, "%d  ", blobs[i].groupID);
	}
	fprintf(fp, "\n");
	fclose(fp);
	*/

	int count = start_ID;
	for (size_t i = 0; i < blobs.size(); i++)
	{
		int ind = blobs[i].groupID-start_ID;

		if (!isUsed[ind])
		{
			blobs[i].groupID = count;
			newID[ind] = count++;
			isUsed[ind] = true;
		} else
		{
			blobs[i].groupID = newID[ind] ;
		}
	}

	/*
	fp = fopen("dum2.txt", "w");
	for (int i = 0; i < blobs.size(); i++)
	{
		fprintf(fp, "%d  ", blobs[i].groupID);
	}
	fprintf(fp, "\n");

	fclose(fp);
	*/

	return(count);
}

void BEEBlobMatcher::FillSamplePoints7(const BEEBLOBS blobs_all, Mat & points)
{
	// fill the points with blob features
	for(size_t i = 0; i < blobs_all.size(); i++)
	{
		int ind = blobs_all[i].groupID;		
		if (ind < 0) continue;

		// Feature 0: Blob Area
		points.at<float>(i,0) = blobs_all[i].area;
		// Feature 1: Blob Major Axis
		points.at<float>(i,1) = blobs_all[i].major;
		// Feature 2: Blob Minor Axis
		points.at<float>(i,2) = blobs_all[i].minor;
		// Feature 3: eccentricity
		points.at<float>(i,3) = blobs_all[i].eccentricity;
		// Feature 4: Blob mean R 
		points.at<float>(i,4) = blobs_all[i].meanColor[2];
		// Feature 5: Blob mean G
		points.at<float>(i,5) = blobs_all[i].meanColor[1];
		// Feature 6: Blob mean B
		points.at<float>(i,6) = blobs_all[i].meanColor[0];
	}
}

void BEEBlobMatcher::FillSamplePoints5(const BEEBLOBS blobs_all, Mat & points)
{
	// fill the points with blob features
	for(size_t i = 0; i < blobs_all.size(); i++)
	{
		int ind = blobs_all[i].groupID;
		if (ind < 0) continue;

		// Feature 0: Blob Area
		points.at<float>(ind,0) = blobs_all[i].area;
		// Feature 1: Blob Major Axis
		points.at<float>(ind,1) = blobs_all[i].major;
		// Feature 2: Blob Minor Axis
		points.at<float>(ind,2) = blobs_all[i].minor;
		// Feature 3: Blob mean R 
		points.at<float>(ind,3) = blobs_all[i].meanColor[2];
		// Feature 4: Blob mean G
		points.at<float>(ind,4) = blobs_all[i].meanColor[1];
	}

}


void BEEBlobMatcher::GetDiffValues(const Mat points, const int dimensions, 
	const int i, const int j,
	vector<float> & ddiff,  vector<float> & mmean, vector<float> &  dratio)
{
	if (!ddiff.empty()) ddiff.clear();
	if (!mmean.empty()) mmean.clear();
	if (!dratio.empty()) dratio.clear();

	for (int ind_dim = 0; ind_dim < dimensions; ind_dim++)
	{
		float dvalue = fabs(points.at<float>(i, ind_dim) - points.at<float>(j, ind_dim));
		ddiff.push_back(dvalue);

		float mvalue = (points.at<float>(i, ind_dim) + points.at<float>(j, ind_dim))/2.0f;
		mmean.push_back(mvalue);

		float rvalue = dvalue / mvalue;
		dratio.push_back(rvalue);
	}

}
