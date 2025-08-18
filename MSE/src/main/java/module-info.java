module com.zerompurdy.mse {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires javafx.graphics;


    opens com.zerompurdy.mse to javafx.fxml;
    opens com.zerompurdy.mse.controllers to javafx.fxml;
    exports com.zerompurdy.mse;
}