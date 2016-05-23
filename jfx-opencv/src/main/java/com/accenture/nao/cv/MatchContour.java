
package com.accenture.nao.cv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class MatchContour {
	private final char[] boz = new char[9];
	private final int matchMethod = Imgproc.TM_CCOEFF_NORMED; //TM_SQDIFF, TM_CCOEFF_NORMED
	private final Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));
	private final Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(6, 6));

	public void process(final String file, final String outFile) {
		Mat source = Imgcodecs.imread(file);
		Mat gref = new Mat();
		Mat xx = new Mat();
		Mat hsvboard = new Mat();
		Mat mask = new Mat();

		transformBoard(source, hsvboard, mask);
		transformXO(source, gref);
		final List<Rect> fields = findBoardFields(source, mask);
		final List<List<Rect>> board = to3x3Board(source, fields);

		findBoardXO(source, gref, board);
		Imgcodecs.imwrite(outFile, source);
	}

	/**
	 * Do the necessary transformation to extract the board
	 * The board should in be this case be in.. RED
	 */
	private void transformBoard(final Mat source, final Mat destination, final Mat mask) {
		Imgproc.cvtColor(source, destination, Imgproc.COLOR_BGR2HSV, 1);

		final Scalar minValues = new Scalar(0, 122, 41);
		final Scalar maxValues = new Scalar(40, 255, 255);
		Core.inRange(destination, minValues, maxValues, mask);
		Imgproc.blur(mask, mask, new Size(16, 16));
		Imgproc.threshold(mask, mask, 1, 255, Imgproc.THRESH_BINARY_INV);
	}

	/**
	 * Transforms the image, so we can extract the X and O.
	 */
	private void transformXO(final Mat source, final Mat destination) {
		Imgproc.cvtColor(source, destination, Imgproc.COLOR_BGR2GRAY, 1);

		Imgproc.blur(destination, destination, new Size(4, 4));
		Imgproc.erode(destination, destination, erodeElement);
		Imgproc.dilate(destination, destination, dilateElement);
		Imgproc.threshold(destination, destination, 240, 255, Imgproc.THRESH_BINARY_INV);
	}

	/**
	 * We recognize the board, all the fields are then recognized as rectangles.
	 * The rectangles are ordered, based on the location of the field we can detect if an X or O is on the field
	 */
	private List<Rect> findBoardFields(final Mat source, final Mat mask) {
		List<MatOfPoint> contours = new ArrayList<>(10);
		Mat hierarchy = new Mat();
		Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

		int count = 0;
		final List<Rect> boardField = new ArrayList<Rect>(9);
		if (hierarchy.size().height > 0 && hierarchy.size().width > 0) {
			for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
				final Mat currentContour = contours.get(idx);
				final double contourArea = Imgproc.contourArea(currentContour, false);
				final MatOfPoint contour = contours.get(idx);
				final MatOfPoint2f contour2f = getMatOfPoint2f(contour);
				final double epsillon = Imgproc.arcLength(contour2f, true) * 0.06;
				MatOfPoint2f approxContour2f = new MatOfPoint2f();
				Imgproc.approxPolyDP(contour2f, approxContour2f, epsillon, true);

				// check if we have 4 vertices and the contour area has good size
				if (approxContour2f.toList().size() == 4 && contourArea <= 1407965) {
					//					Imgproc.putText(source, "j=" + count, contours.get(idx).toList().get(0), Core.FONT_HERSHEY_COMPLEX, 1, RgbColor.GREEN, 1);
					boardField.add(new Rect(approxContour2f.toList().get(0), approxContour2f.toList().get(2)));
					// all the points of the rectangle
					for (final Point p : approxContour2f.toList()) {
						//						Imgproc.drawMarker(source, p, RgbColor.GREEN);
					}
				}
				count++;
			}
		}

		return boardField;
	}

	private void transformPieces(final Mat source, final Mat destination) {
		Imgproc.cvtColor(source, destination, Imgproc.COLOR_BGR2GRAY, 1);
		Imgproc.blur(source, destination, new Size(4, 4));
		Imgproc.erode(source, destination, erodeElement);
		Imgproc.dilate(source, destination, dilateElement);
		Imgproc.threshold(source, destination, 240, 255, Imgproc.THRESH_BINARY_INV);

	}

	private List<List<Rect>> to3x3Board(final Mat source, final List<Rect> fields) {
		Collections.sort(fields, new XComparator());
		final List<List<Rect>> board = new ArrayList<>(3);
		board.add(new ArrayList<>(3));
		board.add(new ArrayList<>(3));
		board.add(new ArrayList<>(3));

		int x = 0;
		int colNr = -1;
		for (final Rect field : fields) {
			if (x % 3 == 0) {
				colNr++;
			}
			board.get(colNr).add(field);
			x++;
		}

		for (int col = 0; col < board.size(); col++) {
			Collections.sort(board.get(col), new YComparator());
			int rowNr = 0;
 			for (final Rect r : board.get(col)) {
				Imgproc.putText(source, col + "." + rowNr, r.br(), Core.FONT_HERSHEY_COMPLEX, 1, RgbColor.GREEN, 1);
				rowNr++;
			}
		}

		return board;
	}

	private class XComparator implements Comparator<Rect> {
		@Override
		public int compare(final Rect r1, final Rect r2) {
			return r1.x - r2.x;
		}
	}

	private class YComparator implements Comparator<Rect> {
		@Override
		public int compare(final Rect r1, final Rect r2) {
			return r1.y - r2.y;
		}
	}

	/**
	 * Find the X and O's of tictactoe, ignore the board
	 * Maps the X and O to a List and a nice prettyBoard for a System.out
	 */
	private void findBoardXO(final Mat source, final Mat gref, final List<List<Rect>> board) {
		List<MatOfPoint> scenecontours = new ArrayList<>(10);
		Mat scenehierarchy = new Mat();
		Imgproc.findContours(gref, scenecontours, scenehierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
		if (scenehierarchy.size().height > 0 && scenehierarchy.size().width > 0) {
			char[][] prettyBoard = new char[3][3];
			for (int idx = 0; idx >= 0; idx = (int) scenehierarchy.get(0, idx)[0]) {
				final Mat m = scenecontours.get(idx);
				final double contourArea = Imgproc.contourArea(m, false);

				final MatOfPoint cont = scenecontours.get(idx);
				final MatOfPoint2f curve2f = getMatOfPoint2f(cont);
				final double epsillon = Imgproc.arcLength(curve2f, true) * 0.06;
				MatOfPoint2f approxCurve2f = new MatOfPoint2f();
				Imgproc.approxPolyDP(curve2f, approxCurve2f, epsillon, true);

				Mat lines = new Mat(m.rows(), m.cols(), CvType.CV_8UC1);
				Mat m2 = new Mat(m.rows(), m.cols(), CvType.CV_8UC1);
				m2.convertTo(m2, CvType.CV_8UC1);

				final int i = drawO(contourArea, source, scenecontours, approxCurve2f, idx, board, prettyBoard)
					+ drawX(contourArea, source, scenecontours, approxCurve2f, idx, board, prettyBoard)
					+ drawBoard(contourArea, source, scenecontours, approxCurve2f, idx);
				if (i < 1) {
					System.out.println(approxCurve2f.toList().size() + " unkown..:" + idx);
				}
			}

			printPrettyBoard(prettyBoard);
		}
	}

	/**
	 * Prints out a pretty board, chars representing the board
	 */
	private void printPrettyBoard(final char[][] prettyBoard) {
		for (int i = 0; i < prettyBoard.length; i++) {
			for (int j = 0; j < prettyBoard.length; j++) {
				if (j == 2) {
					System.out.println(prettyBoard[2][0]);
				} else {
					System.out.print(prettyBoard[j][i]);
				}
			}
		}
	}

	/**
	 * We draw the X and also maps it to the prettyBoard char array
	 */
	private int drawX(final double contourArea, final Mat image, final List<MatOfPoint> sceneContours, final MatOfPoint2f approxCurve2f,
		final int idx, final List<List<Rect>> board, final char[][] prettyBoard) {
		if (approxCurve2f.toList().size() > 5 && contourArea <= 6000) {
			Imgproc.drawContours(image, sceneContours, idx, RgbColor.CYAN, 3);
			mapFoundElementToPrettyBoard(image, approxCurve2f, board, prettyBoard, "x=");
			return 1;
		}
		return 0;
	}

	/**
	 * We draw the O and also maps it to the prettyBoard char array
	 */
	private int drawO(final double contourArea, final Mat image, final List<MatOfPoint> sceneContours, final MatOfPoint2f approxCurve2f,
		final int idx, final List<List<Rect>> board, final char[][] prettyBoard) {
		if (approxCurve2f.toList().size() <= 5 && contourArea > 6000 && contourArea <= 100000) {
			Imgproc.drawContours(image, sceneContours, idx, RgbColor.YELLOW, 3);
			mapFoundElementToPrettyBoard(image, approxCurve2f, board, prettyBoard, "o=");
			return 1;
		}
		return 0;
	}

	private void mapFoundElementToPrettyBoard(final Mat image, final MatOfPoint2f approxCurve2f, final List<List<Rect>> board, final char[][] prettyBoard,
		final String text) {
		for (final Point p : approxCurve2f.toList()) {
			int j = 0;
			for (final List<Rect> cols : board) {
				int i = 0;
				for (final Rect r : cols) {
					if (r.contains(p)) {
						prettyBoard[j][i] = text.charAt(0);
						// All the vertices points can have a text
						// Imgproc.putText(image, text + j + " " + i, p, Core.FONT_HERSHEY_COMPLEX, 1, RgbColor.NAVY, 2);
						// For our case we only care for the first one we find.
						return;
					}
					i++;
				}
				j++;
			}
			// you can use drawMarker to see the vertices
			// Imgproc.drawMarker(image, p, RgbColor.BLACK);
		}
	}

	/**
	 * Draws the outline contour of the board, we just ignore this big element in the image
	 */
	private int drawBoard(final double contourArea, final Mat image, final List<MatOfPoint> sceneContours,
		final MatOfPoint2f approxCurve2f,
		final int idx) {
		// the criteria to recognize the board
		if (contourArea > 100000) {
			Imgproc.drawContours(image, sceneContours, idx, RgbColor.OLIVE, 3);
			return 1;
		}
		return 0;
	}

	/**
	 * Converts MatOfPoint to MatOfPoint2f
	 */
	private MatOfPoint2f getMatOfPoint2f(final MatOfPoint matOfPoint) {
		return new MatOfPoint2f(matOfPoint.toArray());
	}
}