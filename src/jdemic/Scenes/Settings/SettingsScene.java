package jdemic.Scenes.Settings;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import jdemic.Scenes.MainMenuScene;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.PanelUtil;
import jdemic.ui.TextUtil;

import java.util.function.UnaryOperator;

public class SettingsScene {
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
    private Slider masterVol, musicVol;
    private ComboBox<String> resCombo;
    private ToggleButton fsToggle;
    private ComboBox<String> speedCombo;

    public SettingsScene(Stage stage) {
        this.stage = stage;
        this.root = new StackPane();
        setupBackground();
        setupConfirmationOverlay();
        setupUI();
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

        ButtonsUtil genBtn = new ButtonsUtil("GENERAL", "#00b5d4", "black", "#00b5d4", "#00b5d4", 2, 15, 15, 0.18, 0.08, 0.02, root);
        ButtonsUtil audBtn = new ButtonsUtil("AUDIO", "#00b5d4", "black", "#00b5d4", "#00b5d4", 2, 15, 15, 0.18, 0.08, 0.02, root);
        ButtonsUtil dispBtn = new ButtonsUtil("DISPLAY", "#00b5d4", "black", "#00b5d4", "#00b5d4", 2, 15, 15, 0.18, 0.08, 0.02, root);
        ButtonsUtil gameBtn = new ButtonsUtil("GAMEPLAY", "#00b5d4", "black", "#00b5d4", "#00b5d4", 2, 15, 15, 0.18, 0.08, 0.02, root);
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
        genBtn.setOnMouseClicked(e -> switchTab("GENERAL", generalView));
        audBtn.setOnMouseClicked(e -> switchTab("AUDIO", audioView));
        dispBtn.setOnMouseClicked(e -> switchTab("DISPLAY", displayView));
        gameBtn.setOnMouseClicked(e -> switchTab("GAMEPLAY", gameplayView));

        // Start on Display tab
        switchTab("DISPLAY", displayView);

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

        ButtonsUtil backBtn = new ButtonsUtil("BACK", "#00b5d4", "black", "#00b5d4", "#00b5d4", 2, 10, 10, 0.12, 0.06, 0.01, root);
        ButtonsUtil resetBtn = new ButtonsUtil("RESET", "#00b5d4", "black", "#00b5d4", "#00b5d4", 2, 10, 10, 0.12, 0.06, 0.01, root);
        ButtonsUtil applyBtn = new ButtonsUtil("APPLY", "#00b5d4", "black", "#00b5d4", "#00b5d4", 2, 10, 10, 0.12, 0.06, 0.01, root);
        ButtonsUtil exitBtn = new ButtonsUtil("EXIT", "#00b5d4", "black", "#00b5d4", "00b5d4", 2, 10, 10, 0.12, 0.06, 0.01, root);

        //THIS IS FOR DEBUG ONLY
        //REMOVE LATER WHEN ALL SETTINGS FUNCTION GOOD AND WELL
        ButtonsUtil debugBtn = new ButtonsUtil("PRINT DEBUG", "#00b5d4", "black", "#00b5d4", "#00b5d4", 2 , 10 , 10 , 0.15, 0.06, 0.01, root);

        backBtn.setOnMouseClicked(e -> {
            if (hasUnsavedChanges) showOverlay("DISCARD CHANGES?", this::returnToMainMenu);
            else returnToMainMenu();
        });

        resetBtn.setOnMouseClicked(e -> {
            if (hasUnsavedChanges) {
                showOverlay("DISCARD CHANGES?", () -> {
                    resetUIToManager(); // Pulls original data back into UI
                });
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

        //DEBUG ONLY
        debugBtn.setOnMouseClicked(e -> {
            System.out.println("==== DEBUG DATA ONLY ====");
            SettingsManager.getInstance().printAll();
            System.out.println("=========================\n");
        });

        footer.getChildren().addAll(backBtn, resetBtn, applyBtn, exitBtn, debugBtn);

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

        // Check resolution (has to be same or lower than the monitor's normal resolution)
        if (chosenWidth <= screenWidth && chosenHeight <= screenHeight) {
            sm.resolutionProperty().set(selectedRes);

            if (!wantFullScreen) {
                stage.setFullScreen(false);
                stage.setWidth(chosenWidth);
                stage.setHeight(chosenHeight);
                stage.centerOnScreen();
            }
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
        speedCombo.setValue(sm.animationSpeedProperty().get());

        hasUnsavedChanges = false;
    }

    private void returnToMainMenu() {
        stage.getScene().setRoot(new MainMenuScene(stage).getRoot());
    }

    private void markDirty() {
        hasUnsavedChanges = true;
    }

    // --- CONTENT CREATION ---

    private VBox createDisplayContent() {
        VBox box = new VBox(25);
        box.setAlignment(Pos.TOP_CENTER);
        SettingsManager sm = SettingsManager.getInstance();

        Label header = TextUtil.createText("DISPLAY SETTINGS", "hkmodular", 0.03, "#ffffff", root);

        resCombo = new ComboBox<>();
        resCombo.getItems().addAll("1280x720", "1600x900", "1920x1080");
        resCombo.setValue(sm.resolutionProperty().get()); // Load from manager
        resCombo.setStyle("-fx-background-color: black; -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-font-family: 'hkmodular';");
        resCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill: #00b5d4; -fx-background-color: black;");
            }
        });
        resCombo.setOnAction(e -> markDirty());

        fsToggle = new ToggleButton();
        fsToggle.setSelected(sm.isFullscreenProperty().get()); // Load from manager
        updateFsToggleStyle();
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
        String bgColor = isOn ? "black" : "#00b5d4";
        String textColor = isOn ? "#00b5d4" : "black";
        fsToggle.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + "; -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-font-family: 'hkmodular'; -fx-padding: 5 15 5 15;");
    }

