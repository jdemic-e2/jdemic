package jdemic.frontend;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import jdemic.Scenes.SceneManager.SceneManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ApplicationExtension.class)
class SceneManagerFxTest {
    private Stage stage;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        stage.setScene(new Scene(new Pane(), 800, 600));
        SceneManager.init(stage);
    }

    @Test
    void switchSceneCreatesFreshRootEachTime() {
        SceneManager.switchScene("MAIN_MENU");
        WaitForAsyncUtils.waitForFxEvents();
        Parent firstRoot = stage.getScene().getRoot();

        SceneManager.switchScene("MAIN_MENU");
        WaitForAsyncUtils.waitForFxEvents();
        Parent secondRoot = stage.getScene().getRoot();

        assertNotNull(firstRoot);
        assertNotNull(secondRoot);
        assertNotSame(firstRoot, secondRoot,
                "SceneManager must not reuse roots that may already belong to another Scene.");
    }
}
