
#include "BEEBlobData.h"
#include <numeric>


RNG rng(12345);

BEEBlob::BEEBlob():
	thresh_brightColor(40),
	thresh_size(100),
	//thresh_edge_ratio(0.25),
	thresh_edge_ratio(0.40),
	thresh_small_blob(300)
{	
	m_raw_blobs.clear();
}

bool BEEBlob::GetContourAttributes(const vector< vector<Point> > contours_one, 
	const vector<Vec4i> hierarchy_one, 
	const int index, const float size_thresh, 
	Moments & mu_one, Point2f & mc_one, 
	vector< vector<Point> > & hole_contours, bool & hasHole)
{
	float diff_thresh = 0.4;	

	mu_one = moments(Mat(contours_one[index]), false );

	if (fabs(mu_one.m00) < size_thresh) return(false);

	mc_one = Point2f(mu_one.m10/mu_one.m00, mu_one.m01/mu_one.m00);

	// check if it is a hole itself
	if (hierarchy_one[index][3] != -1) return(false);

	// check if there are holes inside
	for (size_t ind_H = 0; ind_H < hierarchy_one.size(); ind_H++)
	{	
		// if a contour's parent is the current contour, it is a hole
		if (hierarchy_one[ind_H][3] == index && ind_H != index)
		{
			Moments mu_hole = moments(Mat(contours_one[ind_H]), false ); 
			float area_diff = fabs(mu_one.m00 - mu_hole.m00)/(mu_one.m00 + mu_hole.m00) *2.0f;

			// ignore small and big holes 
			if (area_diff > 0.4 && area_diff < 0.9)
			{
				hole_contours.push_back(contours_one[ind_H]);
				hasHole = true;
			}
		}
	}

	if (hasHole) return(false);
	
	return(true);
}

void BEEBlob::BlobFinder(const Mat label)
{
	numBlobs = 0;

	//Convert label image to GrayScale if it is not already
	if (label.channels() == 3)
		cvtColor(label, label, CV_BGR2GRAY); 

	double minVal, maxVal;
	minMaxLoc(label, &minVal, &maxVal); 

	float size_thresh = label.rows * label.cols * 0.001;
	//float size_thresh = label.rows * label.cols * 0.0005;
	
	vector< vector<Point> > contours_one;
	vector<Vec4i> hierarchy_one;
	vector< vector<Point> > hole_contours;

	Moments mu_one;
	Point2f mc_one;

	m_raw_blobs.clear();

	// ind = 0 is the background
	for (int ind = 1; ind <= maxVal; ind++)
	{
		Mat img = (label ==  ind);

		contours_one.clear();
		hierarchy_one.clear();

		findContours(img, contours_one, hierarchy_one, CV_RETR_CCOMP, CV_CHAIN_APPROX_SIMPLE ); //Find the Contour BLOBS
		
		for(size_t i = 0; i < contours_one.size(); i++ )
		{ 
			bool hasHole = 0;
			hole_contours.clear();

			bool isGood = GetContourAttributes(contours_one, hierarchy_one, i, 
				size_thresh, mu_one, mc_one, 
				hole_contours, hasHole);
		
			if (!isGood) continue;

			BEEBLOB blob_one;

			blob_one.points = contours_one[i];
			blob_one.center = mc_one;
			blob_one.area = mu_one.m00;
			blob_one.bbox = boundingRect(Mat(contours_one[i]));
			blob_one.hasHole = hasHole;
			blob_one.hole_contours = hole_contours;
			blob_one.isHighTexture = false;
			blob_one.Overlapped = 0;

			m_raw_blobs.push_back(blob_one);
		}
	}

	numBlobs = m_raw_blobs.size();
}

void BEEBlob::Initialization(const Mat image)
{
	bImageData.Gradient_Initialization(image);
}

