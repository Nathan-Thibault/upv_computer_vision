package es.upv.computervision;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.opencv.core.Core;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AircraftDetection extends Application {

    public static final ExecutorService executorService = Executors.newFixedThreadPool(1);

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(AircraftDetection.class.getResource("browse-video-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), stage.getMaxWidth(), stage.getMaxHeight());
        stage.setTitle("Computer vision final project");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setOnCloseRequest(e -> {
            executorService.shutdown();
            try {
                if(!executorService.awaitTermination(1, TimeUnit.SECONDS)){
                    executorService.shutdownNow();
                }
            } catch (InterruptedException ex) {
                executorService.shutdownNow();
            }

            Platform.exit();
            System.exit(0);
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}