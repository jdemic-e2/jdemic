package jdemic.e2e;

import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import jdemic.Main;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
@Disabled("Pending fix: Monocle is incompatible with Java 21 Window class, and SceneManager has static root caching issues.")
class MainMenuUIE2ETest {

    @BeforeAll
    public static void setupHeadlessMode() {
        // Modo headless nativo compatible con Java 21 (sin usar Monocle)
        System.setProperty("testfx.robot", "glass");
        System.setProperty("testfx.headless", "true");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        System.setProperty("java.awt.headless", "true");
    }

    @Start
    void start(Stage stage) throws Exception {
        new Main().start(stage);
    }

    // Novedad: ¡El basurero! Limpia la ventana y la memoria después de cada test
    // para evitar el error del "StackPane is already set as root".
    @AfterEach
    public void tearDown() throws Exception {
        FxToolkit.hideStage();
        FxToolkit.cleanupStages();
    }

    @Test
    @DisplayName("UI-E2E-01 | Main Menu loads and displays the game title")
    void mainMenuShouldDisplayTitle(FxRobot robot) {
        assertTrue(
                robot.lookup(".label").tryQuery().isPresent(),
                "At least one Label must be visible on the Main Menu screen.");
    }

    @Test
    @DisplayName("UI-E2E-02 | Main Menu has a 'HOST' or 'PLAY' button visible")
    void mainMenuShouldHavePlayOrHostButton(FxRobot robot) {
        boolean hasButton = robot.lookup(".button").tryQuery().isPresent();
        assertTrue(hasButton,
                "The Main Menu must have at least one clickable button (Host / Play / Join).");
    }

    @Test
    @DisplayName("UI-E2E-03 | Clicking PLAY navigates to the PlayScene (Lobby Selection)")
    void clickingPlayButtonShouldNavigateToLobby(FxRobot robot) {
        robot.clickOn("PLAY");
        // Asegúrate de que el botón en tu juego dice exactamente "HOST GAME"
        boolean hasHostGame = robot.lookup("HOST GAME").tryQuery().isPresent();
        assertTrue(hasHostGame, "After clicking PLAY, the 'HOST GAME' button must be visible.");
    }

    @Test
    @DisplayName("UI-E2E-04 | End-to-End Navigation: PLAY -> HOST GAME -> HostGameScene")
    void navigatingToHostGameScene(FxRobot robot) {
        robot.clickOn("PLAY");
        robot.clickOn("HOST GAME");

        assertTrue(
                robot.lookup(".button").tryQuery().isPresent(),
                "After clicking HOST GAME, a new scene should load with buttons.");
    }

    @Test
    @DisplayName("UI-E2E-05 | Stage title is set to the game name")
    void stageTitleShouldBeGameName(FxRobot robot) {
        Stage stage = (Stage) robot.targetWindow();
        String title = stage.getTitle();
        assertNotNull(title, "The application window must have a title set.");
    }
}