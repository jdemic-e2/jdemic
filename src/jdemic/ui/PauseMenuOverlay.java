package jdemic.ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import jdemic.Scenes.Settings.AudioManager;
import jdemic.Scenes.Settings.SettingsManager;

/**
 * In-game pause overlay. Keeps the active MapTestScene mounted so game state is preserved.
 */
public class PauseMenuOverlay {

    private static final String FONT_HKMODULAR = "hkmodular";
    private static final String CYAN = "#00b5d4";
    private static final String BRIGHT_CYAN = "#00d4ff";
    private static final String RED = "#ff274c";
    private static final String BLACK = "black";
    private static final String WHITE = "#ffffff";
    private static final String YELLOW = "#ffff00";

    public enum Panel {
        MAIN,
        SETTINGS,
        RULES
    }

    private final StackPane parent;
    private final Runnable onDisconnectConfirmed;
    private final StackPane overlayRoot;
    private final StackPane contentHolder;
    private final VBox mainMenuContent;
    private Panel activePanel = Panel.MAIN;
    private StackPane activeConfirmationRoot;

    private final Runnable onOverlayHidden;

    public PauseMenuOverlay(StackPane parent, Runnable onDisconnectConfirmed) {
        this(parent, onDisconnectConfirmed, null);
    }

    public PauseMenuOverlay(StackPane parent, Runnable onDisconnectConfirmed, Runnable onOverlayHidden) {
        this.parent = parent;
        this.onDisconnectConfirmed = onDisconnectConfirmed;
        this.onOverlayHidden = onOverlayHidden;

        overlayRoot = new StackPane();
        overlayRoot.setPickOnBounds(false);
        overlayRoot.setVisible(false);
        overlayRoot.setManaged(false);
        overlayRoot.setMouseTransparent(true);

        Rectangle dim = new Rectangle();
        dim.widthProperty().bind(parent.widthProperty());
        dim.heightProperty().bind(parent.heightProperty());
        dim.setFill(Color.rgb(5, 10, 20, 0.75));
        dim.setMouseTransparent(false);

        contentHolder = new StackPane();
        StackPane.setAlignment(contentHolder, Pos.CENTER);
        contentHolder.prefWidthProperty().bind(parent.widthProperty().multiply(0.42));
        contentHolder.prefHeightProperty().bind(parent.heightProperty().multiply(0.55));
        contentHolder.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        mainMenuContent = buildMainMenu();
        contentHolder.getChildren().add(mainMenuContent);

        overlayRoot.getChildren().addAll(dim, contentHolder);
    }

    public boolean isVisible() {
        return overlayRoot.getParent() != null && overlayRoot.isVisible();
    }

    /**
     * True when the pause overlay (or an active disconnect confirmation) is attached and can intercept mouse input.
     */
    public boolean blocksMouseInput() {
        if (activeConfirmationRoot != null && parent.getChildren().contains(activeConfirmationRoot)) {
            return true;
        }
        return overlayRoot.getParent() != null
                && overlayRoot.isVisible()
                && overlayRoot.isManaged()
                && !overlayRoot.isMouseTransparent();
    }

    public void show() {
        showPanel(Panel.MAIN);
        if (!parent.getChildren().contains(overlayRoot)) {
            parent.getChildren().add(overlayRoot);
        }
        overlayRoot.setManaged(true);
        overlayRoot.setVisible(true);
        overlayRoot.setMouseTransparent(false);
        overlayRoot.setPickOnBounds(true);
        overlayRoot.toFront();
    }

    public void hide() {
        dismissActiveConfirmation();
        overlayRoot.setVisible(false);
        overlayRoot.setManaged(false);
        overlayRoot.setMouseTransparent(true);
        overlayRoot.setPickOnBounds(false);
        parent.getChildren().remove(overlayRoot);
        showPanel(Panel.MAIN);
        if (onOverlayHidden != null) {
            onOverlayHidden.run();
        }
    }

    private void dismissActiveConfirmation() {
        if (activeConfirmationRoot != null) {
            parent.getChildren().remove(activeConfirmationRoot);
            activeConfirmationRoot = null;
        }
    }

    public void toggle() {
        if (isVisible()) {
            if (activePanel != Panel.MAIN) {
                showPanel(Panel.MAIN);
            } else {
                hide();
            }
        } else {
            show();
        }
    }