    private VBox createAudioContent() {
        VBox box = new VBox(25);
        box.setAlignment(Pos.TOP_CENTER);
        SettingsManager sm = SettingsManager.getInstance();

        Label header = TextUtil.createText("AUDIO SETTINGS", "hkmodular", 0.03, "#ffffff", root);

        masterVol = new Slider(0, 100, sm.masterVolumeProperty().get() * 100);
        masterVol.valueProperty().addListener((obs, oldVal, newVal) -> markDirty());

        musicVol = new Slider(0, 100, sm.musicVolumeProperty().get() * 100);
        musicVol.valueProperty().addListener((obs, oldVal, newVal) -> markDirty());

        box.getChildren().addAll(header, createSettingRow("MASTER VOLUME", masterVol), createSettingRow("MUSIC VOLUME", musicVol));
        return box;
    }

    private VBox createGeneralContent() {
        VBox box = new VBox(25);
        box.setAlignment(Pos.TOP_CENTER);
        SettingsManager sm = SettingsManager.getInstance();

        Label header = TextUtil.createText("GENERAL SETTINGS", "hkmodular", 0.03, "#ffffff", root);
        nameField = new TextField();

        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText();
            if (text.matches("[a-zA-Z0-9]*") && text.length() <= 16) return change;
            return null;
        };

        nameField.setTextFormatter(new TextFormatter<>(filter));
        nameField.setPromptText("Enter Username");
        nameField.setText(sm.playerNameProperty().get()); // Load from manager
        nameField.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: #00b5d4; -fx-border-color: #00b5d4; -fx-border-width: 1; -fx-font-family: 'hkmodular'; -fx-font-size: 12px;");
        nameField.setMaxWidth(300);
        nameField.textProperty().addListener((obs, oldVal, newVal) -> markDirty());

        box.getChildren().addAll(header, createSettingRow("PLAYER NAME", nameField));
        return box;
    }

    private VBox createGameplayContent() {
        VBox box = new VBox(25);
        box.setAlignment(Pos.TOP_CENTER);
        SettingsManager sm = SettingsManager.getInstance();

        Label header = TextUtil.createText("GAMEPLAY SETTINGS", "hkmodular", 0.03, "#ffffff", root);

        speedCombo = new ComboBox<>();
        speedCombo.getItems().addAll("SLOW", "MEDIUM", "FAST");
        speedCombo.setValue(sm.animationSpeedProperty().get()); // Load from manager
        speedCombo.setStyle("-fx-background-color: black; -fx-border-color: #00b5d4; -fx-border-width: 2; -fx-font-family: 'hkmodular';");

        speedCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill: #00b5d4; -fx-background-color: black; -fx-font-family: 'hkmodular';");
            }
        });

        speedCombo.setOnAction(e -> markDirty());

        box.getChildren().addAll(header, createSettingRow("ANIMATION SPEED", speedCombo));
        return box;
    }

    private HBox createSettingRow(String labelText, Node control) {
        Label lbl = TextUtil.createText(labelText, "hkmodular", 0.015, "#00b5d4", root);
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

        StackPane dialogBox = PanelUtil.createPanel(0.4, 0.3, "#ff0000", 2, 15, 0, root);
        dialogBox.setStyle("-fx-background-color: black; -fx-border-color: #ff0000; -fx-border-width: 2;");

        VBox content = new VBox(30);
        content.setAlignment(Pos.CENTER);

        overlayText = TextUtil.createText("UNSAVED CHANGES", "hkmodular", 0.02, "#ffffff", root);

        HBox btnBox = new HBox(20);
        btnBox.setAlignment(Pos.CENTER);

        ButtonsUtil yesBtn = new ButtonsUtil("YES", "#ff0000", "black", "#ff0000", "#ff0000", 2, 10, 10, 0.1, 0.05, 0.01, root);
        ButtonsUtil noBtn = new ButtonsUtil("NO", "#00b5d4", "black", "#00b5d4", "#00b5d4", 2, 10, 10, 0.1, 0.05, 0.01, root);

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

    private void setupBackground() {
        ImageView background = new ImageView(new Image(getClass().getResource("/background.png").toExternalForm()));
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());
        background.setPreserveRatio(false);
        root.getChildren().add(background);
    }

    public StackPane getRoot() {return root;}
}