
#include <iterator>

#include "BEEExtractor.h"
#include "BEEBlobMatcher.h"

BEEExtractor::BEEExtractor():
m_isRemoveBG(0), m_num_hsv_clusters(10)
{
	// default not remove BG
	// number of labels in hsv clustering is 10

	if (!m_blobs_all.empty())
		m_blobs_all.clear();

	if (!m_blobs_texture.empty())
		m_blobs_texture.clear();

	m_num_uniqueIDs = 0;

}

BEEExtractor::BEEExtractor(bool bValue):
m_isRemoveBG(bValue)
{
	m_num_uniqueIDs = 0;
	m_num_hsv_clusters = 10;

	if (!m_blobs_all.empty())
		m_blobs_all.clear();

	if (!m_blobs_texture.empty())
		m_blobs_texture.clear();
}

BEEExtractor::~BEEExtractor()
{
	if (!m_hsv_label.empty())
		m_hsv_label.release();

	if (!m_segment_full.empty())
		m_segment_full.release();
}

void BEEExtractor::Initialization(BEEImageData & data)
{
	Mat image = data.GetRGBImage();

	SetWidth(data.GetImageWidth());
	SetHeight(data.GetImageHeight());

	// Gradient and Edges
	data.RawImageGradient(image);
	data.ComputeEdgeMask(image);

	// Perform color segmentation on original image
	SET::SegmentFullImage(image, this->m_segment_full, 
		SET::SetSegmentationParams(1.5, 300, 200));

	data.ReLabelImage(m_segment_full, m_segment_full_relabeled);
}

void BEEExtractor::BEEExtractionFromImage(string str_fname)
{

	BEEImageData bImgData(str_fname, m_isRemoveBG);

	Mat image = bImgData.GetRGBImage();

	Mat org_image = image.clone();

	// Pre-Processing: edge extraction and segmentation
	Initialization(bImgData);

	// Initial clustering on whole image in HSV space
	HSVClustering(bImgData);

	BEEBlobMatcher bmatcher;

	int start_new_label = 0;

	// for debugging 
	Mat dst = Mat::zeros(m_H, m_W, CV_8UC3);

	// Extraction blobs with rich texture
	BEEBlob bblob_texture;
	bblob_texture.TextureBlobFinder(image);
	
	m_blobs_texture = bblob_texture.GetTextureBlobs();
	start_new_label = bmatcher.BlobMerging(image, m_blobs_texture);
	start_new_label = bmatcher.TextBlobMerging(image, m_blobs_texture);

#if 1
	// show texture image
	//dst = Mat::zeros(m_H, m_W, CV_8UC3);
	dst = image.clone();
	bblob_texture.DrawBlobs(dst, m_blobs_texture);
	imwrite("dum0.jpg", dst);
#endif

	int TOTAL_ITER = 1;

	for (int iter = 0; iter < TOTAL_ITER; iter++)
	{
		for (int indL = 0; indL < m_num_hsv_clusters; indL++)
		{
			Mat label_ind_img;	
			int numLabels = bImgData.CreateLabeledImage(image, 
				GetHSVLabel(), 
				indL, 
				label_ind_img);

			imwrite("label_ind_img.jpg", label_ind_img);

			Mat enhanced_label_img;
			EnhanceLabeling(image, 
				label_ind_img, 
				enhanced_label_img, 
				numLabels);

			imwrite("enhanced_label_img.jpg", enhanced_label_img);

			BEEBlob bblob;
			bblob.BlobFinder(enhanced_label_img);

#if 1
			dst = Mat::zeros(m_H, m_W, CV_8UC3);
			bblob.DrawContours(dst);

			char fname_dum[20];
			sprintf(fname_dum, "dum_%3.3d.jpg", indL);
			imwrite(fname_dum, dst);
#endif

			bblob.BlobStatComp(image, 
				bImgData.GetEdgeMask(),
				bImgData.GetThreshedImage());

			bmatcher.BlobClustering(image, bblob, start_new_label);

			bmatcher.BlobAccumulating(bblob, m_blobs_all);
#if 1
			dst = Mat::zeros(m_H, m_W, CV_8UC3);
			bblob.DrawContours(dst, 1);

			char fname_dum1[20];
			sprintf(fname_dum1, "dum1_%3.3d.jpg", indL);
			imwrite(fname_dum1, dst);

#endif
		}



#if 1
		dst = Mat::zeros(m_H, m_W, CV_8UC3);
		//dst = bImgData.GetRGBImage().clone();
		bblob_texture.DrawBlobs(dst, m_blobs_all);
		imwrite("dum2.jpg", dst);
#endif
		if (iter == 0)
			copy(m_blobs_texture.begin(), m_blobs_texture.end(), back_inserter(m_blobs_all));

		start_new_label = bmatcher.BlobMerging(image, m_blobs_all);
		

#if 1
		dst = Mat::zeros(m_H, m_W, CV_8UC3);
		//dst = bImgData.GetRGBImage().clone();
		bblob_texture.DrawBlobs(dst, m_blobs_all);
		imwrite("dum3.jpg", dst);

		// overlay the blobs on the original image
		dst = org_image.clone();
		bblob_texture.DrawBlobs(dst, m_blobs_all, false);
		imwrite("dum4.jpg", dst);

#endif

		// Check the residue image
		Mat res_image = bblob_texture.WhiteOutImage(image, m_blobs_all);
		imwrite("res_image.jpg", res_image);

		image = res_image;

	}
}



