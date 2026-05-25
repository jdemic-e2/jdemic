package jdemic.Scenes.Settings;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import jdemic.Scenes.SceneManager.SceneManager;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.PanelUtil;
import jdemic.ui.SafeResourceLoader;
import jdemic.ui.TextUtil;

import java.util.function.UnaryOperator;

public class SettingsScene {
    private static final String FONT_HKMODULAR = "hkmodular";
    private static final String CYAN = "#00b5d4";
    private static final String RED = "#ff0000";
    private static final String BLACK = "black";
    private static final String WHITE = "#ffffff";
    private static final String TAB_GENERAL = "GENERAL";
    private static final String TAB_AUDIO = "AUDIO";
    private static final String TAB_DISPLAY = "DISPLAY";
    private static final String TAB_GAMEPLAY = "GAMEPLAY";
    private static final String SCENE_MAIN_MENU = "MAIN_MENU";
    private static final String BACKGROUND_RESOURCE = "/background.png";
    private static final String BUTTON_LABEL_BACK = "BACK";
    private static final String DISCARD_CHANGES_MESSAGE = "DISCARD CHANGES?";
    private static final String COMBO_STYLE = "-fx-background-color: black; -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-font-family: 'hkmodular';";

    private StackPane root;
    private Stage stage;

    // State Tracking
    private boolean hasUnsavedChanges = false;
    private String activeTab = ""; // Prevents re-clicking the same tab

    // Layout Containers
    private StackPane contentArea;
    private StackPane confirmationOverlay;
    private Label overlayText;
    private Runnable onConfirmAction;

    // Cached Views (so data isn't lost when switching tabs)
    private VBox generalView, audioView, displayView, gameplayView;

    // UI Control References (for saving/loading data)
    private TextField nameField;
    private Slider masterVol, musicVol, sfxVol;
    private ComboBox<String> resCombo;
    private ToggleButton fsToggle;
    private ComboBox<String> speedCombo;

    public SettingsScene(Stage stage) {
        this.stage = stage;
        this.root = new StackPane();
        setupStylesheet();
        jdemic.Scenes.SceneUtil.setBackground(root);
        setupConfirmationOverlay();
        setupUI();
    }

    private void setupStylesheet() {
        java.net.URL stylesheet = getClass().getResource("/styles/settings.css");
        if (stylesheet != null) {
            root.getStylesheets().add(stylesheet.toExternalForm());
        } else {
            System.err.println("[SettingsScene] Missing resource: /styles/settings.css");
        }
    }

