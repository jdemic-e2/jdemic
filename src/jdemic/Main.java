package jdemic;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jdemic.Scenes.MainMenuScene;

public class Main extends Application {
    @Override
    public void start(Stage stage) {

        MainMenuScene mainMenu = new MainMenuScene(stage);
        Scene scene = new Scene(mainMenu.getRoot(),1280,720);

        stage.setTitle("Cyber Crisis");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}