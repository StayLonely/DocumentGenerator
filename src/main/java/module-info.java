module ru.staylonely.course.documentsgenerator {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires org.json;
    requires org.apache.poi.ooxml;


    opens ru.staylonely.course.documentsgenerator to javafx.fxml;
    exports ru.staylonely.course.documentsgenerator;
}