    private void setupUI() {
        StackPane transparentBox = new StackPane();
        transparentBox.maxWidthProperty().bind(root.widthProperty().multiply(0.85));
        transparentBox.maxHeightProperty().bind(root.heightProperty().multiply(0.7));
        transparentBox.setPickOnBounds(false); // Lets clicks pass through the empty space

        HBox layout = new HBox(30);
        layout.setPadding(new Insets(40));
        layout.setPickOnBounds(false);

        VBox sidebar = new VBox(20);
        sidebar.setFillWidth(false);
        sidebar.setAlignment(Pos.CENTER_LEFT);
        sidebar.setPickOnBounds(false);

        StackPane.setAlignment(transparentBox, Pos.CENTER_LEFT);
        layout.setAlignment(Pos.CENTER_LEFT);

        ButtonsUtil genBtn = new ButtonsUtil(TAB_GENERAL, CYAN, BLACK, CYAN, CYAN, 2, 15, 15, 0.18, 0.08, 0.02, root);
        ButtonsUtil audBtn = new ButtonsUtil(TAB_AUDIO, CYAN, BLACK, CYAN, CYAN, 2, 15, 15, 0.18, 0.08, 0.02, root);
        ButtonsUtil dispBtn = new ButtonsUtil(TAB_DISPLAY, CYAN, BLACK, CYAN, CYAN, 2, 15, 15, 0.18, 0.08, 0.02, root);
        ButtonsUtil gameBtn = new ButtonsUtil(TAB_GAMEPLAY, CYAN, BLACK, CYAN, CYAN, 2, 15, 15, 0.18, 0.08, 0.02, root);
        sidebar.getChildren().addAll(genBtn, audBtn, dispBtn, gameBtn);
        sidebar.translateXProperty().bind(root.widthProperty().multiply(0.01));

        contentArea = new StackPane();
        HBox.setHgrow(contentArea, Priority.ALWAYS);
        contentArea.setPickOnBounds(false);

        // Pre-build the views ONCE so they hold state
        generalView = createGeneralContent();
        audioView = createAudioContent();
        displayView = createDisplayContent();
        gameplayView = createGameplayContent();

        // Navigation Logic (Using the switchTab helper)
        genBtn.setOnMouseClicked(e -> switchTab(TAB_GENERAL, generalView));
        audBtn.setOnMouseClicked(e -> switchTab(TAB_AUDIO, audioView));
        dispBtn.setOnMouseClicked(e -> switchTab(TAB_DISPLAY, displayView));
        gameBtn.setOnMouseClicked(e -> switchTab(TAB_GAMEPLAY, gameplayView));

        // Start on Display tab
        switchTab(TAB_DISPLAY, displayView);

        Region spaceLeft = new Region();
        HBox.setHgrow(spaceLeft,Priority.ALWAYS);

        Region spaceRight = new Region();
        HBox.setHgrow(spaceRight,Priority.ALWAYS);

        layout.getChildren().addAll(sidebar, spaceLeft, contentArea, spaceRight);
        transparentBox.getChildren().add(layout); // Added to our new invisible box

        HBox footer = new HBox(30);
        footer.setAlignment(Pos.BOTTOM_CENTER);
        StackPane.setAlignment(footer, Pos.BOTTOM_CENTER);
        StackPane.setMargin(footer, new Insets(0, 0, 40, 0));
        footer.setPickOnBounds(false);

        ButtonsUtil backBtn = new ButtonsUtil(BUTTON_LABEL_BACK, CYAN, BLACK, CYAN, CYAN, 2, 10, 10, 0.12, 0.06, 0.01, root);
        ButtonsUtil resetBtn = new ButtonsUtil("RESET", CYAN, BLACK, CYAN, CYAN, 2, 10, 10, 0.12, 0.06, 0.01, root);
        ButtonsUtil applyBtn = new ButtonsUtil("APPLY", CYAN, BLACK, CYAN, CYAN, 2, 10, 10, 0.12, 0.06, 0.01, root);
        ButtonsUtil exitBtn = new ButtonsUtil("EXIT", CYAN, BLACK, CYAN, CYAN, 2, 10, 10, 0.12, 0.06, 0.01, root);

        backBtn.setOnMouseClicked(e -> {
            if (hasUnsavedChanges) showOverlay(DISCARD_CHANGES_MESSAGE, this::returnToMainMenu);
            else returnToMainMenu();
        });

        resetBtn.setOnMouseClicked(e -> {
            if (hasUnsavedChanges) {
                showOverlay(DISCARD_CHANGES_MESSAGE, this::resetUIToManager);
            } else {
                showOverlay("RESET TO DEFAULTS?", this::resetToDefaults);
            }
        });

        applyBtn.setOnMouseClicked(e -> {
            saveToManager(); // Pushes UI data to the Manager
        });

        exitBtn.setOnMouseClicked(e -> {
            if (hasUnsavedChanges) {
                showOverlay("SAVE CHANGES BEFORE EXIT?", () -> {
                    saveToManager();
                    returnToMainMenu();
                });
            } else returnToMainMenu();
        });

        footer.getChildren().addAll(backBtn, resetBtn, applyBtn, exitBtn);

        // Add the invisible top box and the footer to the root
        root.getChildren().addAll(transparentBox, footer);
    }

    // Tab switching
    private void switchTab(String tabName, VBox newContent) {
        if (activeTab.equals(tabName)) return; // Ignore click if already on this tab
        activeTab = tabName;
        contentArea.getChildren().clear();
        contentArea.getChildren().add(newContent);
    }

    //Save and reset data
    private void saveToManager() {
        SettingsManager sm = SettingsManager.getInstance();

        // AUDIO
        sm.masterVolumeProperty().set(masterVol.getValue() / 100.0);
        sm.musicVolumeProperty().set(musicVol.getValue() / 100.0);
        sm.sfxVolumeProperty().set(sfxVol.getValue() / 100.0);
        // Update volume
        AudioManager.getInstance().updateVolume();

        // Name
        sm.playerNameProperty().set(nameField.getText());

        // Speed
        sm.animationSpeedProperty().set(speedCombo.getValue());

        // DISPLAY
        javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();
        double screenHeight = screen.getBounds().getHeight();

        String selectedRes = resCombo.getValue();
        String[] parts = selectedRes.split("x");
        int chosenWidth = Integer.parseInt(parts[0]);
        int chosenHeight = Integer.parseInt(parts[1]);

        boolean wantFullScreen = fsToggle.isSelected();

        sm.resolutionProperty().set(selectedRes);

        // Only the immediate window resize is limited by the current monitor bounds.
        if (!wantFullScreen && chosenWidth <= screenWidth && chosenHeight <= screenHeight) {
            stage.setFullScreen(false);
            stage.setWidth(chosenWidth);
            stage.setHeight(chosenHeight);
            stage.centerOnScreen();
        }

        // Fullscreen setting apply
        sm.isFullscreenProperty().set(wantFullScreen);
        javafx.application.Platform.runLater(() -> {
            if (stage.isFullScreen() != wantFullScreen) {
                stage.setFullScreen(wantFullScreen);
            }
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);
        });

