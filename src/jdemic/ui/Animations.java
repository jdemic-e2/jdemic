package jdemic.ui;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class Animations {
    public static Pane createScrollingText(StackPane root, String text, int scrollSpeed) {
        Label subTitle1 = TextUtil.createText(text, "hkmodular", 0.015, "#00b5d4", root);
        Label subTitle2 = TextUtil.createText(text, "hkmodular", 0.015, "#00b5d4", root);

        Pane marqueePane = new Pane(subTitle1, subTitle2);

        marqueePane.prefHeightProperty().bind(subTitle1.heightProperty());
        marqueePane.maxWidthProperty().bind(root.widthProperty());
        marqueePane.setPickOnBounds(false);
        marqueePane.setMouseTransparent(true);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(root.widthProperty());
        clip.heightProperty().bind(marqueePane.heightProperty());
        marqueePane.setClip(clip);

        double gapBetweenTexts = 50;

        subTitle1.widthProperty().addListener((obs, oldVal, newVal) -> {
            subTitle2.setLayoutX(newVal.doubleValue() + gapBetweenTexts);
        });

        AnimationTimer scrollTimer = new AnimationTimer() {
            private long lastTime = 0;

            @Override
            public void handle(long now) {
                if (lastTime == 0) { lastTime = now; return; }
                double delta = (now - lastTime) / 1e9;
                lastTime = now;

                double s1X = subTitle1.getLayoutX() - scrollSpeed * delta;
                double s2X = subTitle2.getLayoutX() - scrollSpeed * delta;

                // Loop logic
                if (s1X + subTitle1.getWidth() < 0) {
                    s1X = subTitle2.getLayoutX() + subTitle2.getWidth() + gapBetweenTexts;
                }
                if (s2X + subTitle2.getWidth() < 0) {
                    s2X = subTitle1.getLayoutX() + subTitle1.getWidth() + gapBetweenTexts;
                }

                subTitle1.setLayoutX(s1X);
                subTitle2.setLayoutX(s2X);
            }
        };
        marqueePane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                scrollTimer.start();
            }
            else {
                scrollTimer.stop();
            }
        });
        return marqueePane;
    }

    public static ScaleTransition createPulseAnimation(Node node, double scale, double durationSeconds) {
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(durationSeconds), node);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(scale);
        pulse.setToY(scale);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
        return pulse;
    }
}