    private void showPanel(Panel panel) {
        activePanel = panel;
        contentHolder.getChildren().clear();
        switch (panel) {
            case SETTINGS -> contentHolder.getChildren().add(buildSettingsPanel());
            case RULES -> contentHolder.getChildren().add(buildRulesPanel());
            default -> contentHolder.getChildren().add(mainMenuContent);
        }
    }

    private VBox buildMainMenu() {
        StackPane panelBox = PanelUtil.createPanel(1.0, 1.0, CYAN, 2, 15, 10, parent);
        panelBox.setStyle(
                "-fx-background-color: rgba(10, 20, 40, 0.95);"
                        + "-fx-border-color: " + BRIGHT_CYAN + ";"
                        + "-fx-border-width: 2;"
                        + "-fx-background-radius: 12;"
                        + "-fx-border-radius: 12;"
        );
        GlowUtil.applyGlow(panelBox, BRIGHT_CYAN, 20);

        VBox layout = new VBox();
        layout.setAlignment(Pos.CENTER);
        layout.spacingProperty().bind(panelBox.heightProperty().multiply(0.06));
        layout.paddingProperty().bind(Bindings.createObjectBinding(
                () -> new Insets(
                        panelBox.getHeight() * 0.08,
                        panelBox.getWidth() * 0.1,
                        panelBox.getHeight() * 0.08,
                        panelBox.getWidth() * 0.1),
                panelBox.widthProperty(),
                panelBox.heightProperty()
        ));

        Label title = TextUtil.createText("PAUSED", FONT_HKMODULAR, 0.035, BRIGHT_CYAN, parent);
        title.setTextAlignment(TextAlignment.CENTER);

        ButtonsUtil resumeBtn = menuButton("RESUME", CYAN);
        ButtonsUtil settingsBtn = menuButton("SETTINGS", CYAN);
        ButtonsUtil tutorialBtn = menuButton("TUTORIAL", CYAN);
        ButtonsUtil disconnectBtn = menuButton("DISCONNECT", RED);

        resumeBtn.setOnMouseClicked(e -> hide());
        settingsBtn.setOnMouseClicked(e -> showPanel(Panel.SETTINGS));
        tutorialBtn.setOnMouseClicked(e -> showPanel(Panel.RULES));
        disconnectBtn.setOnMouseClicked(e -> confirmDisconnect());

        layout.getChildren().addAll(title, resumeBtn, settingsBtn, tutorialBtn, disconnectBtn);
        panelBox.getChildren().add(layout);

        VBox wrapper = new VBox(panelBox);
        wrapper.setAlignment(Pos.CENTER);
        VBox.setVgrow(panelBox, Priority.ALWAYS);
        return wrapper;
    }

