package jdemic.ui;

import javafx.animation.Animation;
import jdemic.Scenes.Settings.SettingsManager;

public final class AnimationSpeedUtil {
    private AnimationSpeedUtil() {
    }

    public static double rate() {
        String speed = SettingsManager.getInstance().animationSpeedProperty().get();
        if ("SLOW".equalsIgnoreCase(speed)) {
            return 0.65;
        }
        if ("FAST".equalsIgnoreCase(speed)) {
            return 1.6;
        }
        return 1.0;
    }

    public static void play(Animation animation) {
        if (animation == null) {
            return;
        }
        animation.setRate(rate());
        animation.play();
    }
}
