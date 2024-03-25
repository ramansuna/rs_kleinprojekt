module RS_KleinProjekt.rs_kleinprojekt {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;

    opens RS_KleinProjekt.rs_kleinprojekt to javafx.fxml;
    exports RS_KleinProjekt.rs_kleinprojekt;
}
