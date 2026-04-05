package jdemic.Scenes;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import jdemic.Scenes.Lobby.LobbyScene;
import jdemic.Scenes.Settings.SettingsScene;
import jdemic.Scenes.Tutorial.TutorialRulesScene;
import jdemic.ui.Animations;
import jdemic.ui.ButtonsUtil;
import jdemic.ui.DotUtil;
import jdemic.ui.GlowLineUtil;
import jdemic.ui.GlowUtil;
import jdemic.ui.PanelUtil;
import jdemic.ui.TextUtil;

public class MainMenuScene {

    private StackPane root;
    private Stage stage;
    public MainMenuScene(Stage stage) {
        this.stage = stage;
        root = new StackPane();
        setupBackground();
        setupUI();
    }

    private void setupBackground() {
        ImageView background = new ImageView(new Image(getClass().getResource("/background.png").toExternalForm()));
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());
        background.setPreserveRatio(false);
        root.getChildren().add(background);
    }

    private void setupUI() {

        Label title = TextUtil.createText("CYBER\nCRISIS", "hkmodular", 0.05, "#cfc900", root);
        title.setTextAlignment(TextAlignment.CENTER);
        GlowUtil.applyGlow(title, "#000000", 10);
        title.translateYProperty().bind(root.heightProperty().multiply(0.06));
        StackPane.setAlignment(title, Pos.CENTER);
        Animations.createPulseAnimation(title, 1.05, 2);


        ImageView titleBox = new ImageView(new Image(getClass().getResource("/elements/glowTitleFrame.png").toExternalForm()));
        titleBox.setPreserveRatio(true);
        titleBox.fitWidthProperty().bind(root.widthProperty().multiply(0.45));
        titleBox.translateYProperty().bind(root.heightProperty().multiply(0.06));
        StackPane.setAlignment(titleBox, Pos.CENTER);
        GlowUtil.applyGlow(titleBox, "#000000", 110);

        String text = "S Y S T E M   O N L I N E   •   A W A I T I N G   C O M M A N D   •   P R E S S   P L A Y   T O   B E G I N   •";
        Pane scrollingText = Animations.createScrollingText(root, text,60);

       StackPane.setAlignment(scrollingText, Pos.BOTTOM_LEFT);
       scrollingText.translateYProperty().bind(root.heightProperty().multiply(0.85));

        root.getChildren().add(scrollingText);

        Image mapImg = new Image(getClass().getResource("/backgroundMap.png").toExternalForm());
        ImageView map = new ImageView(mapImg);
        map.setPreserveRatio(true);
        map.fitWidthProperty().bind(root.widthProperty().multiply(0.8));
        StackPane.setAlignment(map, Pos.TOP_CENTER);
        map.translateYProperty().bind(root.heightProperty().multiply(0.05));
        map.translateXProperty().bind(root.widthProperty().multiply(-0.05));

      //  Animations.createPulse(map, 1.05, 2);

        VBox rightPanel = new VBox();
        rightPanel.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(root.getHeight() * 0.08, root.getWidth() * 0.02, 0, -root.getWidth() * 0.01), root.widthProperty(), root.heightProperty()));
        rightPanel.setSpacing(0);
        rightPanel.spacingProperty().bind(root.heightProperty().multiply(0.01));
        rightPanel.maxWidthProperty().bind(root.widthProperty().multiply(0.25));
        rightPanel.setAlignment(Pos.TOP_CENTER);

        StackPane statusPanelBox = PanelUtil.createPanel(0.10, 0.20, "#00b5d4", 2, 15, 0, root);
        statusPanelBox.setStyle("-fx-background-color: black;" + "-fx-border-color: #ffffff;" + "-fx-border-width: 2;");
        GlowUtil.applyGlow(statusPanelBox, "#00b5d4", 20);
        statusPanelBox.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(root.getHeight() * 0.02, root.getWidth() * 0.015, root.getHeight() * 0.03, root.getWidth() * 0.02), root.widthProperty(), root.heightProperty()));
        VBox.setMargin(statusPanelBox, new Insets(0, 0, -root.getHeight() * 0.015, 0));

        Label statusPanel = TextUtil.createText("NETWORK STATUS:", "hkmodular", 0.01, "#00b5d4", root);
        Label status = TextUtil.createText("CRITICAL", "hkmodular", 0.015, "#ff0000", root);
        status.translateYProperty().bind(root.heightProperty().multiply(-0.005));

        Rectangle statusLine = GlowLineUtil.createGlowLine(0.7, rightPanel);
        GlowUtil.applyGlow(statusLine, "#00b5d4", 10);
        VBox lineBox = new VBox(statusLine);
        lineBox.setAlignment(Pos.CENTER);
        lineBox.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(0, 0, root.getHeight() * 0.01, 0), root.heightProperty()));

        Label threats = TextUtil.createText("ACTIVE THREATS:", "hkmodular", 0.01, "#00b5d4", root);
        Label threatsNumber = TextUtil.createText("4", "hkmodular", 0.015, "#ff0000", root);
        threats.translateYProperty().bind(root.heightProperty().multiply(-0.01));
        threatsNumber.translateYProperty().bind(root.heightProperty().multiply(-0.005));

        VBox threatsGroup = new VBox(threats, threatsNumber);
        threatsGroup.spacingProperty().bind(root.heightProperty().multiply(0.003));
        threatsGroup.setAlignment(Pos.CENTER);

        Label regions = TextUtil.createText("REGIONS AFFECTED:", "hkmodular", 0.01, "#00b5d4", root);
        Label regionsNumber = TextUtil.createText("13", "hkmodular", 0.015, "#ff0000", root);
        regions.translateYProperty().bind(root.heightProperty().multiply(-0.01));
        regionsNumber.translateYProperty().bind(root.heightProperty().multiply(-0.005));

        VBox regionsGroup = new VBox(regions, regionsNumber);
        regionsGroup.spacingProperty().bind(root.heightProperty().multiply(0.003));
        regionsGroup.setAlignment(Pos.CENTER);

        VBox statusGroup = new VBox(statusPanel, status, lineBox, threatsGroup, regionsGroup);
        statusGroup.spacingProperty().bind(root.heightProperty().multiply(0.01));
        statusGroup.setAlignment(Pos.CENTER);

        statusPanelBox.getChildren().add(statusGroup);

        StackPane virusPanelBox = PanelUtil.createPanel(0.10, 0.20, "#00b5d4", 2, 15, 0, root);
        virusPanelBox.setStyle("-fx-background-color: black;" + "-fx-border-color: #ffffff;" + "-fx-border-width: 2;");
        GlowUtil.applyGlow(virusPanelBox, "#00b5d4", 20);
        virusPanelBox.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(root.getHeight() * 0.02, root.getWidth() * 0.015, root.getHeight() * 0.02, root.getWidth() * 0.015), root.widthProperty(), root.heightProperty()));
        virusPanelBox.translateYProperty().bind(root.heightProperty().multiply(-0.02));

        Label virusText = TextUtil.createText("VIRUSES DETECTED:", "hkmodular", 0.01, "#00b5d4", root);

        Rectangle virusLine = GlowLineUtil.createGlowLine(0.7, rightPanel);
        GlowUtil.applyGlow(virusLine, "#00b5d4", 10);
        VBox virusLineBox = new VBox(virusLine);
        virusLineBox.setAlignment(Pos.CENTER);
        virusLineBox.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(root.getHeight() * 0.006, 0, 0, 0), root.heightProperty()));

        VBox virusTextBox = new VBox(virusText, virusLineBox);
        virusTextBox.spacingProperty().bind(root.heightProperty().multiply(0.003));
        virusTextBox.setAlignment(Pos.CENTER);

        HBox virusConficker = new HBox(DotUtil.createDot(0.010, "#ff0000", 24, root), TextUtil.createText("CONFICKER", "hkmodular", 0.01, "#00b5d4", root));
        HBox virusWannacry = new HBox(DotUtil.createDot(0.010, "#ff0000", 24, root),TextUtil.createText("WANNACRY", "hkmodular", 0.01, "#00b5d4", root));
        HBox virusStuxnet = new HBox( DotUtil.createDot(0.010, "#ff0000", 24, root),TextUtil.createText("STUXNET", "hkmodular", 0.01, "#00b5d4", root));
        HBox virusZeus = new HBox(DotUtil.createDot(0.010, "#ff0000", 24, root), TextUtil.createText("ZEUS", "hkmodular", 0.01, "#00b5d4", root));

        for (HBox row : new HBox[]{virusConficker, virusWannacry, virusStuxnet, virusZeus}) {
            row.spacingProperty().bind(root.widthProperty().multiply(0.015));
            row.setAlignment(Pos.CENTER_LEFT);
        }

        VBox virusGroup = new VBox(virusConficker, virusWannacry, virusStuxnet, virusZeus);
        virusGroup.spacingProperty().bind(root.heightProperty().multiply(0.008));

        virusGroup.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(root.getHeight() * 0.01, 0, 0, root.getWidth() * 0.03), root.widthProperty(), root.heightProperty()));
        virusGroup.setAlignment(Pos.TOP_LEFT);

        VBox virusContainer = new VBox(virusTextBox, virusGroup);
        virusContainer.spacingProperty().bind(root.heightProperty().multiply(0.01));
        virusContainer.setAlignment(Pos.TOP_CENTER);
        virusPanelBox.getChildren().add(virusContainer);

        rightPanel.translateXProperty().bind(root.widthProperty().multiply(0.43));
        rightPanel.translateYProperty().bind(root.heightProperty().multiply(0.02));
        rightPanel.getChildren().addAll(statusPanelBox, virusPanelBox);

        root.getChildren().add(titleBox);
        root.getChildren().add(map);
        root.getChildren().add(title);
        root.getChildren().add(rightPanel);

        ButtonsUtil playBtn = new ButtonsUtil("PLAY", "#00b5d4", "black", "#00b5d4", "#00b5d4", 2, 15, 15, 0.18, 0.08, 0.02, root);
        ButtonsUtil tutorialBtn = new ButtonsUtil("TUTORIAL", "#00b5d4", "black", "#00b5d4", "#00b5d4", 2, 15, 15, 0.18, 0.08, 0.02, root);
        ButtonsUtil settingsBtn = new ButtonsUtil("SETTINGS", "#00b5d4", "black", "#00b5d4", "#00b5d4", 2, 15, 15, 0.18, 0.08, 0.02, root);
        ButtonsUtil exitBtn = new ButtonsUtil("EXIT", "#00b5d4", "black", "#00b5d4", "#00b5d4", 2, 15, 15, 0.18, 0.08, 0.02, root);

        tutorialBtn.setOnMouseClicked(e -> {
            System.out.println("CLICKED");

            TutorialRulesScene tutorial = new TutorialRulesScene(stage);
            stage.getScene().setRoot(new TutorialRulesScene(stage).getRoot());
        });
    
        playBtn.setPickOnBounds(true);

        playBtn.setOnMouseClicked(e -> {
            System.out.println("[DEBUG] PLAY clicked");
            stage.getScene().setRoot(new LobbyScene(stage).getRoot());
        });

        settingsBtn.setOnMouseClicked(e -> {
            SettingsScene settings = new SettingsScene(stage);
            stage.getScene().setRoot(settings.getRoot());
        });

        exitBtn.setOnMouseClicked(e -> {
            StackPane confirmOverlay = new StackPane();
            confirmOverlay.setStyle("-fx-background-color: rgba(5, 10, 20, 0.8);");
            confirmOverlay.prefWidthProperty().bind(root.widthProperty());
            confirmOverlay.prefHeightProperty().bind(root.heightProperty());

            VBox dialog = new VBox(20); // 20px spacing between elements
            dialog.setAlignment(Pos.CENTER);

            dialog.maxWidthProperty().bind(root.widthProperty().multiply(0.40));
            dialog.maxHeightProperty().bind(root.heightProperty().multiply(0.30));

            dialog.setStyle(
                    "-fx-background-color: rgba(10, 20, 40, 0.95);" +
                            "-fx-border-color: #00d4ff;" +
                            "-fx-border-width: 2;" +
                            "-fx-background-radius: 15;" +
                            "-fx-border-radius: 15;" +
                            "-fx-effect: dropshadow(gaussian, #00d4ff, 25, 0, 0, 0);"
            );

            dialog.paddingProperty().bind(Bindings.createObjectBinding(() ->
                    new Insets(root.getHeight() * 0.04), root.heightProperty())
            );

            Label warningTitle = TextUtil.createText("EXIT CONFIRMATION", "hkmodular", 0.02, "#00d4ff", root);
            warningTitle.setTextAlignment(TextAlignment.CENTER);

            Label warningText = TextUtil.createText("Do you really want to exit the game?", "hkmodular", 0.01, "#ffffff", root);
            warningText.setTextAlignment(TextAlignment.CENTER);

            ButtonsUtil yesBtn = new ButtonsUtil("YES", "#00d4ff", "black", "#00d4ff", "#00d4ff", 2, 12, 12, 0.14, 0.06, 0.015, root);
            ButtonsUtil noBtn = new ButtonsUtil("NO", "#ff274c", "black", "#ff274c", "#ff274c", 2, 12, 12, 0.14, 0.06, 0.015, root);

            yesBtn.setOnMouseClicked(ev -> System.exit(0));
            noBtn.setOnMouseClicked(ev -> root.getChildren().remove(confirmOverlay));

            HBox buttonContainer = new HBox(30); // 30px spacing between YES and NO
            buttonContainer.setAlignment(Pos.CENTER);
            buttonContainer.getChildren().addAll(yesBtn, noBtn);

            dialog.getChildren().addAll(warningTitle, warningText, buttonContainer);
            confirmOverlay.getChildren().add(dialog);

            root.getChildren().add(confirmOverlay);
        });

        VBox menuBox = new VBox();
        menuBox.setFillWidth(false);
        menuBox.setAlignment(Pos.BOTTOM_CENTER);
        menuBox.spacingProperty().bind(root.heightProperty().multiply(0.025));
        menuBox.getChildren().addAll(playBtn, tutorialBtn, settingsBtn, exitBtn);
        menuBox.translateXProperty().bind(root.widthProperty().multiply(-0.35));
        menuBox.translateYProperty().bind(root.heightProperty().multiply(-0.25));

        root.getChildren().add(menuBox);
    }
     public StackPane getRoot() {return root;}
}