void BEEExtractor::HSVClustering(BEEImageData & data)
{
	Mat matH = data.GetHSVChannel(0);

	// Mat matS = data.GetHSVChannel(1);
	// Not use Saturation
	Mat matS = Mat::zeros(matH.rows, matH.cols, CV_8UC1);

	Mat matV = data.GetHSVChannel(2);

	// OpenCV scaling for H is different from S & V
	// To be compatible with Matlab testing
	Mat matV_scaled;
	matV.convertTo(matV_scaled, CV_8UC1, 180.0/255.0);
	
	vector<Mat> channels;
	channels.push_back(matH);	
	channels.push_back(matS);
	channels.push_back(matV_scaled); 

	/// Merge the three channels
	Mat image;
	merge(channels, image);
	//image = data.GetRGBImage();

	Mat reshaped_image = image.reshape(1, image.cols * image.rows);
	//cout << "reshaped image: " << reshaped_image.rows << ", " << reshaped_image.cols << endl;
	assert(reshaped_image.type() == CV_8UC1);

	Mat reshaped_image32f;
	reshaped_image.convertTo(reshaped_image32f, CV_32FC1, 1.0 / 255.0);
	//cout << "reshaped image 32f: " << reshaped_image32f.rows << ", " << reshaped_image32f.cols << endl;
	assert(reshaped_image32f.type() == CV_32FC1);

	int cluster_number = 10;
	//TermCriteria criteria(TermCriteria::COUNT, 100, 1);
	TermCriteria criteria(CV_TERMCRIT_ITER|CV_TERMCRIT_EPS, 100, 1);

	Mat centers, labelsf;
	kmeans(reshaped_image32f, m_num_hsv_clusters, labelsf, criteria, 3, KMEANS_PP_CENTERS, centers);

	//ShowClusteringResult(labelsf, centers, image.cols, image.rows);

	m_hsv_label = Mat::zeros(image.rows, image.cols, CV_8UC1);
	labelsf = labelsf.reshape(1, image.rows);
	labelsf.convertTo(m_hsv_label, CV_8UC1, 1);

}


void BEEExtractor::ShowClusteringResult(const cv::Mat& labels, const cv::Mat& centers, int width, int height)
{
	std::cout << "===\n";
	std::cout << "labels: " << labels.rows << " " << labels.cols << std::endl;
	std::cout << "centers: " << centers.rows << " " << centers.cols << std::endl;
	assert(labels.type() == CV_32SC1);
	assert(centers.type() == CV_32FC1);

	cv::Mat rgb_image(height, width, CV_8UC3);
	cv::MatIterator_<cv::Vec3b> rgb_first = rgb_image.begin<cv::Vec3b>();
	cv::MatIterator_<cv::Vec3b> rgb_last = rgb_image.end<cv::Vec3b>();
	cv::MatConstIterator_<int> label_first = labels.begin<int>();

	cv::Mat centers_u8;
	centers.convertTo(centers_u8, CV_8UC1, 255.0);
	cv::Mat centers_u8c3 = centers_u8.reshape(3);

	while ( rgb_first != rgb_last ) {
		const cv::Vec3b& rgb = centers_u8c3.ptr<cv::Vec3b>(*label_first)[0];
		*rgb_first = rgb;
		++rgb_first;
		++label_first;
	}
	cv::imshow("tmp", rgb_image);
	cv::waitKey();
}


