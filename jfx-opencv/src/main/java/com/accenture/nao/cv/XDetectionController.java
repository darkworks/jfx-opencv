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

public class XDetectionController {
	@FXML
	private Button cameraButton;
	@FXML
	private ImageView originalFrame;
	@FXML
	private ImageView maskImage;
	@FXML
	private ImageView morphImage;
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
	private Label hsvCurrentValues;
	@FXML
	private Slider blurSize;
	@FXML
	private Slider erodeSize;
	@FXML
	private Slider dilateSize;
	@FXML
	private Slider originalImageSize;
	@FXML
	private Slider maskImageSize;
	@FXML
	private Slider morphImageSize;

	private ScheduledExecutorService timer;
	private VideoCapture capture = new VideoCapture();
	private boolean cameraActive;
	private ObjectProperty<String> hsvValuesProp;
	Mat template = Imgcodecs.imread("C:\\Temp\\inputs\\x_template.jpg", 0);

	@FXML
	private void startCamera() {
		hsvValuesProp = new SimpleObjectProperty<>();
		hsvCurrentValues.textProperty().bind(hsvValuesProp);

		imageViewProperties(originalFrame, (int) originalImageSize.getValue());
		imageViewProperties(maskImage, (int) maskImageSize.getValue());
		imageViewProperties(morphImage, (int) morphImageSize.getValue());

		if (!cameraActive) {
			capture.open(0);
			if (capture.isOpened()) {
				cameraActive = true;
				Runnable frameGrabber = new Runnable() {
					@Override
					public void run() {
						Image imageToShow = grabFrame();
						originalFrame.setImage(imageToShow);
					}
				};

				timer = Executors.newSingleThreadScheduledExecutor();
				timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

				cameraButton.setText("Stop Camera");
			} else {
				System.err.println("Failed to open the camera connection...");
			}
		} else {
			stopCamera();
		}
	}

	private void stopCamera() {
		cameraActive = false;
		cameraButton.setText("Start Camera");

		try {
			timer.shutdown();
			timer.awaitTermination(33, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
		}
		capture.release();
	}

	private Image grabFrame() {
		Image imageToShow = null;
		Mat frame = new Mat();

		if (capture.isOpened()) {
			try {
				capture.read(frame);
				if (!frame.empty()) {
					imageViewProperties(originalFrame, (int) originalImageSize.getValue());
					imageViewProperties(maskImage, (int) maskImageSize.getValue());
					imageViewProperties(morphImage, (int) morphImageSize.getValue());

					Mat blurredImage = new Mat();
					Mat hsvImage = new Mat();
					Mat mask = new Mat();
					Mat morphOutput = new Mat();
					Imgproc.blur(frame, blurredImage, new Size(blurSize.getValue(), blurSize.getValue()));
					Imgproc.cvtColor(blurredImage, hsvImage, Imgproc.COLOR_BGR2HSV);

					Scalar minValues = new Scalar(hueStart.getValue(), saturationStart.getValue(),
						valueStart.getValue());
					Scalar maxValues = new Scalar(hueStop.getValue(), saturationStop.getValue(),
						valueStop.getValue());

					// threshold HSV image to select tennis balls
					Core.inRange(hsvImage, minValues, maxValues, mask);

					// show the partial output
					onFXThread(maskImage.imageProperty(), mat2Image(mask));

					// morphological operators
					// dilate with large element, erode with small ones
					Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(dilateSize.getValue(), dilateSize.getValue()));
					Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(erodeSize.getValue(), erodeSize.getValue()));
					Imgproc.erode(mask, morphOutput, erodeElement);
					Imgproc.dilate(mask, morphOutput, dilateElement);

					// find the tennis ball(s) contours and show them
//					frame = findAndDrawBalls(morphOutput, frame, new Scalar(250, 0, 0));

					String valuesToPrint = "Blur: " + blurSize.getValue() + "\tHue range: " + minValues.val[0] + "-" + maxValues.val[0]
						+ "\tSaturation range: " + minValues.val[1] + "-" + maxValues.val[1] + "\tValue range: "
						+ minValues.val[2] + "-" + maxValues.val[2];

					Mat newLol = new Mat();
					Imgproc.matchTemplate(mask, template, newLol, Imgproc.TM_CCOEFF_NORMED);

					onFXThread(hsvValuesProp, valuesToPrint);
					onFXThread(morphImage.imageProperty(), mat2Image(newLol));

					imageToShow = mat2Image(frame);

					blurredImage.release();
					hsvImage.release();
					mask.release();
					morphOutput.release();
					dilateElement.release();
					erodeElement.release();
					frame.release();
				}

			} catch (Exception e) {
				System.err.print("ERROR");
				e.printStackTrace();
			}
		}

		return imageToShow;
	}


	private Mat findTemplate(Mat maskedImage, Mat frame, Scalar color) {
		// init
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();

		// find contours
		Imgproc.findContours(maskedImage, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

		// if any contour exist...
		if (hierarchy.total() > 7 && hierarchy.total() < 10) {
			// for each contour, display it
			for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
				Imgproc.drawContours(frame, contours, idx, color);
			}
		}
		//		if (hierarchy.total() >= 10) {
		//			// for each contour, display it
		//			for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
		//				Imgproc.drawContours(frame, contours, idx, color);
		//			}
		//		}

		return frame;
	}

	private Mat findAndDrawBalls(Mat maskedImage, Mat frame, Scalar color) {
		// init
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();

		// find contours
		Imgproc.findContours(maskedImage, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

		// if any contour exist...
		if (hierarchy.total() > 7 && hierarchy.total() < 10) {
			// for each contour, display it
			for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
				Imgproc.drawContours(frame, contours, idx, color);
			}
		}
//		if (hierarchy.total() >= 10) {
//			// for each contour, display it
//			for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
//				Imgproc.drawContours(frame, contours, idx, color);
//			}
//		}

		return frame;
	}

	private void imageViewProperties(ImageView image, int dimension) {
		image.setFitWidth(dimension);
		image.setPreserveRatio(true);
	}

	private Image mat2Image(Mat frame) {
		MatOfByte buffer = new MatOfByte();
		Imgcodecs.imencode(".png", frame, buffer);
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