        sm.saveSettings();
        hasUnsavedChanges = false;
        System.out.println("Saved changes!");
    }

    private void resetUIToManager() {
        SettingsManager sm = SettingsManager.getInstance();
        nameField.setText(sm.playerNameProperty().get());
        resCombo.setValue(sm.resolutionProperty().get());
        fsToggle.setSelected(sm.isFullscreenProperty().get());
        updateFsToggleStyle(); // Refresh the visual color of the button
        masterVol.setValue(sm.masterVolumeProperty().get() * 100);
        musicVol.setValue(sm.musicVolumeProperty().get() * 100);
        sfxVol.setValue(sm.sfxVolumeProperty().get() * 100);
        speedCombo.setValue(sm.animationSpeedProperty().get());

        hasUnsavedChanges = false;
    }

    private void resetToDefaults() {
        SettingsData defaults = new SettingsData();
        nameField.setText(defaults.playerName);
        resCombo.setValue(defaults.resolution);
        fsToggle.setSelected(defaults.isFullScreen);
        updateFsToggleStyle();
        masterVol.setValue(defaults.masterVolume * 100);
        musicVol.setValue(defaults.musicVolume * 100);
        sfxVol.setValue(defaults.sfxVolume * 100);
        speedCombo.setValue(defaults.animationSpeed);

        saveToManager();
    }

    private void returnToMainMenu() {
        SceneManager.switchScene(SCENE_MAIN_MENU);
    }

    private void markDirty() {
        hasUnsavedChanges = true;
    }

    // --- CONTENT CREATION ---

    private VBox createDisplayContent() {
        VBox box = new VBox(25);
        box.setAlignment(Pos.TOP_CENTER);
        SettingsManager sm = SettingsManager.getInstance();

        Label header = TextUtil.createText("DISPLAY SETTINGS", FONT_HKMODULAR, 0.03, WHITE, root);

        resCombo = new ComboBox<>();
        resCombo.getItems().addAll("800x600","1280x720", "1600x900", "1920x1080");
        resCombo.setValue(sm.resolutionProperty().get()); // Load from manager
        resCombo.setStyle(COMBO_STYLE);
        resCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: " + CYAN + "; -fx-background-color: " + BLACK + ";");
            }
        });
        resCombo.setOnAction(e -> markDirty());

        fsToggle = new ToggleButton();
        fsToggle.setSelected(sm.isFullscreenProperty().get()); // Load from manager
        updateFsToggleStyle();
        fsToggle.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> AudioManager.getInstance().playButtonSFX());
        fsToggle.setOnAction(e -> {
            updateFsToggleStyle();
            markDirty();
        });

        box.getChildren().addAll(header, createSettingRow("RESOLUTION", resCombo), createSettingRow("FULLSCREEN", fsToggle));
        return box;
    }

    private void updateFsToggleStyle() {
        boolean isOn = fsToggle.isSelected();
        fsToggle.setText(isOn ? "ON" : "OFF");
        fsToggle.getStyleClass().removeAll("cyber-toggle", "cyber-toggle-on", "cyber-toggle-off");
        fsToggle.getStyleClass().addAll("cyber-toggle", isOn ? "cyber-toggle-on" : "cyber-toggle-off");
    }

    private VBox createAudioContent() {
        VBox box = new VBox(25);
        box.setAlignment(Pos.TOP_CENTER);
        SettingsManager sm = SettingsManager.getInstance();

        Label header = TextUtil.createText("AUDIO SETTINGS", FONT_HKMODULAR, 0.03, WHITE, root);

        masterVol = new Slider(0, 100, sm.masterVolumeProperty().get() * 100);
        styleSlider(masterVol);
        masterVol.valueProperty().addListener((obs, oldVal, newVal) -> markDirty());

        musicVol = new Slider(0, 100, sm.musicVolumeProperty().get() * 100);
        styleSlider(musicVol);
        musicVol.valueProperty().addListener((obs, oldVal, newVal) -> markDirty());

        sfxVol = new Slider(0, 100, sm.sfxVolumeProperty().get() * 100);
        styleSlider(sfxVol);
        sfxVol.valueProperty().addListener((obs, oldVal, newVal) -> markDirty());

        box.getChildren().addAll(
                header,
                createSettingRow("MASTER VOLUME", masterVol),
                createSettingRow("MUSIC VOLUME", musicVol),
                createSettingRow("SFX VOLUME", sfxVol)
        );
        return box;
    }

    private void styleSlider(Slider slider) {
        slider.getStyleClass().add("cyber-slider");
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
        slider.setBlockIncrement(5);
    }

    private VBox createGeneralContent() {
        VBox box = new VBox(25);
        box.setAlignment(Pos.TOP_CENTER);
        SettingsManager sm = SettingsManager.getInstance();

        Label header = TextUtil.createText("GENERAL SETTINGS", FONT_HKMODULAR, 0.03, WHITE, root);
        nameField = new TextField();

        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText();
            if (text.matches("[a-zA-Z0-9]*") && text.length() <= 16) return change;
            return null;
        };

        nameField.setTextFormatter(new TextFormatter<>(filter));
        nameField.setPromptText("Enter Username");
        nameField.setText(sm.playerNameProperty().get()); // Load from manager
        nameField.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: " + CYAN + "; -fx-border-color: " + CYAN + "; -fx-border-width: 1; -fx-font-family: '" + FONT_HKMODULAR + "'; -fx-font-size: 12px;");
        nameField.setMaxWidth(300);
        nameField.textProperty().addListener((obs, oldVal, newVal) -> markDirty());

        box.getChildren().addAll(header, createSettingRow("PLAYER NAME", nameField));
        return box;
    }

    private VBox createGameplayContent() {
        VBox box = new VBox(25);
        box.setAlignment(Pos.TOP_CENTER);
        SettingsManager sm = SettingsManager.getInstance();

        Label header = TextUtil.createText("GAMEPLAY SETTINGS", FONT_HKMODULAR, 0.03, WHITE, root);

        speedCombo = new ComboBox<>();
        speedCombo.getItems().addAll("SLOW", "MEDIUM", "FAST");
        speedCombo.setValue(sm.animationSpeedProperty().get()); // Load from manager
        speedCombo.setStyle(COMBO_STYLE);

        speedCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill: " + CYAN + "; -fx-background-color: " + BLACK + "; -fx-font-family: '" + FONT_HKMODULAR + "';");
            }
        });

        speedCombo.setOnAction(e -> markDirty());

        box.getChildren().addAll(header, createSettingRow("ANIMATION SPEED", speedCombo));
        return box;
    }

    private HBox createSettingRow(String labelText, Node control) {
        Label lbl = TextUtil.createText(labelText, FONT_HKMODULAR, 0.015, CYAN, root);
        HBox row = new HBox(20, lbl, control);
        row.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().add(1, spacer);
        return row;
    }

    private void setupConfirmationOverlay() {
        confirmationOverlay = new StackPane();
        confirmationOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.8);");
        confirmationOverlay.setVisible(false);

        StackPane dialogBox = PanelUtil.createPanel(0.4, 0.3, RED, 2, 15, 0, root);
        dialogBox.setStyle("-fx-background-color: " + BLACK + "; -fx-border-color: " + RED + "; -fx-border-width: 2;");

        VBox content = new VBox(30);
        content.setAlignment(Pos.CENTER);

        overlayText = TextUtil.createText("UNSAVED CHANGES", FONT_HKMODULAR, 0.02, WHITE, root);

        HBox btnBox = new HBox(20);
        btnBox.setAlignment(Pos.CENTER);

        ButtonsUtil yesBtn = new ButtonsUtil("YES", RED, BLACK, RED, RED, 2, 10, 10, 0.1, 0.05, 0.01, root);
        ButtonsUtil noBtn = new ButtonsUtil("NO", CYAN, BLACK, CYAN, CYAN, 2, 10, 10, 0.1, 0.05, 0.01, root);

        yesBtn.setOnMouseClicked(e -> {
            confirmationOverlay.setVisible(false);
            if (onConfirmAction != null) onConfirmAction.run();
        });

        noBtn.setOnMouseClicked(e -> confirmationOverlay.setVisible(false));

        btnBox.getChildren().addAll(yesBtn, noBtn);
        content.getChildren().addAll(overlayText, btnBox);
        dialogBox.getChildren().add(content);

        confirmationOverlay.getChildren().add(dialogBox);
        root.getChildren().add(confirmationOverlay);
    }

    private void showOverlay(String message, Runnable onConfirm) {
        overlayText.setText(message);
        this.onConfirmAction = onConfirm;
        confirmationOverlay.setVisible(true);
        confirmationOverlay.toFront();
    }



    public StackPane getRoot() {return root;}
}
