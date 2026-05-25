package jdemic.Scenes;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;
import jdemic.ui.SafeResourceLoader;
import jdemic.ui.GlowUtil;
import jdemic.ui.TextUtil;

import java.net.URL;

public class SceneUtil {
    private static final String BACKGROUND_RESOURCE = "/background.png";

    public static void setBackground(StackPane root) {
        setBackground(root, BACKGROUND_RESOURCE);
    }

    public static void setBackground(StackPane root, String resourcePath) {
        URL bgUrl = SceneUtil.class.getResource(resourcePath);
        if (bgUrl == null) {
            System.err.println("[SceneUtil] Missing resource: " + resourcePath);
            return;
        }
        ImageView background = new ImageView(SafeResourceLoader.loadImage(bgUrl));
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());
        background.setPreserveRatio(false);
        root.getChildren().add(0, background);
        background.toBack();
    }

    public static Label createSceneTitle(StackPane root, String text, double size, String color, Pos alignment, double offsetX, double offsetY, String glowColor, double glowRadius) {
        Label title = TextUtil.createText(text, "hkmodular", size, color, root);
        title.setTextAlignment(alignment == Pos.TOP_CENTER ? TextAlignment.CENTER : TextAlignment.LEFT);
        if (glowColor != null) { GlowUtil.applyGlow(title, glowColor, glowRadius); }
        title.translateXProperty().bind(root.widthProperty().multiply(offsetX));
        title.translateYProperty().bind(root.heightProperty().multiply(offsetY));
        StackPane.setAlignment(title, alignment);
        root.getChildren().add(title);
        return title;
    }

    public static Label createErrorLabel(StackPane root) {
        Label errorLabel = TextUtil.createText("", "hkmodular", 0.025, "#ff0000", root);
        errorLabel.setVisible(false);
        return errorLabel;
    }
}
