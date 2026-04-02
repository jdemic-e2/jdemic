package jdemic.Scenes.Tutorial;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import jdemic.Scenes.MainMenuScene;
import jdemic.ui.*;

public class TutorialUtil {

    public static void setBackground(StackPane root) {
        ImageView background = new ImageView(new Image(TutorialUtil.class.getResource("/background.png").toExternalForm()));
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());
        background.setPreserveRatio(false);
        root.getChildren().add(background);
    }

    public static void addBottomButtons(
            StackPane root,
            Region reference,
            Stage stage,
            Runnable onBack,
            Runnable onNext
    ) {
        ButtonsUtil exitBtn = new ButtonsUtil(
                "EXIT", "#ff0000", "black", "#ff0000", "#ff0000",
                2, 15, 15,
                0.15, 0.07, 0.02,
                root
        );

        ButtonsUtil nextBtn = new ButtonsUtil(
                "NEXT", "#00b5d4", "black", "#00b5d4", "#00b5d4",
                2, 15, 15,
                0.15, 0.07, 0.02,
                root
        );
        ButtonsUtil backBtn = new ButtonsUtil(
                "BACK", "#888888", "black", "#888888", "#888888",
                2, 15, 15,
                0.15, 0.07, 0.02,
                root
        );

        StackPane.setAlignment(exitBtn, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(nextBtn, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(backBtn, Pos.BOTTOM_RIGHT);

        exitBtn.translateXProperty().bind(root.widthProperty().multiply(0.075));
        nextBtn.translateXProperty().bind(root.widthProperty().multiply(-0.075));
        backBtn.translateXProperty().bind(root.widthProperty().multiply(-0.25));

        exitBtn.translateYProperty().bind(root.heightProperty().multiply(-0.08));
        nextBtn.translateYProperty().bind(root.heightProperty().multiply(-0.08));
        backBtn.translateYProperty().bind(root.heightProperty().multiply(-0.08));

        exitBtn.setOnMouseClicked(e -> {
            ConfirmationOverlay overlay = new ConfirmationOverlay(
                    root,
                    "ARE YOU SURE YOU WANT TO EXIT?",
                    "YES",
                    "NO",
                    () -> stage.getScene().setRoot(new MainMenuScene(stage).getRoot()),
                    null
            );
            root.getChildren().add(overlay.getRoot());
        });

        nextBtn.setOnMouseClicked(e -> {
            if (onNext != null) onNext.run();
        });
        backBtn.setOnMouseClicked(e -> {
            if (onBack != null) onBack.run();
        });

        root.getChildren().addAll(exitBtn, nextBtn, backBtn);
    }

    public static StackPane createTutorialPanel(StackPane root) {
        StackPane tutorialPanel = PanelUtil.createPanel(0.85, 0.60, "#00b5d4", 2, 15, 0, root);
        StackPane.setAlignment(tutorialPanel, Pos.CENTER);
        tutorialPanel.setStyle( "-fx-background-color: black;" + "-fx-border-color: #ffffff;" + "-fx-border-width: 2;");
        GlowUtil.applyGlow(tutorialPanel, "#00b5d4", 25);
        tutorialPanel.prefWidthProperty().bind(root.widthProperty().multiply(0.85));
        tutorialPanel.prefHeightProperty().bind(root.heightProperty().multiply(0.60));
        tutorialPanel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        return tutorialPanel;
    }

    public static Label createTutorialTitle(
            StackPane root,
            String text,
            double size,
            String color,
            Pos alignment,
            double offsetX,
            double offsetY,
            String glowColor,
            double glowRadius
    ) {
        Label title = TextUtil.createText(text, "hkmodular", size, color, root);
        title.setTextAlignment(alignment == Pos.TOP_CENTER ? TextAlignment.CENTER : TextAlignment.LEFT);
        if (glowColor != null) {GlowUtil.applyGlow(title, glowColor, glowRadius);}
        title.translateXProperty().bind(root.widthProperty().multiply(offsetX));
        title.translateYProperty().bind(root.heightProperty().multiply(offsetY));
        StackPane.setAlignment(title, alignment);
        root.getChildren().add(title);
        return title;
    }

    public static VBox createLine(Region parent, String glowColor, double glowRadius) {
        Rectangle line = GlowLineUtil.createGlowLine(0.85, parent);
        if (glowColor != null) {GlowUtil.applyGlow(line, glowColor, glowRadius);}
        line.setHeight(3);
        VBox box = new VBox(line);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }
    public static VBox createRow(
            StackPane root,
            String left,
            String right,
            String leftColor,
            String rightColor,
            double fontSize
    ) {
        Label leftLabel = TextUtil.createText(left, "neotechprobold", fontSize, leftColor, root);
        Label rightLabel = TextUtil.createText(right, "neotechprobold", fontSize, rightColor, root);

        rightLabel.setWrapText(true);
        rightLabel.setTextOverrun(OverrunStyle.CLIP);
        rightLabel.setMinHeight(Region.USE_PREF_SIZE);
        rightLabel.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(rightLabel, Priority.ALWAYS);
        HBox row = new HBox(leftLabel, rightLabel);

        row.spacingProperty().bind(root.widthProperty().multiply(0.05));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        leftLabel.minWidthProperty().bind(root.widthProperty().multiply(0.18));

        return new VBox(row);
    }
}