void BEEExtractor::EnhanceLabeling(const Mat image, const Mat label_ind_img, 
	Mat &enhanced_label_img, const int numLabels)
{
	enhanced_label_img = label_ind_img;

	int small_blob_size = 500;
	int large_blob_size = 2000;

	int small_blob_size_thresh = image.rows * image.cols * 0.001;
	int intersection_thresh = 50;

	Mat mask = (label_ind_img > 0);

	int morph_size = 5;

	Mat element = getStructuringElement(MORPH_ELLIPSE,
		Size( 2*morph_size + 1, 2*morph_size+1 ),
		Point( morph_size, morph_size ) );

	dilate(mask, mask, element);

	BEEImageData bImageData;	

	if (m_segment_full.empty())
	{
		SET::SegmentFullImage(image, m_segment_full, 
			SET::SetSegmentationParams(1.5, 300, 200));

		bImageData.ReLabelImage(m_segment_full, m_segment_full_relabeled);
	}

	vector<float> unique_color = bImageData.unique_label_float(m_segment_full_relabeled);

	Mat mask_both;

	for (int i = 1; i < numLabels; i++)
	{
		Mat maskL = (label_ind_img == i);
		double areaL = bImageData.GetMaskArea(maskL);

		// don't bother with very small blobs, will be discarded anyway
		if (areaL < small_blob_size && areaL > small_blob_size_thresh)
		{
			// check the intersection with original segmentation
			for (size_t s = 0; s < unique_color.size(); s++)
			{
				Mat mask_seg_color, mask_seg;

				compare(m_segment_full_relabeled, unique_color[s], mask_seg_color, CMP_EQ);				
				mask_seg_color.convertTo(mask_seg, CV_8UC1);

				medianBlur(mask_seg, mask_seg, 5);

				double areaS = bImageData.GetMaskArea(mask_seg);

				if (areaS == 0 || areaS > large_blob_size)
					continue;

				mask_seg = mask_seg > 0;
				mask_seg /= 255;

				maskL = maskL > 0;
				maskL /= 255;

				add(maskL, mask_seg, mask_both);

				mask_both = (mask_both == 2);

				//imwrite("mask_both.jpg", mask_both);

				double tmp_area = bImageData.GetMaskArea(mask_both);

				if (bImageData.GetMaskArea(mask_both) < intersection_thresh) continue;

				double solidityS = bImageData.GetMaskSolidity(mask_seg);
				if (solidityS < 0.5) continue;

				double solidityL = bImageData.GetMaskSolidity(maskL);
				if (solidityL > 0.5)
				{
					dilate(mask_seg, mask_seg, element);
					enhanced_label_img.setTo(i, mask_seg);
				}				

			}
		}
	}
	
}


int BEEExtractor::GetUniqueBlobIDs()
{
	m_num_uniqueIDs = 0;

	vector<int> myvector;
	for (size_t i = 0; i < m_blobs_all.size(); i++)
	{
		if (m_blobs_all[i].groupID >= 0)
			myvector.push_back(m_blobs_all[i].groupID);
	}

	std::vector<int>::iterator it;
	it = std::unique (myvector.begin(), myvector.end());   

	m_num_uniqueIDs = std::distance(myvector.begin(),it);

	int uniqueIDs = m_num_uniqueIDs;

	return uniqueIDs;
}

void BEEExtractor::WriteOutBlobs(string fname_file, string fname_out)
{
	GetUniqueBlobIDs();

	size_t NB = m_blobs_all.size();

	FILE * fp = fopen(fname_out.c_str(), "w");

	fprintf(fp, "(File \"%s\" \r\n", fname_file.c_str());

	fprintf(fp, "imageHeight %d \r\n", m_H);
	fprintf(fp, "imageWidth %d \r\n", m_W);

	fprintf(fp, "numBlobs %d \r\n", (int) NB);

	fprintf(fp, "numGroups %d \r\n", m_num_uniqueIDs);

	fprintf(fp, "Blobs \r\n");

	fprintf(fp, "\t( \r\n");

	for (int i = 0; i < NB; i++)
	{
		fprintf(fp, "\t\t (ID %d\r\n", i);    
		fprintf(fp, "\t\t  Area %f \r\n", m_blobs_all[i].area);
		fprintf(fp, "\t\t  Centroid (%d %d) \r\n", m_blobs_all[i].center.x, m_blobs_all[i].center.y);
		fprintf(fp, "\t\t  BoundingBox (%d %d %d %d) \r\n", 
			m_blobs_all[i].bbox.x, m_blobs_all[i].bbox.y,
			m_blobs_all[i].bbox.width, m_blobs_all[i].bbox.height);
		fprintf(fp, "\t\t  MajorAxisLength %f \r\n", m_blobs_all[i].major);
		fprintf(fp, "\t\t  MinorAxisLength %f \r\n", m_blobs_all[i].minor);
		fprintf(fp, "\t\t  Eccentricity %f\r\n", m_blobs_all[i].eccentricity);
		fprintf(fp, "\t\t  Solidity %f\r\n", m_blobs_all[i].solidity);
		fprintf(fp, "\t\t  MeanRed %f\r\n", m_blobs_all[i].meanColor[2]);
		fprintf(fp, "\t\t  MeanGreen %f\r\n", m_blobs_all[i].meanColor[1]);
		fprintf(fp, "\t\t  MeanBlue %f\r\n", m_blobs_all[i].meanColor[0]);
		fprintf(fp, "\t\t  isHighTexture %d\r\n", m_blobs_all[i].isHighTexture);
		fprintf(fp, "\t\t  GroupIndex %d\r\n", m_blobs_all[i].groupID);
		fprintf(fp, "\t\t ) \r\n");
	}

	fprintf(fp, "\t) \r\n");
	fprintf(fp, ")");

	fclose(fp);

}
