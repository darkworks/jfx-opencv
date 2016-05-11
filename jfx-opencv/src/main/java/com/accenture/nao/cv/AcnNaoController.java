package com.accenture.nao.cv;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseDragEvent;

public class AcnNaoController {
	@FXML
	private Button cameraButton;
	@FXML
	private ImageView originalFrame;
	@FXML
	private ImageView calibratedFrame;

	@FXML
	private Label mouseCurrentValues;

	// a timer for acquiring the video stream
	private Timer timer;
	// the OpenCV object that performs the video capture
	private VideoCapture capture;
	// a flag to change the button behavior
	private boolean cameraActive;
	// the saved chessboard image
	private Mat savedImage;

	private Mat undistored;
	// the calibrated camera frame
	private Image undistoredImage, CamStream;
	// various variables needed for the calibration
	private List<Mat> imagePoints;
	private List<Mat> objectPoints;
	private MatOfPoint3f obj;
	private MatOfPoint2f imageCorners;
	private int successes;
	private Mat intrinsic;
	private Mat distCoeffs;
	private boolean isCalibrated;

	private ObjectProperty<String> hsvValuesProp;

	private Point mouseA = new Point(379, 280);
	private Point mouseB = new Point(379, 281);
	private boolean drawRectangle = false;

	/**
	 * Init all the (global) variables needed in the controller
	 */
	protected void init() {
		capture = new VideoCapture();
		cameraActive = false;
		obj = new MatOfPoint3f();
		imageCorners = new MatOfPoint2f();
		savedImage = new Mat();
		undistored = new Mat();
		undistoredImage = null;
		imagePoints = new ArrayList<>();
		objectPoints = new ArrayList<>();
		intrinsic = new Mat(3, 3, CvType.CV_32FC1);
		distCoeffs = new Mat();
		successes = 0;
		isCalibrated = false;
	}

	@FXML
	protected void updateCalibration() {
		if (!cameraButton.isDisabled()) {
			Image imageToShow = null;
			Mat frame = new Mat();
		}
	}

