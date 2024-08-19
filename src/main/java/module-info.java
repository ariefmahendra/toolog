module com.ariefmahendra.log {
    requires javafx.controls;
    requires javafx.fxml;
    requires jsch;
    requires static lombok;
    requires java.prefs;
    requires org.apache.commons.io;
    requires org.slf4j;

    opens com.ariefmahendra.log to javafx.fxml;
    opens com.ariefmahendra.log.controller to javafx.fxml;
    exports com.ariefmahendra.log;
}