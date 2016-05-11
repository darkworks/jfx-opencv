package com.accenture.nao.cv;

import org.opencv.core.Core;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class AcnNaoApp extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			final FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/accenture/nao/cv/AcnNaoController.fxml"));
			final BorderPane rootElement = (BorderPane) loader.load();
			final Scene scene = new Scene(rootElement, 1200, 900);
			final AcnNaoController controller = loader.getController();
			scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());

			rootElement.setStyle("-fx-background-color: whitesmoke;");
			primaryStage.setTitle("Accenture OpenCV Test App");
			primaryStage.setScene(scene);
			controller.init();
			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		launch(args);
	}

	@FXML
	public void exitApplication(ActionEvent event) {
		Platform.exit();
	}

	@Override
	public void stop() {
		System.out.println("Application is closing");
	}
}