void BEEBlob::TextureBlobFinder(const Mat image)
{
	Initialization(image);

	float grad_thresh_big = bImageData.m_meanGrad[0];
	float grad_thresh_gen = 20*3;

	int small_size_thresh = 900;	// 30x30 pixels consided too small for texture blobs
	int size_thresh = 10000;		// 100x100 pixels considered big blobs

	float solidity_thresh_gen = 0.4;
	float solidity_thresh_big = 0.35;

	vector<float> unique_color = bImageData.unique_label_float(bImageData.m_segment_texture_relabeled);
	
	for (size_t i = 0, count = 0; i < unique_color.size(); i++)
	{
		Mat mask_seg_color, mask_seg;

		compare(bImageData.m_segment_texture_relabeled, unique_color[i], mask_seg_color, CMP_EQ);
		mask_seg_color.convertTo(mask_seg, CV_8UC1);

		Scalar mean_grad_mag = mean(bImageData.m_grad, mask_seg);
		Scalar mean_fil_grad_mag = mean(bImageData.m_fG, mask_seg);
		Scalar mean_gmaskgrad_mag = mean(bImageData.m_gMaskGrad, mask_seg);

        //  need to make sure Scalar type matching
        //	Scalar numPix = sum(mask_seg)/255;
		Scalar numPix = sum(mask_seg);
		numPix[0] = numPix[0]/255;
		int size_seg = numPix[0];

		// don't consider tiny segments
		if (size_seg < small_size_thresh) continue;

		char fname[20];
		sprintf(fname, "mask_seg%2.2d.jpg", (int) i);
		//imwrite(fname, mask_seg);

		//printf("i = %d, grad = %f, fil = %f,  gmaskgrad = %f \n", 
			//(int) i, mean_grad_mag[0], mean_fil_grad_mag[0], mean_gmaskgrad_mag[0]);

		double solidity_seg = bImageData.GetMaskSolidity(mask_seg);

		if ( (( mean_grad_mag[0] > grad_thresh_big*1.5 && size_seg > 10000 ||
			    mean_grad_mag[0] > grad_thresh_gen*1.5 && 
				mean_fil_grad_mag[0] > grad_thresh_gen*1.5 ||
				(mean_grad_mag[0] > grad_thresh_gen*0.8 && 
				 mean_gmaskgrad_mag[0] > bImageData.m_meanGMaskGrad[0] * 0.4)
			  ) && 
			  (solidity_seg  > solidity_thresh_gen || 
			   solidity_seg  > solidity_thresh_big && size_seg > 20000
			  )
			 ) ||
			 (mean_gmaskgrad_mag[0] > bImageData.m_meanGMaskGrad[0] && solidity_seg  > 0.2)
			)
		{
			vector<vector<Point> > contours_one;
			vector<Vec4i> hierarchy_one;
			vector< vector<Point> > hole_contours;

			Moments mu_one;
			Point2f mc_one;

			findContours(mask_seg, contours_one, hierarchy_one, CV_RETR_CCOMP, CV_CHAIN_APPROX_SIMPLE );
		
			for(size_t j = 0; j < contours_one.size(); j++ )
			{ 
				bool hasHole = 0;
				hole_contours.clear();

				bool isGood = GetContourAttributes(contours_one, hierarchy_one, j, 
					small_size_thresh, mu_one, mc_one, 
					hole_contours, hasHole);

				if (!isGood || hasHole) continue;

				count = count + 1;

				BEEBLOB blob_one;
				blob_one.meanColor = mean(image, mask_seg);
				blob_one.points = contours_one[j];
				blob_one.center = mc_one;
				blob_one.area = mu_one.m00;
				blob_one.bbox = boundingRect(Mat(contours_one[j]));
				blob_one.hasHole = hasHole;
				blob_one.groupID = count;
				blob_one.isHighTexture = true;
				blob_one.Overlapped = 0;

				RotatedRect minEllipse;
				if (contours_one[j].size() > 5)
				{					
					minEllipse = fitEllipse(contours_one[j]);
				} else
				{
					minEllipse.size.width = 1;
					minEllipse.size.height = 1;
					minEllipse.angle = 0;
				}

				float aa, bb;
				blob_one.major = minEllipse.size.width;
				blob_one.minor = minEllipse.size.height;
				blob_one.orientation = minEllipse.angle;

				aa = std::max(minEllipse.size.width, minEllipse.size.height);
				bb = std::min(minEllipse.size.width, minEllipse.size.height);	

				blob_one.eccentricity = sqrt(1-(bb*bb)/(aa*aa));

				m_texture_blobs.push_back(blob_one);
			}
		}
	}
}