	/**
	 * The action triggered by pushing the button on the GUI
	 */
	@FXML
	protected void startCamera() {
		if (!cameraActive) {
			capture.open(0);
			if (capture.isOpened()) {
				cameraActive = true;
				// grab a frame every 33 ms (30 frames/sec)
				final TimerTask frameGrabber = new TimerTask() {
					@Override
					public void run() {
						CamStream = grabFrame();
						// show the original frames
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								originalFrame.setImage(CamStream);
								// set fixed width
								originalFrame.setFitWidth(380);
								//								originalFrame.setFitWidth(CamStream.getWidth());
								//								originalFrame.setFitHeight(CamStream.getHeight());
								// preserve image ratio
								originalFrame.setPreserveRatio(true);
								// show the original frames
								calibratedFrame.setImage(undistoredImage);
								// set fixed width
								calibratedFrame.setFitWidth(380);
								//								calibratedFrame.setFitWidth(CamStream.getWidth());
								//								calibratedFrame.setFitHeight(CamStream.getHeight());
								// preserve image ratio
								calibratedFrame.setPreserveRatio(true);

								originalFrame.setOnMouseExited(e -> reInitMouseAB());
								originalFrame
									.setOnDragDetected(mouseEvent -> originalFrame.startFullDrag());
								originalFrame.setOnMouseDragEntered(
									e -> initialDrag(e));
								originalFrame.setOnMouseDragReleased(
									e -> finishDrag(e));

							}
						});

					}
				};
				timer = new Timer();
				timer.schedule(frameGrabber, 0, 33);
				cameraButton.setText("Stop Camera");
			} else {
				// log the error
				System.err.println("Impossible to open the camera connection...");
			}
		} else {
			stopCamera();
		}
	}

	private void stopCamera() {
		// the camera is not active at this point
		cameraActive = false;
		// update again the button content
		cameraButton.setText("Start Camera");
		// stop the timer
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		// release the camera
		capture.release();
		// clean the image areas
		originalFrame.setImage(null);
		calibratedFrame.setImage(null);

	}

	private void initialDrag(final MouseDragEvent e) {
		drawRectangle = false;
		mouseA = new Point(e.getX(), e.getY());
	}

	private void finishDrag(final MouseDragEvent e) {
		drawRectangle = true;
		mouseB = new Point(e.getX(), e.getY());
	}

	private void reInitMouseAB() {
		drawRectangle = false;
		mouseA = new Point(0, 0);
		mouseB = new Point(0, 0);
	}

	/**
	 * Get a frame from the opened video stream (if any)
	 *
	 * @return the {@link Image} to show
	 */
	private Image grabFrame() {
		// init everything
		Image imageToShow = null;
		Mat frame = new Mat();

		// check if the capture is open
		if (capture.isOpened()) {
			try {
				// read the current frame
				capture.read(frame);
				// if the frame is not empty, process it
				if (!frame.empty()) {
					drawOn(frame);
					undistoredImage = mat2Image(undistored);
					// convert the Mat object (OpenCV) to Image (JavaFX)
					imageToShow = mat2Image(frame);
				}
			} catch (Exception e) {
				// log the (full) error
				System.err.print("ERROR");
				e.printStackTrace();
			}
		}

		return imageToShow;
	}

	private void drawOn(final Mat frame) {
		Mat grayImage = new Mat();

		if (drawRectangle) {
			Imgproc.rectangle(frame, mouseA, mouseB, new Scalar(0, 255, 0), 5);
			System.out.println("mouseA: " + mouseA + " mouseB: " + mouseB);
		}
		Imgproc.cvtColor(frame, grayImage, Imgproc.COLOR_BGR2GRAY);

		Mat binaryImage = grayImage;
		Imgproc.threshold(grayImage, binaryImage, 0, 255, Imgproc.THRESH_OTSU);

		Mat Kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
		Imgproc.morphologyEx(binaryImage, binaryImage, Imgproc.MORPH_CLOSE, Kernel);

		List<MatOfPoint> contours = new ArrayList<>();
		//		Imgproc.findContours(binaryImage, contours, new Mat(), Imgproc.CHAIN_APPROX_SIMPLE);
		Mat hierarchy = new Mat();
		Imgproc.findContours(binaryImage, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

		for (int i = 0; i < contours.size(); i++) {
			if (Imgproc.contourArea(contours.get(i)) > 50) {
				MatOfPoint2f curve2f = getMatOfPoint2f(contours.get(i));
				double epsillon = Imgproc.arcLength(curve2f, true) * 0.02;
				MatOfPoint2f approxCurve2f = new MatOfPoint2f();
				Imgproc.approxPolyDP(curve2f, approxCurve2f, epsillon, true);

				Scalar color = new Scalar(0, 255, 0);
				int thickness = 3;

				if ((approxCurve2f.toList().size() > 7 && approxCurve2f.toList().size() < 10)) {
					System.out.println("this is a O");
					Imgproc.drawContours(frame, contours, i, color, thickness);
					Point p = Imgproc.boundingRect(contours.get(i)).tl();
					p.y -= 10;
					Imgproc.putText(frame, "O.", p, Core.FONT_HERSHEY_COMPLEX, 1, color, thickness);
				} else if (approxCurve2f.toList().size() >= 10) {
					System.out.println("this is a X");
					Imgproc.drawContours(frame, contours, i, color, thickness);
					Point p = Imgproc.boundingRect(contours.get(i)).tl();
					p.y -= 10;
					Imgproc.putText(frame, "X.", p, Core.FONT_HERSHEY_COMPLEX, 1, color, thickness);
				}
				//				Rect rect = Imgproc.boundingRect(contours.get(i));
				//				if (rect.height > 28) {
				//					Imgproc.rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255));
				//				}
			}
		}

		undistored = binaryImage.clone();
	}

	private MatOfPoint2f getMatOfPoint2f(final MatOfPoint matOfPoint) {
		return new MatOfPoint2f(matOfPoint.toArray());
	}

	//	String recognizeCountour(Mat contour)
	//	{
	//		double ContourArea = Imgproc.contourArea(contour);
	//
	//		Contour const Poly = get_approx_poly(contour);
	//
	//		ContourType type = ContourType::unknown;
	//		if ((Poly.size() > 7 && Poly.size() < 10) && ContourArea > 1000) {
	//			type = ContourType::o_type;
	//		} else if (Poly.size() >= 10 && ContourArea < 10000) {
	//			type = ContourType::x_type;
	//		}
	//
	//		return type;
	//	}

	/**
	 * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
	 */
	private Image mat2Image(Mat frame) {
		// create a temporary buffer
		MatOfByte buffer = new MatOfByte();
		// encode the frame in the buffer, according to the PNG format
		Imgcodecs.imencode(".png", frame, buffer);
		// build and return an Image created from the image encoded in the
		// buffer
		return new Image(new ByteArrayInputStream(buffer.toArray()));
	}

	private <T> void onFXThread(final ObjectProperty<T> property, final T value) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				property.set(value);
			}
		});
	}

}
