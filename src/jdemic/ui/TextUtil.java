package jdemic.ui;

import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Utility class for creating styled and responsive text labels.
 */
public class TextUtil {
    /**
     * Creates a JavaFX Label with dynamic font sizing and custom styling.
     * Adjusted to use Height for more stable scaling in landscape mode.
     * * @param text       The text content to display inside the label
     * @param fontName   The name/key of the font to be used
     * @param sizeRatio  The ratio used to calculate font size based on the root height
     * @param colorHex   The text color in HEX format
     * @param root       The root container used for responsive scaling
     * @return           A styled Label with height-responsive font size
     */
    public static Label createText(String text, String fontName, double sizeRatio, String colorHex, Region root) {
        Label label = new Label(text);
        label.setTextFill(Color.web(colorHex));

        // Binding to heightProperty instead of widthProperty for better proportions
        label.fontProperty().bind(Bindings.createObjectBinding(
                () -> FontUtil.getFont(fontName, root.getHeight() * sizeRatio),
                root.heightProperty()
        ));

        return label;
    }
}