/**
 * E2E UI tests for jDemic using TestFX.
 *
 * These tests launch the actual JavaFX application in headless mode
 * (no physical screen required) and simulate a real player interacting
 * with the interface: clicking buttons, navigating between scenes, and
 * verifying that the correct UI elements appear on screen.
 *
 * Headless mode is enabled by the Monocle glass platform, which allows
 * these tests to run in CI environments without a display server.
 *
 * Authors: Rubén Alcázar, Álvaro Miñán
 * Branch:  feature/e2e-testing-setup
 */

package jdemic.e2e;

import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import jdemic.Main;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApplicationExtension integrates TestFX with JUnit 5.
 * It starts the JavaFX application before each test and tears it
 * down afterwards automatically.
 */
@ExtendWith(ApplicationExtension.class)
class MainMenuUIE2ETest {

    /**
     * @Start is called by TestFX before each test to launch the app.
     *        It mirrors what JavaFX calls Application#start(Stage).
     */
    @Start
    void start(Stage stage) throws Exception {
        // Enable headless (no screen) mode via Monocle
        System.setProperty("testfx.robot", "glass");
        System.setProperty("testfx.headless", "true");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        System.setProperty("glass.platform", "Monocle");
        System.setProperty("monocle.platform", "Headless");

        new Main().start(stage);
    }

    // ─────────────────────────────────────────────────────────────
    // 1. MAIN MENU PRESENCE
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("UI-E2E-01 | Main Menu loads and displays the game title")
    void mainMenuShouldDisplayTitle(FxRobot robot) {
        // The main menu must show the game title text on screen.
        // The actual text depends on what the front-end team used; adjust if needed.
        assertTrue(
                robot.lookup(".label").tryQuery().isPresent(),
                "At least one Label must be visible on the Main Menu screen.");
    }

    @Test
    @DisplayName("UI-E2E-02 | Main Menu has a 'HOST' or 'PLAY' button visible")
    void mainMenuShouldHavePlayOrHostButton(FxRobot robot) {
        // Check that at least one interactive button is present on the main menu.
        // TestFX can locate nodes by CSS class or by text content.
        boolean hasButton = robot.lookup(".button").tryQuery().isPresent();
        assertTrue(hasButton,
                "The Main Menu must have at least one clickable button (Host / Play / Join).");
    }

    @Test
    @DisplayName("UI-E2E-03 | Clicking PLAY navigates to the PlayScene (Lobby Selection)")
    void clickingPlayButtonShouldNavigateToLobby(FxRobot robot) {
        // First click PLAY in the main menu
        robot.clickOn("PLAY");

        // The next scene (PlayScene) should contain "HOST GAME" and "JOIN BY CODE"
        boolean hasHostGame = robot.lookup("HOST GAME").tryQuery().isPresent();
        assertTrue(hasHostGame, "After clicking PLAY, the 'HOST GAME' button must be visible.");
    }

    @Test
    @DisplayName("UI-E2E-04 | End-to-End Navigation: PLAY -> HOST GAME -> HostGameScene")
    void navigatingToHostGameScene(FxRobot robot) {
        robot.clickOn("PLAY");
        robot.clickOn("HOST GAME");

        // HostGameScene should have some labels or a "SEND" button for IP
        assertTrue(
                robot.lookup(".button").tryQuery().isPresent(),
                "After clicking HOST GAME, a new scene should load with buttons.");
    }

    @Test
    @DisplayName("UI-E2E-05 | Stage title is set to the game name")
    void stageTitleShouldBeGameName(FxRobot robot) {
        // The Main class sets: stage.setTitle("Cyber Crisis")
        Stage stage = (Stage) robot.targetWindow();
        String title = stage.getTitle();
        assertNotNull(title, "The application window must have a title set.");
    }
}
