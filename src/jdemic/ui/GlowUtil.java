package jdemic.ui;

import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

public class GlowUtil {

        public static void applyGlow(Node node, String colorHex, double radius) {

            Color base = Color.web(colorHex);
            DropShadow outerGlow = new DropShadow();
            outerGlow.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 0.3));
            outerGlow.setRadius(radius * 2.5);
            DropShadow midGlow = new DropShadow();
            midGlow.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 0.6));
            midGlow.setRadius(radius * 1.2);
            midGlow.setInput(outerGlow);

            DropShadow innerGlow = new DropShadow();
            innerGlow.setColor(base);
            innerGlow.setRadius(radius * 0.4);
            innerGlow.setInput(midGlow);
            node.setEffect(innerGlow);
        }
            public static DropShadow createGlow(String colorHex, double radius) {
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(colorHex));
        glow.setRadius(radius);
        glow.setSpread(0.5);
        return glow;
    }
    }