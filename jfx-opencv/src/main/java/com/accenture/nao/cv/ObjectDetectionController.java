package com.accenture.nao.cv;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * The controller associated with the only view of our application. The application logic is implemented here. It handles the button for
 * starting/stopping the camera, the acquired video stream, the relative controls and the image segmentation process.
 *
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @version 1.5 (2015-11-26)
 * @since 1.0 (2015-01-13)
 */
public class ObjectDetectionController {
	// FXML camera button
	@FXML
	private Button cameraButton;
	// the FXML area for showing the current frame
	@FXML
	private ImageView originalFrame;
	// the FXML area for showing the mask
	@FXML
	private ImageView maskImage;
	// the FXML area for showing the output of the morphological operations
	@FXML
	private ImageView morphImage;
	// FXML slider for setting HSV ranges
	@FXML
	private ImageView maskImage2;
	@FXML
	private ImageView morphImage2;

	@FXML
	private Slider hueStart;
	@FXML
	private Slider hueStop;
	@FXML
	private Slider saturationStart;
	@FXML
	private Slider saturationStop;
	@FXML
	private Slider valueStart;
	@FXML
	private Slider valueStop;
	@FXML
	private Slider hueStart2;
	@FXML
	private Slider hueStop2;
	@FXML
	private Slider saturationStart2;
	@FXML
	private Slider saturationStop2;
	@FXML
	private Slider valueStart2;
	@FXML
	private Slider valueStop2;
	// FXML label to show the current values set with the sliders
	@FXML
	private Label hsvCurrentValues;
	@FXML
	private Label hsvCurrentValues2;

	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that performs the video capture
	private VideoCapture capture = new VideoCapture();
	// a flag to change the button behavior
	private boolean cameraActive;

	// property for object binding
	private ObjectProperty<String> hsvValuesProp;
	private ObjectProperty<String> hsvValuesProp2;

	/**
	 * The action triggered by pushing the button on the GUI
	 */
	@FXML
	private void startCamera() {
		// bind a text property with the string containing the current range of
		// HSV values for object detection
		hsvValuesProp = new SimpleObjectProperty<>();
		hsvCurrentValues.textProperty().bind(hsvValuesProp);

		hsvValuesProp2 = new SimpleObjectProperty<>();
		hsvCurrentValues2.textProperty().bind(hsvValuesProp2);

		// set a fixed width for all the image to show and preserve image ratio
		imageViewProperties(originalFrame, 600);
		imageViewProperties(maskImage, 200);
		imageViewProperties(morphImage, 200);
		imageViewProperties(maskImage2, 200);
		imageViewProperties(morphImage2, 200);

		if (!cameraActive) {
			// start the video capture
			capture.open(0);

			// is the video stream available?
			if (capture.isOpened()) {
				cameraActive = true;

				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameGrabber = new Runnable() {

					@Override
					public void run() {
						Image imageToShow = grabFrame();
						originalFrame.setImage(imageToShow);
					}
				};

				timer = Executors.newSingleThreadScheduledExecutor();
				timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

				// update the button content
				cameraButton.setText("Stop Camera");
			} else {
				// log the error
				System.err.println("Failed to open the camera connection...");
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
		try {
			timer.shutdown();
			timer.awaitTermination(33, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// log the exception
			System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
		}

		// release the camera
		capture.release();
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
					// init
					Mat blurredImage = new Mat();
					Mat hsvImage = new Mat();
					Mat mask = new Mat();
					Mat morphOutput = new Mat();

					Mat blurredImage2 = new Mat();
					Mat hsvImage2 = new Mat();
					Mat mask2 = new Mat();
					Mat morphOutput2 = new Mat();

					// remove some noise
					Imgproc.blur(frame, blurredImage, new Size(7, 7));
					Imgproc.blur(frame, blurredImage2, new Size(7, 7));

					// convert the frame to HSV
					Imgproc.cvtColor(blurredImage, hsvImage, Imgproc.COLOR_BGR2HSV);
					Imgproc.cvtColor(blurredImage2, hsvImage2, Imgproc.COLOR_BGR2HSV);

					// get thresholding values from the UI
					// remember: H ranges 0-180, S and V range 0-255
					Scalar minValues = new Scalar(hueStart.getValue(), saturationStart.getValue(),
						valueStart.getValue());
					Scalar maxValues = new Scalar(hueStop.getValue(), saturationStop.getValue(),
						valueStop.getValue());

					Scalar minValues2 = new Scalar(hueStart2.getValue(), saturationStart2.getValue(),
						valueStart2.getValue());
					Scalar maxValues2 = new Scalar(hueStop2.getValue(), saturationStop2.getValue(),
						valueStop2.getValue());

					// show the current selected HSV range
					String valuesToPrint = "Hue range: " + minValues.val[0] + "-" + maxValues.val[0]
						+ "\tSaturation range: " + minValues.val[1] + "-" + maxValues.val[1] + "\tValue range: "
						+ minValues.val[2] + "-" + maxValues.val[2];
					String valuesToPrint2 = "Hue range: " + minValues2.val[0] + "-" + maxValues2.val[0]
						+ "\tSaturation range: " + minValues2.val[1] + "-" + maxValues2.val[1] + "\tValue range: "
						+ minValues2.val[2] + "-" + maxValues2.val[2];

					onFXThread(hsvValuesProp, valuesToPrint);
					onFXThread(hsvValuesProp2, valuesToPrint2);

					// threshold HSV image to select tennis balls
					Core.inRange(hsvImage, minValues, maxValues, mask);
					Core.inRange(hsvImage2, minValues2, maxValues2, mask2);

					// show the partial output
					onFXThread(maskImage.imageProperty(), mat2Image(mask));
					onFXThread(maskImage2.imageProperty(), mat2Image(mask2));

					// morphological operators
					// dilate with large element, erode with small ones
					Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
					Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));
					Imgproc.erode(mask, morphOutput, erodeElement);
					Imgproc.erode(mask, morphOutput, erodeElement);
					Imgproc.dilate(mask, morphOutput, dilateElement);
					Imgproc.dilate(mask, morphOutput, dilateElement);

					Mat dilateElement2 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
					Mat erodeElement2 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));
					Imgproc.erode(mask, morphOutput, erodeElement2);
					Imgproc.erode(mask, morphOutput, erodeElement2);
					Imgproc.dilate(mask2, morphOutput2, dilateElement2);
					Imgproc.dilate(mask2, morphOutput2, dilateElement2);

