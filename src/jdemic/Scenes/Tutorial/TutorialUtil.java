package jdemic.Scenes.Tutorial;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import jdemic.Scenes.SceneManager.SceneManager;
import jdemic.ui.*;

import java.net.URL;

public class TutorialUtil {

    private static final String PANEL_STYLE = "-fx-background-color: black;"
            + "-fx-border-color: #00b5d4;"
            + "-fx-border-width: 2;"
            + "-fx-background-radius: 6;"
            + "-fx-border-radius: 6;";

    private static final String CARD_WRAPPER_STYLE = "-fx-background-color: black;"
            + "-fx-border-color: #00b5d4;"
            + "-fx-border-width: 2;"
            + "-fx-background-radius: 20;"
            + "-fx-border-radius: 20;";

    private static final String CLOSE_BUTTON_STYLE = "-fx-background-color: #ff2d2d;"
            + "-fx-text-fill: white;"
            + "-fx-font-weight: bold;"
            + "-fx-background-radius: 50;"
            + "-fx-min-width: 30;"
            + "-fx-min-height: 30;"
            + "-fx-max-width: 30;"
            + "-fx-max-height: 30;";

    public static void setBackground(StackPane root) {
        java.net.URL bgUrl = TutorialUtil.class.getResource("/background.png");
        if (bgUrl == null) {
            System.err.println("[TutorialUtil] Missing resource: /background.png");
            return;
        }
        ImageView background = new ImageView(new Image(bgUrl.toExternalForm()));
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
                    () -> {
                        SceneManager.clearCache();
                        SceneManager.switchScene("MAIN_MENU");
                    },
                    null
            );
            root.getChildren().add(overlay.getRoot());
        });

        backBtn.setOnMouseClicked(e -> onBack.run());
        nextBtn.setOnMouseClicked(e -> onNext.run());

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

    public static StackPane createInstructionPanel(
            StackPane root,
            String text,
            double fontSize
    ) {
        StackPane panel = new StackPane();
        panel.prefWidthProperty().bind(root.widthProperty().multiply(0.18));
        panel.prefHeightProperty().bind(root.heightProperty().multiply(0.18));
        panel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        panel.setStyle(PANEL_STYLE);
        GlowUtil.applyGlow(panel, "#00b5d4", 15);

        Label panelText = TextUtil.createText(text, "hkmodular", fontSize, "#00b5d4", root);
        panelText.setWrapText(true);
        panelText.setAlignment(Pos.CENTER);
        panelText.setTextAlignment(TextAlignment.CENTER);
        panelText.maxWidthProperty().bind(panel.widthProperty().multiply(0.75));

        StackPane panelContent = new StackPane(panelText);
        panelContent.paddingProperty().bind(Bindings.createObjectBinding(() ->
                        new Insets(
                                panel.getHeight() * 0.15,
                                panel.getWidth() * 0.1,
                                panel.getHeight() * 0.15,
                                panel.getWidth() * 0.1
                        ),
                panel.widthProperty(),
                panel.heightProperty()
        ));
        panel.getChildren().add(panelContent);
        return panel;
    }

    public static ImageView createClickableCard(
            StackPane root,
            String resourcePath,
            String errorContext,
            double widthRatio,
            double hoverScale,
            Runnable onClick
    ) {
        URL cardUrl = TutorialUtil.class.getResource(resourcePath);
        if (cardUrl == null) {
            System.err.println("[" + errorContext + "] Missing resource: " + resourcePath);
            return new ImageView();
        }

        ImageView card = new ImageView(new Image(cardUrl.toExternalForm()));
        card.setPreserveRatio(true);
        card.fitWidthProperty().bind(root.widthProperty().multiply(widthRatio));
        GlowUtil.applyGlow(card, "#00b5d4", 10);

        card.setOnMouseEntered(e -> {
            card.setScaleX(hoverScale);
            card.setScaleY(hoverScale);
        });
        card.setOnMouseExited(e -> {
            card.setScaleX(1);
            card.setScaleY(1);
        });
        card.setOnMouseClicked(e -> onClick.run());
        return card;
    }

    public static void openCardOverlay(
            StackPane root,
            String resourcePath,
            String fallbackText,
            String errorContext
    ) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.7);");

        URL cardUrl = TutorialUtil.class.getResource(resourcePath);
        if (cardUrl == null) {
            System.err.println("[" + errorContext + "] Missing resource: " + resourcePath);
            Label fallback = new Label(fallbackText);
            fallback.setStyle("-fx-text-fill: white; -fx-font-size: 24;");
            overlay.getChildren().add(fallback);
            root.getChildren().add(overlay);
            return;
        }

        ImageView card = new ImageView(new Image(cardUrl.toExternalForm()));
        card.setPreserveRatio(true);
        card.fitWidthProperty().bind(root.widthProperty().multiply(0.3));

        Rectangle clip = new Rectangle();
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        clip.widthProperty().bind(card.layoutBoundsProperty().map(bounds -> bounds.getWidth()));
        clip.heightProperty().bind(card.layoutBoundsProperty().map(bounds -> bounds.getHeight()));
        card.setClip(clip);

        StackPane cardWrapper = new StackPane(card);
        cardWrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        cardWrapper.prefWidthProperty().bind(card.fitWidthProperty());
        cardWrapper.prefHeightProperty().bind(card.fitHeightProperty());
        cardWrapper.setStyle(CARD_WRAPPER_STYLE);
        GlowUtil.applyGlow(cardWrapper, "#00b5d4", 20);

        Button close = new Button("X");
        close.setStyle(CLOSE_BUTTON_STYLE);
        close.setOnAction(e -> closeOverlay(root, overlay, cardWrapper));

        cardWrapper.getChildren().add(close);
        StackPane.setAlignment(close, Pos.TOP_RIGHT);
        close.setTranslateX(8);
        close.setTranslateY(-8);

        overlay.getChildren().add(cardWrapper);
        root.getChildren().add(overlay);
        playOverlayIn(cardWrapper);
    }

    public static void enableHorizontalDragScroll(Region viewport, Pane content) {
        DoubleProperty scrollX = new SimpleDoubleProperty(0);
        content.translateXProperty().bind(scrollX);

        final double[] lastX = {0};
        viewport.setOnMousePressed(e -> lastX[0] = e.getSceneX());
        viewport.setOnMouseDragged(e -> {
            double delta = e.getSceneX() - lastX[0];
            lastX[0] = e.getSceneX();

            double contentWidth = content.getBoundsInLocal().getWidth();
            double viewportWidth = viewport.getWidth();
            double minScroll = Math.min(0, viewportWidth - contentWidth);
            double target = scrollX.get() + delta;

            scrollX.set(Math.max(minScroll, Math.min(0, target)));
        });
    }

    public static void clipToBounds(Region region) {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(region.widthProperty());
        clip.heightProperty().bind(region.heightProperty());
        region.setClip(clip);
    }

    private static void playOverlayIn(Node node) {
        node.setScaleX(0.7);
        node.setScaleY(0.7);
        node.setOpacity(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(250), node);
        scale.setToX(1);
        scale.setToY(1);

        FadeTransition fade = new FadeTransition(Duration.millis(250), node);
        fade.setToValue(1);

        new ParallelTransition(scale, fade).play();
    }

    private static void closeOverlay(StackPane root, StackPane overlay, Node animatedNode) {
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(150), animatedNode);
        scaleOut.setToX(0.7);
        scaleOut.setToY(0.7);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), animatedNode);
        fadeOut.setToValue(0);

        ParallelTransition out = new ParallelTransition(scaleOut, fadeOut);
        out.setOnFinished(ev -> root.getChildren().remove(overlay));
        out.play();
    }
}
