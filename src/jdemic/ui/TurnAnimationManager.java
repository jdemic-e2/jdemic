package jdemic.ui;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import jdemic.GameLogic.DiseaseColor;


//Class responsible for the following animations:
//- Turn start/end
//- Action consumed
//- Infection phase intro
//- City infection
//- Outbreak
//- Cure discovered
//- Victory/Defeat
public class TurnAnimationManager {

        private final StackPane root;

        public TurnAnimationManager(StackPane root) {
                this.root = root;
        }

        public void playTurnStart(String playerName, Runnable onFinished) {
                Label turnLabel = playOverlayMessage(
                                "IT'S " + playerName.toUpperCase() + "'S\nTURN",
                                0.038,
                                "#cfc900",
                                "#000000",
                                10,
                                "-fx-background-color: rgba(0,181,212,0.18);",
                                350,
                                450,
                                1.2,
                                700,
                                0.7,
                                0.85,
                                onFinished);
                Animations.createPulseAnimation(turnLabel, 1.05, 2);
        }

        public void playTurnEnd(Runnable onFinished) {
                playOverlayMessage(
                                "TURN\nCOMPLETE",
                                0.032,
                                "#cfc900",
                                "#000000",
                                10,
                                "-fx-background-color: rgba(207,201,0,0.05);",
                                250,
                                350,
                                0.9,
                                500,
                                0.85,
                                0.92,
                                onFinished);
        }

        public void playActionConsumed(Runnable onFinished) {

                Region flash = createOverlay("-fx-background-color: rgba(0,181,212,0.10);");
                root.getChildren().add(flash);
                flash.toFront();

                FadeTransition fadeIn = fadeTo(flash, 80, 1);
                FadeTransition fadeOut = fadeTo(flash, 220, 0);
                SequentialTransition full = new SequentialTransition(fadeIn, fadeOut);
                full.setOnFinished(e -> {
                        root.getChildren().remove(flash);
                        if (onFinished != null) {
                                onFinished.run();
                        }
                });
                AnimationSpeedUtil.play(full);
        }

        public void playInfectionPhaseIntro(Runnable onFinished) {
                playOverlayMessage(
                                "INFECTION\nPHASE",
                                0.038,
                                "#ff4444",
                                "#660000",
                                20,
                                "-fx-background-color: rgba(120,0,0,0.22);",
                                250,
                                450,
                                1.1,
                                500,
                                0.8,
                                1,
                                onFinished);
        }

        public void playCityInfection(Node cityNode, Runnable onFinished) {
                DropShadow redGlow = new DropShadow();
                redGlow.setColor(Color.RED);
                redGlow.setRadius(0);
                cityNode.setEffect(redGlow);

                Timeline pulse = new Timeline(
                                new KeyFrame(Duration.ZERO, new KeyValue(redGlow.radiusProperty(), 0)),
                                new KeyFrame(Duration.millis(300), new KeyValue(redGlow.radiusProperty(), 45)),
                                new KeyFrame(Duration.millis(650), new KeyValue(redGlow.radiusProperty(), 0)));

                ScaleTransition bounce = new ScaleTransition(Duration.millis(650), cityNode);
                bounce.setToX(1.15);
                bounce.setToY(1.15);
                bounce.setAutoReverse(true);
                bounce.setCycleCount(2);

                ParallelTransition full = new ParallelTransition(pulse, bounce);
                full.setOnFinished(e -> {
                        cityNode.setEffect(null);
                        if (onFinished != null) {
                                onFinished.run();
                        }
                });
                AnimationSpeedUtil.play(full);
        }

        public void playDiseaseTreated(Node cityNode, Runnable onFinished) {
                DropShadow cleanGlow = new DropShadow();
                cleanGlow.setColor(Color.web("#00ffea"));
                cleanGlow.setRadius(0);
                cityNode.setEffect(cleanGlow);

                Timeline glow = new Timeline(
                                new KeyFrame(Duration.ZERO, new KeyValue(cleanGlow.radiusProperty(), 0)),
                                new KeyFrame(Duration.millis(250), new KeyValue(cleanGlow.radiusProperty(), 38)),
                                new KeyFrame(Duration.millis(620), new KeyValue(cleanGlow.radiusProperty(), 0)));

                ScaleTransition shrink = new ScaleTransition(Duration.millis(620), cityNode);
                shrink.setToX(0.88);
                shrink.setToY(0.88);
                shrink.setAutoReverse(true);
                shrink.setCycleCount(2);

                ParallelTransition full = new ParallelTransition(glow, shrink);
                full.setOnFinished(e -> {
                        cityNode.setEffect(null);
                        if (onFinished != null) {
                                onFinished.run();
                        }
                });
                AnimationSpeedUtil.play(full);
        }