void BEEBlob::BlobStatComp(const Mat image, const Mat edgeMask, const Mat bthreshedImage)
{
	int half_size = 3;

	vector<Mat> planes;
	split(image, planes);

	Mat mask = Mat::zeros(image.rows, image.cols, CV_8UC1);
	Scalar color(255,  255,  255 );

	Mat maskedImage = Mat::zeros(image.rows, image.cols, CV_8UC1);

	thresh_size = mask.cols * mask.rows * 0.0005;
	thresh_small_blob = mask.cols * mask.rows * 0.0015;

	int count = 0;
	for (int i = 0; i < numBlobs; i++)
	{
		// get the ROI for the current blob
		mask.setTo(Scalar(0));

		vector< vector<Point> > contour;
		contour.push_back(m_raw_blobs[i].points);

		// For foreground
		color = Scalar(255,  255,  255);
		drawContours(mask, contour, 0, color, CV_FILLED, 8);

		//imwrite("mask01.jpg", mask);

		if (m_raw_blobs[i].hasHole)
		{
			color = Scalar(0, 0, 0);
			vector< vector<Point> > holes = m_raw_blobs[i].hole_contours;

			for (size_t idh = 0; idh < holes.size(); idh++)
				drawContours(mask, holes, idh, color, CV_FILLED, 8);
		}

		Scalar meanColor = mean(image, mask);

		if (255 - meanColor[0] < thresh_brightColor &&
			255 - meanColor[1] < thresh_brightColor &&
			255 - meanColor[2] < thresh_brightColor )
		{
			m_raw_blobs[i].groupID = -1;
			continue;
		}

		maskedImage.setTo(Scalar(0));
		bthreshedImage.copyTo(maskedImage, mask);
		
		//imwrite("maskedImage.jpg", maskedImage);

        //  need to make sure Scalar type matching
		//  Scalar numPix = sum(maskedImage)/255; 
		Scalar numPix = sum(maskedImage); 
		numPix[0] = numPix[0]/255;
		if (numPix[0] < thresh_size)
		{
			m_raw_blobs[i].groupID = -1;
			continue;
		}

		Mat maskedEdgeMask, maskedTmp;
		bthreshedImage.copyTo(maskedTmp, edgeMask);
		maskedTmp.copyTo(maskedEdgeMask, mask);

		//imwrite("maskedEdgeMask.jpg", maskedEdgeMask);

        //  need to make sure Scalar type matching
		//  Scalar numEdgePix = sum(maskedEdgeMask)/255; 
		Scalar numEdgePix = sum(maskedEdgeMask); 
		numEdgePix[0] = numEdgePix[0]/255;

		vector<Point> cnt = m_raw_blobs[i].points;

		float edge_ratio = numEdgePix[0] / numPix[0];

		if ((edge_ratio > thresh_edge_ratio && numPix[0] > thresh_small_blob) || cnt.size() <= 5)
		{
			m_raw_blobs[i].groupID = -1;
			continue;
		}

		Point zero(0.0f, 0.0f);
		Point sum  = std::accumulate(cnt.begin(), cnt.end(), zero);
		Point center(sum.x / cnt.size(), sum.y / cnt.size());

		vector<Point> diff(cnt);
		transform(diff.begin(), diff.end(), diff.begin(), bind2nd( minus<Point>(), center) );

		vector<int> sqr_diff(diff.size());
		transform(diff.begin(), diff.end(), sqr_diff.begin(), CmpSquareNorm());

		int min_dist =  *min_element(sqr_diff.begin(), sqr_diff.end());

		Mat mask_small = Mat::zeros(image.rows, image.cols, CV_8UC1);
		Rect rect = Rect(center.x - half_size, center.y - half_size, 2*half_size+1, 2*half_size+1);
		mask_small(rect) = 1;

		double minR, minG, minB, maxX;

		minMaxLoc(planes[0], &minB, &maxX, NULL, NULL, mask_small);
		minMaxLoc(planes[1], &minG, &maxX, NULL, NULL, mask_small);
		minMaxLoc(planes[2], &minR, &maxX, NULL, NULL, mask_small);

		if (min_dist > 25 && minR > 240 && minG > 240 && minB > 240)
		{
			m_raw_blobs[i].groupID = -1;
			continue;
		}

		// Compute Solidity		
		float solidity = ComputeSolidity(cnt);

		if (solidity < 0.5)
		{
			m_raw_blobs[i].groupID = -1;
			continue;
		}

		m_raw_blobs[i].area = numPix[0];
		
		m_raw_blobs[i].bbox = boundingRect(cnt);
		m_raw_blobs[i].center = center;
		
		m_raw_blobs[i].solidity = solidity;

		RotatedRect minEllipse;
		minEllipse = fitEllipse(cnt);

		float aa, bb;
		m_raw_blobs[i].major = minEllipse.size.width;
		m_raw_blobs[i].minor = minEllipse.size.height;
		m_raw_blobs[i].orientation = minEllipse.angle;

		aa = std::max(minEllipse.size.width, minEllipse.size.height);
		bb = std::min(minEllipse.size.width, minEllipse.size.height);	
		
		m_raw_blobs[i].eccentricity = sqrt(1-(bb*bb)/(aa*aa));
	
		m_raw_blobs[i].meanColor = mean(image, mask);
		m_raw_blobs[i].groupID = count++;
		m_raw_blobs[i].isHighTexture = false;
		m_raw_blobs[i].Overlapped = 0;

	}

}


