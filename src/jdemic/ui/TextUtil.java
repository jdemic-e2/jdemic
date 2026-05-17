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
     * @param text       The text content to display inside the label
     * @param fontName   The name/key of the font to be used (handled by FontUtil)
     * @param sizeRatio  The ratio used to calculate font size based on the root width
     * @param colorHex   The text color in HEX format (e.g. "#FFFFFF")
     * @param root       The root container whose width is used for responsive scaling
     * @return           A styled Label with responsive font size
     */
    public static Label createText(String text, String fontName, double sizeRatio, String colorHex, Region root) {
        //Create label with given text
        Label label = new Label(text);
        //Set text color using HEX value
        label.setTextFill(Color.web(colorHex));
        //Bind the font size dynamically to the width of the root container
        //This makes the text responsive when the window is resized.
        label.fontProperty().bind(Bindings.createObjectBinding(() -> FontUtil.getFont(fontName, root.getWidth() * sizeRatio), root.widthProperty()));
        return label;
    }
}