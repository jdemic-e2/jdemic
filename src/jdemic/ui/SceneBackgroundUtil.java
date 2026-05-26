package jdemic.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public final class SceneBackgroundUtil {
    public static final String MENU_BACKGROUND = "/background.png";
    public static final String GAME_BACKGROUND = "/bgGame.png";
    public static final String MAP_BACKGROUND = "/backgroundMap.png";

    private SceneBackgroundUtil() {
    }

    public static ImageView addCoverBackground(StackPane root, String resourcePath) {
        Image image = SafeResourceLoader.loadImage(resourcePath);
        if (image == null) {
            return null;
        }

        ImageView background = new ImageView(image);
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());
        background.setPreserveRatio(false);
        root.getChildren().add(background);
        return background;
    }
}