float BEEBlob::ComputeSolidity(vector<Point> cnt)
{
	vector<Point> hull;
	convexHull(cnt, hull); 
	float hull_area = contourArea(hull);

	if (hull_area > 1)
		return(contourArea(cnt)/hull_area);

	return 0.0f;
}



void BEEBlob::DrawContours(Mat &dst, bool hasID)
{
	// iterate through all the top-level contours,
	// draw each connected component with its own random color

	vector<vector<Point> > contours;
	vector<int> hasHole;

	if (m_raw_blobs.empty()) return;

	map<int, int> raw_to_contourID;

	int count = 0;

	for(size_t idx = 0; idx < m_raw_blobs.size(); idx++ )
	{
		if (hasID)
			if (m_raw_blobs[idx].groupID < 0 || m_raw_blobs[idx].Overlapped) continue;
		
		contours.push_back(m_raw_blobs[idx].points);
		hasHole.push_back(m_raw_blobs[idx].hasHole);
		raw_to_contourID[idx] = count++;
	}

	// use the same color for debugging
	RNG rng( 0xFFFFFFFF );
	map<int, Scalar> ID_Color;

	for(size_t idx = 0; idx < m_raw_blobs.size(); idx++ )
	{
		if (hasID)
			if (m_raw_blobs[idx].groupID < 0 || m_raw_blobs[idx].Overlapped) continue;

		Scalar color(rng.uniform(0,255),  rng.uniform(0,255),  rng.uniform(0,255) );	 
		ID_Color[m_raw_blobs[idx].groupID] = color;
	}

	for(size_t idx = 0; idx < m_raw_blobs.size(); idx++ )
	{
		if (hasID) 
			if (m_raw_blobs[idx].groupID < 0 || m_raw_blobs[idx].Overlapped) continue;

		Scalar color;
		if (hasID)	
			color = ID_Color[m_raw_blobs[idx].groupID];
		else 
			color = Scalar(rng.uniform(0,255),  rng.uniform(0,255),  rng.uniform(0,255) );
		
		drawContours(dst, contours, raw_to_contourID[idx] , color, CV_FILLED, 8);

		if (m_raw_blobs[idx].hasHole)
		{
			color = Scalar(0, 0, 0);
			vector< vector<Point> > holes = m_raw_blobs[idx].hole_contours;

			for (size_t idh = 0; idh < holes.size(); idh++)
				drawContours(dst, holes, idh, color, CV_FILLED, 8);
		}

		char buff[255];
		if (hasID)
			sprintf(buff, "%d", m_raw_blobs[idx].groupID);
		else 
			sprintf(buff, "%d", (int) idx);

		string text = std::string(buff);
		putText(dst,text, m_raw_blobs[idx].center, 0, 0.6, Scalar(255,255,255), 2, 8, false);

	}
}

 void BEEBlob::DrawBlobs(Mat &dst, BEEBLOBS blobs, bool is_show_blobs)
 {
	 // iterate through all the top-level contours,
	 // draw each connected component with its own random color
	 if (blobs.empty()) 
	 {
		 putText(dst, "Empty", Point(dst.rows/2, dst.cols/2), 0, 0.6, Scalar(0,0,255), 2, 8, false);
		 return;
	 }

	 vector<vector<Point> > contours;	

	 for(size_t idx = 0; idx < blobs.size(); idx++ )
	 {
		 if (blobs[idx].groupID < 0 || blobs[idx].Overlapped) continue;
		 contours.push_back(blobs[idx].points);
	 }


	 // Pre-build the color code for groups of blobs
	 // use the same color for debugging
	 RNG rng( 0xFFFFFFFF );
	 map<int, Scalar> ID_Color;

	 for(size_t idx = 0; idx < blobs.size(); idx++ )
	 {
		 if (blobs[idx].groupID < 0 || blobs[idx].Overlapped) continue;

		 Scalar color( rng.uniform(0,255),  rng.uniform(0,255),  rng.uniform(0,255) );	 
		 ID_Color[blobs[idx].groupID] = color;
	  }

	 int count = 0;
	 for(size_t idx = 0; idx < blobs.size(); idx++ )
	 {
		 if (blobs[idx].groupID < 0 || blobs[idx].Overlapped) continue;

		 if (is_show_blobs)
		 {
			 //Scalar color( rng.uniform(0,255),  rng.uniform(0,255),  rng.uniform(0,255) );	 
			 Scalar color = ID_Color[blobs[idx].groupID];

			 drawContours( dst, contours, count++, color, CV_FILLED, 8);

			 if (blobs[idx].hasHole)
			 {
				 color = Scalar(0, 0, 0);
				 vector< vector<Point> > holes = blobs[idx].hole_contours;

				 for (size_t idh = 0; idh < holes.size(); idh++)
					 drawContours(dst, holes, idh, color, CV_FILLED, 8);
			 }
		 }

		 char buff[255];
		 sprintf(buff, "%d", blobs[idx].groupID);

		 string text = std::string(buff);
		 putText(dst, text, blobs[idx].center, 0, 0.6, Scalar(255,255,255), 2, 8, false);
	 }

 }


 
const Mat BEEBlob::WhiteOutImage(const Mat image, const BEEBLOBS blobs)
{
	if (blobs.empty()) return (image);
	
	Mat res_image = image;

	vector<vector<Point> > contours;	

	 for(size_t idx = 0; idx < blobs.size(); idx++ )
	 {
		 if (blobs[idx].groupID < 0 || blobs[idx].Overlapped) continue;
		 contours.push_back(blobs[idx].points);
	 }

	 Scalar color = Scalar(255, 255, 255);

	 int count = 0;
	 for(size_t idx = 0; idx < blobs.size(); idx++ )
	 {
		 if (blobs[idx].groupID < 0 || blobs[idx].Overlapped) continue;

		 drawContours(res_image, contours, count++, color, CV_FILLED, 8);

		 if (blobs[idx].hasHole)
		 {
			 color = Scalar(0, 0, 0);
			 vector< vector<Point> > holes = blobs[idx].hole_contours;

			 for (size_t idh = 0; idh < holes.size(); idh++)
				 drawContours(res_image, holes, idh, color, CV_FILLED, 8);
		 }

	 }

	return(res_image);
}
