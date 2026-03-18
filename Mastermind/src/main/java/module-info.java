module org.example.mastermind {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.mastermind to javafx.fxml;
    exports org.example.mastermind;
}