    private VBox buildSettingsPanel() {
        StackPane panelBox = PanelUtil.createPanel(1.0, 1.0, CYAN, 2, 15, 10, parent);
        panelBox.setStyle(
                "-fx-background-color: rgba(10, 20, 40, 0.95);"
                        + "-fx-border-color: " + CYAN + ";"
                        + "-fx-border-width: 2;"
                        + "-fx-background-radius: 12;"
                        + "-fx-border-radius: 12;"
        );

        SettingsManager sm = SettingsManager.getInstance();

        VBox layout = new VBox(16);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.paddingProperty().bind(Bindings.createObjectBinding(
                () -> new Insets(24, 28, 24, 28),
                panelBox.widthProperty()
        ));

        Label title = TextUtil.createText("IN-GAME SETTINGS", FONT_HKMODULAR, 0.028, BRIGHT_CYAN, parent);
        Label note = TextUtil.createText(
                "Quick audio controls only. Display, gameplay, and other options remain in the main menu Settings screen.",
                FONT_HKMODULAR,
                0.012,
                WHITE,
                parent
        );
        note.setWrapText(true);
        note.setTextAlignment(TextAlignment.CENTER);
        note.maxWidthProperty().bind(panelBox.widthProperty().multiply(0.9));

        Slider masterVol = volumeSlider(sm.masterVolumeProperty().get() * 100);
        masterVol.valueProperty().addListener((obs, oldVal, newVal) -> {
            sm.masterVolumeProperty().set(newVal.doubleValue() / 100.0);
            AudioManager.getInstance().updateVolume();
            sm.saveSettings();
        });

        Slider musicVol = volumeSlider(sm.musicVolumeProperty().get() * 100);
        musicVol.valueProperty().addListener((obs, oldVal, newVal) -> {
            sm.musicVolumeProperty().set(newVal.doubleValue() / 100.0);
            AudioManager.getInstance().updateVolume();
            sm.saveSettings();
        });

        layout.getChildren().addAll(
                title,
                note,
                settingsRow("MASTER VOLUME", masterVol),
                settingsRow("MUSIC VOLUME", musicVol)
        );

        ButtonsUtil backBtn = menuButton("BACK", CYAN);
        backBtn.setOnMouseClicked(e -> showPanel(Panel.MAIN));
        layout.getChildren().add(backBtn);

        panelBox.getChildren().add(layout);

        VBox wrapper = new VBox(panelBox);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private VBox buildRulesPanel() {
        StackPane panelBox = PanelUtil.createPanel(1.0, 1.0, CYAN, 2, 15, 10, parent);
        panelBox.setStyle(
                "-fx-background-color: rgba(10, 20, 40, 0.95);"
                        + "-fx-border-color: " + CYAN + ";"
                        + "-fx-border-width: 2;"
                        + "-fx-background-radius: 12;"
                        + "-fx-border-radius: 12;"
        );

        VBox layout = new VBox(12);
        layout.setAlignment(Pos.TOP_LEFT);
        layout.paddingProperty().bind(Bindings.createObjectBinding(
                () -> new Insets(20, 24, 20, 24),
                panelBox.widthProperty()
        ));

        Label title = TextUtil.createText("GAME RULES", FONT_HKMODULAR, 0.028, YELLOW, parent);

        String rulesText = """
                Goal: develop countermeasures for all four cyber threats before the system collapses.

                Turn phases: take actions (Move, Treat, Share, Build, Discover), draw cards, then infect cities.

                Win: all four cures are discovered.
                Lose: too many outbreaks, out of malware cubes, or the player deck is empty.

                Full interactive tutorial is available from the main menu.""";

        Label body = TextUtil.createText(rulesText, FONT_HKMODULAR, 0.011, WHITE, parent);
        body.setWrapText(true);
        body.setTextAlignment(TextAlignment.LEFT);
        body.maxWidthProperty().bind(panelBox.widthProperty().multiply(0.92));

        ButtonsUtil backBtn = menuButton("BACK", CYAN);
        backBtn.setOnMouseClicked(e -> showPanel(Panel.MAIN));

        layout.getChildren().addAll(title, body, backBtn);
        panelBox.getChildren().add(layout);

        VBox wrapper = new VBox(panelBox);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private HBox settingsRow(String labelText, Slider slider) {
        Label label = TextUtil.createText(labelText, FONT_HKMODULAR, 0.014, CYAN, parent);
        HBox row = new HBox(12, label, slider);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(slider, Priority.ALWAYS);
        slider.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private Slider volumeSlider(double initialPercent) {
        Slider slider = new Slider(0, 100, initialPercent);
        slider.setStyle(
                "-fx-control-inner-background: black;"
                        + "-fx-accent: " + CYAN + ";"
        );
        return slider;
    }

    private ButtonsUtil menuButton(String text, String accentColor) {
        return new ButtonsUtil(
                text,
                accentColor,
                BLACK,
                accentColor,
                accentColor,
                2,
                10,
                10,
                0.75,
                0.09,
                0.018,
                parent
        );
    }

    private void confirmDisconnect() {
        dismissActiveConfirmation();
        ConfirmationOverlay confirm = new ConfirmationOverlay(
                parent,
                "DISCONNECT AND RETURN TO MAIN MENU?",
                "YES",
                "NO",
                () -> {
                    activeConfirmationRoot = null;
                    hide();
                    if (onDisconnectConfirmed != null) {
                        onDisconnectConfirmed.run();
                    }
                },
                () -> activeConfirmationRoot = null
        );
        activeConfirmationRoot = confirm.getRoot();
        parent.getChildren().add(activeConfirmationRoot);
        activeConfirmationRoot.toFront();
    }
}
