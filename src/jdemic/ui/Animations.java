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
    public static Pane createScrollingText(StackPane root, String text) {

        Label subTitle1 = TextUtil.createText(text, "hkmodular", 0.015, "#00b5d4", root);
        Label subTitle2 = TextUtil.createText(text, "hkmodular", 0.015, "#00b5d4", root);
        Pane marqueePane = new Pane(subTitle1, subTitle2);
        StackPane.setAlignment(marqueePane, Pos.BOTTOM_LEFT);
        StackPane.setMargin(marqueePane, new Insets(620, 0, 80, 0));
        marqueePane.setPickOnBounds(false);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(marqueePane.widthProperty());
        clip.heightProperty().bind(marqueePane.heightProperty());
        marqueePane.setClip(clip);
        double gapBetweenTexts = 30;
        subTitle1.layoutBoundsProperty().addListener((obs, oldVal, bounds) -> {
            subTitle1.setLayoutX(0);
            subTitle2.setLayoutX(bounds.getWidth() + gapBetweenTexts);
        });
        double scrollSpeed = 100;
        AnimationTimer scrollTimer = new AnimationTimer() {
            long lastTime = 0;
            @Override
            public void handle(long now) {
                if (lastTime == 0) {lastTime = now;return;}
                double delta = (now - lastTime) / 1e9;
                lastTime = now;
                subTitle1.setLayoutX(subTitle1.getLayoutX() - scrollSpeed * delta);
                subTitle2.setLayoutX(subTitle2.getLayoutX() - scrollSpeed * delta);
                if (subTitle1.getLayoutX() + subTitle1.getWidth() < 0) {
                    subTitle1.setLayoutX(subTitle2.getLayoutX() + subTitle2.getWidth() + gapBetweenTexts);
                }
                if (subTitle2.getLayoutX() + subTitle2.getWidth() < 0) {
                    subTitle2.setLayoutX(subTitle1.getLayoutX() + subTitle1.getWidth() + gapBetweenTexts);
                }
            }
        };
        scrollTimer.start();
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