        public void playOutbreak(Node cityNode, Runnable onFinished) {
                Region overlay = createOverlay("-fx-background-color: rgba(255,0,0,0.16);");
                Label title = createCenteredTitle("OUTBREAK", 0.045, "#ff2222", "#000000", 25, 0.6);

                root.getChildren().addAll(overlay, title);
                overlay.toFront();
                title.toFront();

                Timeline shake = new Timeline();
                for (int i = 0; i < 24; i++) {
                        double offsetX = (Math.random() * 30) - 15;
                        double offsetY = (Math.random() * 18) - 9;
                        shake.getKeyFrames().add(new KeyFrame(Duration.millis(i * 22.0), new KeyValue(root.translateXProperty(), offsetX),  new KeyValue(root.translateYProperty(), offsetY)));
                }
                shake.getKeyFrames().add(new KeyFrame(Duration.millis(600), new KeyValue(root.translateXProperty(), 0), new KeyValue(root.translateYProperty(), 0)));

                DropShadow outbreakGlow = new DropShadow();
                outbreakGlow.setColor(Color.RED);
                outbreakGlow.setRadius(0);
                cityNode.setEffect(outbreakGlow);

                Timeline cityPulse = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(outbreakGlow.radiusProperty(), 0)),
                                new KeyFrame(Duration.millis(250), new KeyValue(outbreakGlow.radiusProperty(), 55)),
                                new KeyFrame(Duration.millis(650), new KeyValue(outbreakGlow.radiusProperty(), 0)));


                FadeTransition fadeIn = fadeTo(title, 180, 1);
                ScaleTransition scale = scaleTo(title, 350, 1);
                ParallelTransition titleIntro = new ParallelTransition(fadeIn, scale);

                FadeTransition overlayFlash = fadeTo(overlay, 500, 1);
                overlayFlash.setAutoReverse(true);
                overlayFlash.setCycleCount(2);

                PauseTransition hold = new PauseTransition(Duration.seconds(0.4));
                FadeTransition fadeOut = fadeTo(title, 250, 0);

                SequentialTransition full = new SequentialTransition(new ParallelTransition(titleIntro, overlayFlash, shake, cityPulse), hold, fadeOut);

                full.setOnFinished(e -> {
                        cityNode.setEffect(null);
                        root.getChildren().removeAll(overlay, title);
                        if (onFinished != null) {
                                onFinished.run();
                        }
                });
                AnimationSpeedUtil.play(full);
        }

        public void playCureDiscovered(DiseaseColor color, Runnable onFinished) {
                String fxColor = switch (color) {
                        case BLUE -> "#00b5d4";
                        case YELLOW -> "#cfc900";
                        case BLACK -> "#0d8019";
                        case RED -> "#ff2d2d";
                };

                Region overlay = createOverlay("-fx-background-color: rgba(0,0,0,0.28);");
                Label title = createCenteredTitle("CURE\nDISCOVERED", 0.042, fxColor, "#000000", 35, 0.5);
                Circle ring = new Circle();
                ring.setRadius(0);
                ring.setStroke(Color.web(fxColor));
                ring.setStrokeWidth(5);
                ring.setFill(Color.TRANSPARENT);
                ring.setOpacity(0.8);
                StackPane.setAlignment(ring, Pos.CENTER);

                root.getChildren().addAll(overlay, ring, title);
                overlay.toFront();
                ring.toFront();
                title.toFront();

                FadeTransition overlayFade = fadeTo(overlay, 400, 1);
                FadeTransition titleFade = fadeTo(title, 500, 1);
                ScaleTransition titleScale = scaleTo(title, 500, 1);
                Timeline ringExpand = new Timeline(new KeyFrame(Duration.ZERO,
                                                new KeyValue(ring.radiusProperty(), 0),
                                                new KeyValue(ring.opacityProperty(), 0.9)),
                                                new KeyFrame(Duration.seconds(1.2), new KeyValue(ring.radiusProperty(), 340), new KeyValue(ring.opacityProperty(), 0)));

                PauseTransition hold = new PauseTransition(Duration.seconds(1));
                FadeTransition titleOut = fadeTo(title, 500, 0);
                FadeTransition overlayOut = fadeTo(overlay, 500, 0);

                SequentialTransition full = new SequentialTransition(
                                new ParallelTransition(overlayFade, titleFade, titleScale, ringExpand), hold,
                                new ParallelTransition(titleOut, overlayOut));
                full.setOnFinished(e -> {
                        root.getChildren().removeAll(overlay, ring, title);
                        if (onFinished != null) {
                                onFinished.run();
                        }
                });
                AnimationSpeedUtil.play(full);
        }
        public void playVictory(Runnable onFinished) {
                Region overlay = createOverlay("-fx-background-color: rgba(0,0,0,0.55);");
                Label title = createCenteredTitle("MISSION\nACCOMPLISHED", 0.050, "#cfc900", "#00b5d4", 35, 0.45);
                Circle ring = new Circle();
                ring.setRadius(0);
                ring.setStroke(Color.web("#00b5d4"));
                ring.setStrokeWidth(4);
                ring.setFill(Color.TRANSPARENT);
                ring.setOpacity(0.8);
                StackPane.setAlignment(ring, Pos.CENTER);

                root.getChildren().addAll(overlay, ring, title);
                overlay.toFront();
                ring.toFront();
                title.toFront();

                FadeTransition overlayFade = fadeTo(overlay, 600, 1);
                FadeTransition titleFade = fadeTo(title, 700, 1);
                ScaleTransition titleScale = scaleTo(title, 700, 1);

                ScaleTransition pulse = new ScaleTransition(Duration.seconds(1.5), title);
                pulse.setToX(1.06);
                pulse.setToY(1.06);
                pulse.setAutoReverse(true);
                pulse.setCycleCount(2);

                Timeline ringExpand = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(ring.radiusProperty(),0), new KeyValue(ring.opacityProperty(),0.8)), new KeyFrame(Duration.seconds(2),new KeyValue(ring.radiusProperty(), 420),new KeyValue(ring.opacityProperty(), 0)));

                PauseTransition hold = new PauseTransition(Duration.seconds(1.2));

                FadeTransition titleOut = fadeTo(title, 700, 0);
                FadeTransition overlayOut = fadeTo(overlay, 700, 0);

                SequentialTransition full = new SequentialTransition(new ParallelTransition(overlayFade, titleFade, titleScale, ringExpand, pulse), hold, new ParallelTransition(titleOut, overlayOut));

                full.setOnFinished(e -> {
                        root.getChildren().removeAll(overlay, ring, title);

                        if (onFinished != null) {
                                onFinished.run();
                        }
                });
                AnimationSpeedUtil.play(full);
        }
        public void playDefeat(Runnable onFinished) {
                Region overlay = createOverlay("-fx-background-color: rgba(120,0,0,0.72);");
                Label title = createCenteredTitle("SYSTEM\nFAILURE", 0.055, "#ff0909", "#000000", 45, 0.55);

                root.getChildren().addAll(overlay, title);
                overlay.toFront();
                title.toFront();
                Timeline shake = new Timeline();
                for (int i = 0; i < 30; i++) {
                        double offsetX = (Math.random() * 40) - 20;
                        double offsetY = (Math.random() * 24) - 12;
                        shake.getKeyFrames().add(new KeyFrame(Duration.millis(i * 22.0), new KeyValue(root.translateXProperty(), offsetX), new KeyValue(root.translateYProperty(), offsetY)));
                }

                shake.getKeyFrames().add(new KeyFrame(Duration.millis(700), new KeyValue(root.translateXProperty(), 0),new KeyValue( root.translateYProperty(), 0)));

                FadeTransition overlayFade = fadeTo(overlay, 180, 1);
                FadeTransition titleFade = fadeTo(title, 250, 1);
                ScaleTransition titleScale = scaleTo(title, 250, 1);

                ParallelTransition intro = new ParallelTransition(overlayFade, titleFade, titleScale, shake);

                PauseTransition hold = new PauseTransition(Duration.seconds(1.6));

                FadeTransition fadeOut = fadeTo(title, 900, 0);

                SequentialTransition full = new SequentialTransition(intro, hold, fadeOut);
                full.setOnFinished(e -> {
                        root.getChildren().removeAll(overlay, title);
                        if (onFinished != null) {
                                onFinished.run();
                        }
                });
                AnimationSpeedUtil.play(full);
        }

        public void playResearchStationBuilt(Node cityNode, Runnable onFinished) {
                Label title = TextUtil.createText("RESEARCH\nSTATION", "hkmodular",0.035,"#00b5d4",root);

                title.setTextAlignment(TextAlignment.CENTER);
                title.setOpacity(0);
                title.setScaleX(0.5);
                title.setScaleY(0.5);
                GlowUtil.applyGlow(title, "#00b5d4", 22);
                StackPane.setAlignment(title, Pos.CENTER);

                Circle ring = new Circle();
                ring.setRadius(0);
                ring.setStroke(Color.web("#00b5d4"));
                ring.setStrokeWidth(4);
                ring.setFill(Color.TRANSPARENT);
                ring.setOpacity(0.85);
                StackPane.setAlignment(ring, Pos.CENTER);
                DropShadow stationGlow = new DropShadow();
                stationGlow.setColor(Color.web("#00e5ff"));
                stationGlow.setRadius(0);
                cityNode.setEffect(stationGlow);
                Timeline cityPulse = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(stationGlow.radiusProperty(), 0)),
                                new KeyFrame(Duration.millis(400),new KeyValue(stationGlow.radiusProperty(), 45)),
                                new KeyFrame(Duration.millis(900),new KeyValue(stationGlow.radiusProperty(), 0)));
                root.getChildren().addAll(ring, title);
                ring.toFront();
                title.toFront();
                FadeTransition fadeIn = fadeTo(title, 260, 1);
                ScaleTransition scale = scaleTo(title, 420, 1);

                ParallelTransition titleIntro = new ParallelTransition( fadeIn, scale);
                Timeline ringExpand = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(ring.radiusProperty(), 0),new KeyValue(ring.opacityProperty(), 0.8)), new KeyFrame(Duration.seconds(1.2),new KeyValue(ring.radiusProperty(),280), new KeyValue(ring.opacityProperty(), 0)));
                PauseTransition hold = new PauseTransition(Duration.seconds(0.5));

                FadeTransition fadeOut = fadeTo(title, 250, 0);
                SequentialTransition full = new SequentialTransition(new ParallelTransition(titleIntro, ringExpand, cityPulse), hold, fadeOut);

                full.setOnFinished(e -> {
                        cityNode.setEffect(null);
                        root.getChildren().removeAll(ring, title);
                        if (onFinished != null) { onFinished.run(); }
                });
                AnimationSpeedUtil.play(full);
        }

        private Label playOverlayMessage(
                        String message,
                        double fontSize,
                        String textColor,
                        String glowColor,
                        double glowRadius,
                        String overlayStyle,
                        int overlayInMillis,
                        int textInMillis,
                        double holdSeconds,
                        int outMillis,
                        double initialScale,
                        double finalScale,
                        Runnable onFinished) {
                Region overlay = createOverlay(overlayStyle);
                Label title = createCenteredTitle(message, fontSize, textColor, glowColor, glowRadius, initialScale);
                root.getChildren().addAll(overlay, title);
                overlay.toFront();
                title.toFront();

                ParallelTransition intro = new ParallelTransition(
                                fadeTo(overlay, overlayInMillis, 1),
                                fadeTo(title, textInMillis, 1),
                                scaleTo(title, textInMillis, 1));
                PauseTransition hold = new PauseTransition(Duration.seconds(holdSeconds));
                ParallelTransition outro = new ParallelTransition(
                                fadeTo(overlay, outMillis, 0),
                                fadeTo(title, outMillis, 0),
                                scaleTo(title, outMillis, finalScale));

                SequentialTransition full = new SequentialTransition(intro, hold, outro);
                full.setOnFinished(e -> {
                        root.getChildren().removeAll(overlay, title);
                        if (onFinished != null) {
                                onFinished.run();
                        }
                });
                AnimationSpeedUtil.play(full);
                return title;
        }

        private Region createOverlay(String style) {
                Region overlay = new Region();
                overlay.prefWidthProperty().bind(root.widthProperty());
                overlay.prefHeightProperty().bind(root.heightProperty());
                overlay.setStyle(style);
                overlay.setOpacity(0);
                overlay.setMouseTransparent(true);
                return overlay;
        }

        private Label createCenteredTitle(String text, double fontSize, String textColor, String glowColor, double glowRadius, double initialScale) {
                Label title = TextUtil.createText(text, "hkmodular", fontSize, textColor, root);
                title.setTextAlignment(TextAlignment.CENTER);
                GlowUtil.applyGlow(title, glowColor, glowRadius);
                title.setOpacity(0);
                title.setScaleX(initialScale);
                title.setScaleY(initialScale);
                StackPane.setAlignment(title, Pos.CENTER);
                return title;
        }

        private FadeTransition fadeTo(Node node, int millis, double opacity) {
                FadeTransition fade = new FadeTransition(Duration.millis(millis), node);
                fade.setToValue(opacity);
                return fade;
        }

        private ScaleTransition scaleTo(Node node, int millis, double scale) {
                ScaleTransition transition = new ScaleTransition(Duration.millis(millis), node);
                transition.setToX(scale);
                transition.setToY(scale);
                return transition;
        }
}
