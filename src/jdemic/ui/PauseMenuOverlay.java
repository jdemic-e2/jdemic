package jdemic.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
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

    private static final String LABEL_PAUSED = "PAUSED";
    private static final String LABEL_RESUME = "RESUME";
    private static final String LABEL_SETTINGS = "SETTINGS";
    private static final String LABEL_TUTORIAL = "TUTORIAL";
    private static final String LABEL_DISCONNECT = "DISCONNECT";
    private static final String LABEL_BACK = "BACK";
    private static final String LABEL_YES = "YES";
    private static final String LABEL_NO = "NO";
    private static final String MSG_DISCONNECT_CONFIRM = "DISCONNECT AND RETURN TO MAIN MENU?";

    private static final double OVERLAY_WIDTH_RATIO = 0.42;
    private static final double OVERLAY_HEIGHT_RATIO = 0.55;
    private static final double DIM_OPACITY = 0.75;
    private static final int PANEL_BORDER_WIDTH = 2;
    private static final int PANEL_CORNER_RADIUS = 12;
    private static final double MAIN_PANEL_GLOW_RADIUS = 20.0;
    private static final double VOLUME_SLIDER_MAX = 100.0;
    private static final double VOLUME_PERCENT_SCALE = 100.0;

    private static final double BTN_WIDTH_RATIO = 0.75;
    private static final double BTN_HEIGHT_RATIO = 0.09;
    private static final double BTN_FONT_RATIO = 0.018;

    public enum Panel {
        MAIN,
        SETTINGS,
        RULES
    }

    private final StackPane parent;
    private final Runnable onDisconnectConfirmed;
    private final Runnable onOverlayHidden;
    private final StackPane overlayRoot;
    private final StackPane contentHolder;
    private final VBox mainMenuContent;
    private Panel activePanel = Panel.MAIN;
    private StackPane activeConfirmationRoot;

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
        dim.setFill(Color.rgb(5, 10, 20, DIM_OPACITY));
        dim.setMouseTransparent(false);

        contentHolder = new StackPane();
        StackPane.setAlignment(contentHolder, Pos.CENTER);
        contentHolder.prefWidthProperty().bind(parent.widthProperty().multiply(OVERLAY_WIDTH_RATIO));
        contentHolder.prefHeightProperty().bind(parent.heightProperty().multiply(OVERLAY_HEIGHT_RATIO));
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

    public void toggle() {
        if (!isVisible()) {
            show();
            return;
        }
        if (activePanel != Panel.MAIN) {
            showPanel(Panel.MAIN);
        } else {
            hide();
        }
    }

    private void dismissActiveConfirmation() {
        if (activeConfirmationRoot != null) {
            parent.getChildren().remove(activeConfirmationRoot);
            activeConfirmationRoot = null;
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
        StackPane panelBox = createPanelBox(BRIGHT_CYAN, MAIN_PANEL_GLOW_RADIUS);
        VBox layout = createCenteredLayout(panelBox, 0.06, 0.08, 0.1);

        Label title = TextUtil.createText(LABEL_PAUSED, FONT_HKMODULAR, 0.035, BRIGHT_CYAN, parent);
        title.setTextAlignment(TextAlignment.CENTER);

        ButtonsUtil resumeBtn = menuButton(LABEL_RESUME, CYAN);
        ButtonsUtil settingsBtn = menuButton(LABEL_SETTINGS, CYAN);
        ButtonsUtil tutorialBtn = menuButton(LABEL_TUTORIAL, CYAN);
        ButtonsUtil disconnectBtn = menuButton(LABEL_DISCONNECT, RED);

        resumeBtn.setOnMouseClicked(e -> hide());
        settingsBtn.setOnMouseClicked(e -> showPanel(Panel.SETTINGS));
        tutorialBtn.setOnMouseClicked(e -> showPanel(Panel.RULES));
        disconnectBtn.setOnMouseClicked(e -> confirmDisconnect());

        layout.getChildren().addAll(title, resumeBtn, settingsBtn, tutorialBtn, disconnectBtn);
        panelBox.getChildren().add(layout);
        return wrapPanel(panelBox);
    }

    private VBox buildSettingsPanel() {
        StackPane panelBox = createPanelBox(CYAN, 0);
        SettingsManager settingsManager = SettingsManager.getInstance();

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

        Slider masterVol = volumeSlider(settingsManager.masterVolumeProperty().get() * VOLUME_PERCENT_SCALE);
        Slider musicVol = volumeSlider(settingsManager.musicVolumeProperty().get() * VOLUME_PERCENT_SCALE);
        bindVolumeSlider(masterVol, settingsManager.masterVolumeProperty());
        bindVolumeSlider(musicVol, settingsManager.musicVolumeProperty());

        layout.getChildren().addAll(
                title,
                note,
                settingsRow("MASTER VOLUME", masterVol),
                settingsRow("MUSIC VOLUME", musicVol),
                backButton()
        );
        panelBox.getChildren().add(layout);
        return wrapPanel(panelBox);
    }

    private VBox buildRulesPanel() {
        StackPane panelBox = createPanelBox(CYAN, 0);

        VBox layout = new VBox(12);
        layout.setAlignment(Pos.TOP_LEFT);
        layout.paddingProperty().bind(Bindings.createObjectBinding(
                () -> new Insets(20, 24, 20, 24),
                panelBox.widthProperty()
        ));

        Label title = TextUtil.createText("GAME RULES", FONT_HKMODULAR, 0.028, YELLOW, parent);
        Label body = TextUtil.createText(RULES_SUMMARY_TEXT, FONT_HKMODULAR, 0.011, WHITE, parent);
        body.setWrapText(true);
        body.setTextAlignment(TextAlignment.LEFT);
        body.maxWidthProperty().bind(panelBox.widthProperty().multiply(0.92));

        layout.getChildren().addAll(title, body, backButton());
        panelBox.getChildren().add(layout);
        return wrapPanel(panelBox);
    }

    private static final String RULES_SUMMARY_TEXT = """
            Goal: develop countermeasures for all four cyber threats before the system collapses.

            Turn phases: take actions (Move, Treat, Share, Build, Discover), draw cards, then infect cities.

            Win: all four cures are discovered.
            Lose: too many outbreaks, out of malware cubes, or the player deck is empty.

            Full interactive tutorial is available from the main menu.""";

    private ButtonsUtil backButton() {
        ButtonsUtil backBtn = menuButton(LABEL_BACK, CYAN);
        backBtn.setOnMouseClicked(e -> showPanel(Panel.MAIN));
        return backBtn;
    }

    private StackPane createPanelBox(String borderColor, double glowRadius) {
        StackPane panelBox = PanelUtil.createPanel(1.0, 1.0, CYAN, PANEL_BORDER_WIDTH, 15, 10, parent);
        panelBox.setStyle(panelBoxStyle(borderColor));
        if (glowRadius > 0) {
            GlowUtil.applyGlow(panelBox, borderColor, glowRadius);
        }
        return panelBox;
    }

    private static String panelBoxStyle(String borderColor) {
        return "-fx-background-color: rgba(10, 20, 40, 0.95);"
                + "-fx-border-color: " + borderColor + ";"
                + "-fx-border-width: " + PANEL_BORDER_WIDTH + ";"
                + "-fx-background-radius: " + PANEL_CORNER_RADIUS + ";"
                + "-fx-border-radius: " + PANEL_CORNER_RADIUS + ";";
    }

    private VBox createCenteredLayout(StackPane panelBox, double spacingRatio, double padVertical, double padHorizontal) {
        VBox layout = new VBox();
        layout.setAlignment(Pos.CENTER);
        layout.spacingProperty().bind(panelBox.heightProperty().multiply(spacingRatio));
        layout.paddingProperty().bind(Bindings.createObjectBinding(
                () -> new Insets(
                        panelBox.getHeight() * padVertical,
                        panelBox.getWidth() * padHorizontal,
                        panelBox.getHeight() * padVertical,
                        panelBox.getWidth() * padHorizontal),
                panelBox.widthProperty(),
                panelBox.heightProperty()
        ));
        return layout;
    }

    private static VBox wrapPanel(StackPane panelBox) {
        VBox wrapper = new VBox(panelBox);
        wrapper.setAlignment(Pos.CENTER);
        VBox.setVgrow(panelBox, Priority.ALWAYS);
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

    private void bindVolumeSlider(Slider slider, DoubleProperty volumeProperty) {
        slider.valueProperty().addListener((obs, oldVal, newVal) ->
                applyVolumePercent(volumeProperty, newVal.doubleValue()));
    }

    private void applyVolumePercent(DoubleProperty volumeProperty, double percent) {
        volumeProperty.set(percent / VOLUME_PERCENT_SCALE);
        AudioManager.getInstance().updateVolume();
        SettingsManager.getInstance().saveSettings();
    }

    private Slider volumeSlider(double initialPercent) {
        Slider slider = new Slider(0, VOLUME_SLIDER_MAX, initialPercent);
        slider.setStyle("-fx-control-inner-background: black; -fx-accent: " + CYAN + ";");
        return slider;
    }

    private ButtonsUtil menuButton(String text, String accentColor) {
        return new ButtonsUtil(
                text,
                accentColor,
                BLACK,
                accentColor,
                accentColor,
                PANEL_BORDER_WIDTH,
                10,
                10,
                BTN_WIDTH_RATIO,
                BTN_HEIGHT_RATIO,
                BTN_FONT_RATIO,
                parent
        );
    }

    private void confirmDisconnect() {
        dismissActiveConfirmation();
        ConfirmationOverlay confirm = new ConfirmationOverlay(
                parent,
                MSG_DISCONNECT_CONFIRM,
                LABEL_YES,
                LABEL_NO,
                this::handleDisconnectConfirmed,
                this::clearActiveConfirmationReference
        );
        activeConfirmationRoot = confirm.getRoot();
        parent.getChildren().add(activeConfirmationRoot);
        activeConfirmationRoot.toFront();
    }

    private void handleDisconnectConfirmed() {
        clearActiveConfirmationReference();
        hide();
        if (onDisconnectConfirmed != null) {
            onDisconnectConfirmed.run();
        }
    }

    private void clearActiveConfirmationReference() {
        activeConfirmationRoot = null;
    }
}
