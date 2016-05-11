package com.accenture.nao.cv;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class FloodFill {
	public static void main(String[] s) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		System.out.println("*** Running Flood fill");
		Mat img = Imgcodecs.imread("c:\\temp\\inputs\\scene.jpg",CvType.CV_8UC1);
		final String outFile1 = "c:\\temp\\output\\floodtest1.png";
		final String outFile2 = "c:\\temp\\output\\floodtest2.png";

		//Create simple input image
		final Point seed = new Point(4, 4);
//		final Mat img = new Mat(100, 100, CvType.CV_8UC1);
		Imgproc.circle(img, seed, 20, new Scalar(128), 3);
		Imgcodecs.imwrite(outFile1, img);
		//Create a mask from edges in the original image
		Mat mask = new Mat();
		Imgproc.Canny(img, mask, 100, 200);
		Core.copyMakeBorder(mask, mask, 1, 1, 1, 1, Core.BORDER_REPLICATE);
		//Fill mask with value 128
		int fillValue = 128;
		Imgproc.floodFill(img, mask, seed, new Scalar(255), new Rect(), new Scalar(0), new Scalar(0), 4);
		//		| cv::FLOODFILL_MASK_ONLY | (fillValue << 8)
		Imgcodecs.imwrite(outFile2, img);
	}
}
