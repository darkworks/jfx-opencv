package com.accenture.nao.cv;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class MatchApp {
	public static void main(String[] s) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		System.out.println("\nRunning Template Matching");
		final int match_method = Imgproc.TM_CCOEFF_NORMED;
		final String outFile = "c:\\temp\\output\\out.png";
		final Scalar green = new Scalar(0, 255, 0);

		Mat ref = Imgcodecs.imread("c:\\temp\\inputs\\scene.jpg");
		Mat tpl = Imgcodecs.imread("c:\\temp\\inputs\\x_template.jpg");
		Mat gref = new Mat();
		Mat gtpl = new Mat();
		Imgproc.cvtColor(ref, gref, Imgproc.COLOR_BGR2GRAY);
		Imgproc.cvtColor(tpl, gtpl, Imgproc.COLOR_BGR2GRAY);

		Mat result = new Mat(ref.rows() - tpl.rows() + 1, ref.cols() - tpl.cols() + 1, CvType.CV_32FC1);

		Imgproc.matchTemplate(gref, gtpl, result, match_method);
		Core.normalize(result, result, 0.8, 1, Core.NORM_MINMAX, -1);
		Mat mask = new Mat(ref.rows() - tpl.rows() + 2, ref.cols() - tpl.cols() + 2, CvType.CV_32FC1);
		//		Mat mask = Mat.ones(result.rows()+2, result.cols()+2, CvType.CV_8UC1);
		//		Mat mask = Mat.zeros(result.rows()+2, result.cols()+2, CvType.CV_8UC1);

		int count = 0;
		double threshold = 0.9;
		while (true) {
			MinMaxLocResult minMaxLoc = Core.minMaxLoc(result);
			double minVal = minMaxLoc.minVal;
			double maxVal = minMaxLoc.maxVal;
			Point minLoc = minMaxLoc.minLoc;
			Point maxLoc = minMaxLoc.maxLoc;

			//			if(template_w + x  > general_mask.cols)
			//				template_w= general_mask.cols-x;
			//			if(template_h + y  > general_mask.rows)
			//				template_h= general_mask.rows-y;

			if (count > 300) {
				break;
			}
			if (maxVal >= threshold) {
				System.out.println("minMaxLoc.maxVal:" + maxVal);
				System.out.println("minMaxLoc.minVal:" + minVal);
				System.out.println("minMaxLoc.maxLoc:" + maxLoc.toString());
				System.out.println("minMaxLoc.minLoc:" + minLoc.toString());

				Imgproc.rectangle(ref, maxLoc, new Point(maxLoc.x + tpl.cols(),
					maxLoc.y + tpl.rows()), green, 3);
				Imgproc.floodFill(result, mask, maxLoc, new Scalar(255, 0, 0), new Rect(50, 50, 50, 50), new Scalar(0), new Scalar(0),
					Imgproc.FLOODFILL_MASK_ONLY);
			}
			//			else {
			//				System.out.println("No matches");
			//				break;
			//			}
			count++;
		}

		// hardcode drawing of the rectangle after finding it with template match...
		//		Imgproc.rectangle(ref, new Point(239, 403), new Point(239 + tpl.cols(),
		//			403 + tpl.rows()), new Scalar(0, 255, 0), 3);
		//		Imgproc.rectangle(ref, new Point(594, 264), new Point(594 + tpl.cols(),
		//			264 + tpl.rows()), new Scalar(0, 255, 0), 3);
		//		Imgproc.rectangle(ref, new Point(891, 355), new Point(891 + tpl.cols(),
		//			355 + tpl.rows()), new Scalar(0, 255, 0), 3);
		Imgcodecs.imwrite(outFile, ref);
	}
}

