package jdemic.ui;

import javafx.animation.ParallelTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public final class AnimationUiUtil {
    private AnimationUiUtil() {
    }

    public static ParallelTransition createMoveWithPulse(
            Node node,
            Duration duration,
            double fromX,
            double fromY,
            double toX,
            double toY,
            double pulseScale,
            Duration pulseDuration,
            Interpolator interpolator
    ) {
        node.setTranslateX(fromX);
        node.setTranslateY(fromY);

        TranslateTransition move = new TranslateTransition(duration, node);
        move.setToX(toX);
        move.setToY(toY);
        move.setInterpolator(interpolator);

        ScaleTransition pulse = new ScaleTransition(pulseDuration, node);
        pulse.setToX(pulseScale);
        pulse.setToY(pulseScale);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);

        return new ParallelTransition(move, pulse);
    }
}
