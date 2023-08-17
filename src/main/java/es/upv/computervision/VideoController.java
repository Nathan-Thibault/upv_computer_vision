package es.upv.computervision;

import es.upv.computervision.frameprocessing.EmptyProcessor;
import es.upv.computervision.frameprocessing.FrameProcessor;
import es.upv.computervision.frameprocessing.LKOpticalFlowProcessor;
import es.upv.computervision.frameprocessing.TestProcessor;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class VideoController implements Initializable {
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList("mp4", "avi");
    @FXML
    private ComboBox processCombo;
    @FXML
    private Pane imagePane;
    @FXML
    private ImageView imageView;
    @FXML
    private TextField pathText;
    @FXML
    private Button browseButton;
    @FXML
    private Button playButton;

    @FXML
    protected void onBrowseButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose a video");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Video Files",
                SUPPORTED_EXTENSIONS.stream().map((String s) -> "*." + s).toList() // change extensions to format required
        ));

        File selectedFile = fileChooser.showOpenDialog(browseButton.getScene().getWindow());
        if (selectedFile == null) return;
        pathText.setText(selectedFile.getAbsolutePath());
    }

    @FXML
    protected void onPlayButtonClick() {
        // Get path from TextField
        String path = pathText.getText().trim();

        // Verify that the path is not empty
        if (path.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "No path specified.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        // Verify that the path is valid
        File videoFile = new File(path);
        if (!videoFile.exists()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "No file found at the specified path.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        // Verify that the extension is supported
        String extension = path.substring(path.lastIndexOf(".") + 1);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            String alertMsg = "File extension at specified path is not supported.\nSupported extensions: ";
            alertMsg += String.join(",", SUPPORTED_EXTENSIONS);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, alertMsg, ButtonType.OK);
            alert.showAndWait();
            return;
        }

        // Verify that a process has been chosen and create a FrameProcessor accordingly
        FrameProcessor frameProcessor;
        switch (processCombo.getSelectionModel().getSelectedIndex()) {
            case -1 -> {
                (new Alert(Alert.AlertType.INFORMATION, "Please choose an image processing method.", ButtonType.OK)).showAndWait();
                return;
            }
            case 0 -> frameProcessor = new EmptyProcessor();
            case 1 -> frameProcessor = new LKOpticalFlowProcessor(15, 20, 3.0f, 40.0f);
            case 2 -> frameProcessor = new TestProcessor();
            default -> {
                (new Alert(Alert.AlertType.ERROR, "The selected process is not supported.", ButtonType.OK)).showAndWait();
                return;
            }
        }

        Runnable onEnd = () -> playButton.setDisable(false);

        // Play the video
        VideoPlayer player = new VideoPlayer(path, imageView, frameProcessor, onEnd);
        player.play();

        playButton.setDisable(true);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        imageView.fitWidthProperty().bind(imagePane.widthProperty());
        imageView.fitHeightProperty().bind(imagePane.heightProperty());
    }
}