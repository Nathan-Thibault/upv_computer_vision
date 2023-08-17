module es.upv.computervision {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires opencv;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome;

    opens es.upv.computervision to javafx.fxml;
    exports es.upv.computervision;
    exports es.upv.computervision.frameprocessing;
    opens es.upv.computervision.frameprocessing to javafx.fxml;
}