package jdemic.ui;

import javafx.beans.binding.Bindings;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/**
 * Small helper to centralize image loading and common responsive bindings.
 */
public class UIImageUtil {
    public static Image load(String path) {
        return SafeResourceLoader.loadImage(path);
    }

    public static ImageView createResponsiveWidthImageView(StackPane root, Image image, double min, double max, double factor) {
        if (image == null) return new ImageView();
        ImageView iv = new ImageView(image);
        iv.setPreserveRatio(true);
        iv.fitWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(min, Math.min(max, root.getWidth() * factor)),
                root.widthProperty()
        ));
        return iv;
    }

    public static ImageView createSquareIcon(StackPane root, Image image, double factor) {
        if (image == null) return new ImageView();
        ImageView iv = new ImageView(image);
        iv.setPreserveRatio(true);
        iv.fitWidthProperty().bind(root.widthProperty().multiply(factor));
        iv.fitHeightProperty().bind(root.widthProperty().multiply(factor));
        return iv;
    }

    public static ImageView loadResponsive(StackPane root, String path, double min, double max, double factor) {
        return createResponsiveWidthImageView(root, load(path), min, max, factor);
    }

    public static ImageView loadSquare(StackPane root, String path, double factor) {
        return createSquareIcon(root, load(path), factor);
    }
}