					// show the partial output
					onFXThread(morphImage.imageProperty(), mat2Image(morphOutput));
					onFXThread(morphImage2.imageProperty(), mat2Image(morphOutput2));

					// find the tennis ball(s) contours and show them
					frame = findAndDrawBalls(morphOutput, frame, new Scalar(250, 0, 0));
					frame = findAndDrawBalls(morphOutput2, frame, new Scalar(0, 255, 0));

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

	/**
	 * Given a binary image containing one or more closed surfaces, use it as a mask to find and highlight the objects contours
	 *
	 * @param maskedImage the binary image to be used as a mask
	 * @param frame       the original {@link Mat} image to be used for drawing the objects contours
	 * @return the {@link Mat} image with the objects contours framed
	 */
	private Mat findAndDrawBalls(Mat maskedImage, Mat frame, Scalar color) {
		// init
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();

		// find contours
		Imgproc.findContours(maskedImage, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

		// if any contour exist...
		if (hierarchy.total() > 7 && hierarchy.total() < 10 ) {
			// for each contour, display it
			for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
				Imgproc.drawContours(frame, contours, idx, color);
			}
		}
		if (hierarchy.total() >= 10) {
			// for each contour, display it
			for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
				Imgproc.drawContours(frame, contours, idx, color);
			}
		}

		return frame;
	}

	/**
	 * Set typical {@link ImageView} properties: a fixed width and the information to preserve the original image ration
	 *
	 * @param image     the {@link ImageView} to use
	 * @param dimension the width of the image to set
	 */
	private void imageViewProperties(ImageView image, int dimension) {
		// set a fixed width for the given ImageView
		image.setFitWidth(dimension);
		// preserve the image ratio
		image.setPreserveRatio(true);
	}

	/**
	 * Convert a {@link Mat} object (OpenCV) in the corresponding {@link Image} for JavaFX
	 *
	 * @param frame the {@link Mat} representing the current frame
	 * @return the {@link Image} to show
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

	/**
	 * Generic method for putting element running on a non-JavaFX thread on the JavaFX thread, to properly update the UI
	 *
	 * @param property a {@link ObjectProperty}
	 * @param value    the value to set for the given {@link ObjectProperty}
	 */
	private <T> void onFXThread(final ObjectProperty<T> property, final T value) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				property.set(value);
			}
		});
	}

}