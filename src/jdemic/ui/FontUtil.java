package jdemic.ui;

import javafx.scene.text.Font;

public class FontUtil {
    public static Font getFont(String fontName, double size) {
        return Font.loadFont(FontUtil.class.getResource("/fonts/" + fontName + ".otf").toExternalForm(), size);
    }
}
