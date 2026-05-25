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
                Region overlay = new Region();
                overlay.prefWidthProperty().bind(root.widthProperty());
                overlay.prefHeightProperty().bind(root.heightProperty());
                overlay.setStyle("-fx-background-color: rgba(0,181,212,0.18);");
                overlay.setOpacity(0);
                overlay.setMouseTransparent(true);

                Label turnLabel = TextUtil.createText("IT'S " + playerName.toUpperCase() + "'S" + "\nTURN", "hkmodular",
                                0.038, "#cfc900", root);
                turnLabel.setTextAlignment(TextAlignment.CENTER);
                GlowUtil.applyGlow(turnLabel, "#000000", 10);
                Animations.createPulseAnimation(turnLabel, 1.05, 2);
                turnLabel.setOpacity(0);
                turnLabel.setScaleX(0.7);
                turnLabel.setScaleY(0.7);
                StackPane.setAlignment(turnLabel, Pos.CENTER);

                root.getChildren().addAll(overlay, turnLabel);
                overlay.toFront();
                turnLabel.toFront();

                FadeTransition overlayIn = new FadeTransition(Duration.millis(350), overlay);
                overlayIn.setToValue(1);

                FadeTransition overlayOut = new FadeTransition(Duration.millis(700), overlay);
                overlayOut.setToValue(0);

                FadeTransition textFadeIn = new FadeTransition(Duration.millis(450), turnLabel);
                textFadeIn.setToValue(1);

                ScaleTransition textScale = new ScaleTransition(Duration.millis(450), turnLabel);
                textScale.setToX(1);
                textScale.setToY(1);

                ParallelTransition textIntro = new ParallelTransition(textFadeIn, textScale);

                PauseTransition hold = new PauseTransition(Duration.seconds(1.2));

                FadeTransition textFadeOut = new FadeTransition(Duration.millis(500), turnLabel);
                textFadeOut.setToValue(0);

                ScaleTransition textShrink = new ScaleTransition(Duration.millis(500), turnLabel);
                textShrink.setToX(0.85);
                textShrink.setToY(0.85);

                ParallelTransition textOutro = new ParallelTransition(textFadeOut, textShrink);

                SequentialTransition full = new SequentialTransition(overlayIn, textIntro, hold,
                                new ParallelTransition(overlayOut, textOutro));

                full.setOnFinished(e -> {
                        root.getChildren().removeAll(overlay, turnLabel);
                        if (onFinished != null) {
                                onFinished.run();
                        }
                });
                AnimationSpeedUtil.play(full);
        }

        public void playTurnEnd(Runnable onFinished) {

                Region overlay = new Region();
                overlay.prefWidthProperty().bind(root.widthProperty());
                overlay.prefHeightProperty().bind(root.heightProperty());
                overlay.setStyle("-fx-background-color: rgba(207,201,0,0.05);");
                overlay.setOpacity(0);
                overlay.setMouseTransparent(true);

                Label endLabel = TextUtil.createText("TURN\nCOMPLETE", "hkmodular", 0.032, "#cfc900", root);
                endLabel.setTextAlignment(TextAlignment.CENTER);
                GlowUtil.applyGlow(endLabel, "#000000", 10);
                endLabel.setOpacity(0);
                endLabel.setScaleX(0.85);
                endLabel.setScaleY(0.85);
                StackPane.setAlignment(endLabel, Pos.CENTER);

                root.getChildren().addAll(overlay, endLabel);
                overlay.toFront();
                endLabel.toFront();

                FadeTransition overlayFadeIn = new FadeTransition(Duration.millis(250), overlay);
                overlayFadeIn.setToValue(1);
                FadeTransition textFadeIn = new FadeTransition(Duration.millis(350), endLabel);
                textFadeIn.setToValue(1);

                ScaleTransition textGrow = new ScaleTransition(Duration.millis(350), endLabel);
                textGrow.setToX(1);
                textGrow.setToY(1);

                ParallelTransition intro = new ParallelTransition(overlayFadeIn, textFadeIn, textGrow);

                PauseTransition hold = new PauseTransition(Duration.seconds(0.9));

                FadeTransition overlayFadeOut = new FadeTransition(Duration.millis(500), overlay);

                overlayFadeOut.setToValue(0);

                FadeTransition textFadeOut = new FadeTransition(Duration.millis(500), endLabel);
                textFadeOut.setToValue(0);

                ScaleTransition textShrink = new ScaleTransition(Duration.millis(500), endLabel);
                textShrink.setToX(0.92);
                textShrink.setToY(0.92);

                ParallelTransition outro = new ParallelTransition(overlayFadeOut, textFadeOut, textShrink);

                SequentialTransition full = new SequentialTransition(intro, hold, outro);
                full.setOnFinished(e -> {
                        root.getChildren().removeAll(overlay, endLabel);
                        if (onFinished != null) {
                                onFinished.run();
                        }
                });
                AnimationSpeedUtil.play(full);
        }

        public void playActionConsumed(Runnable onFinished) {

                Region flash = new Region();
                flash.prefWidthProperty().bind(root.widthProperty());
                flash.prefHeightProperty().bind(root.heightProperty());
                flash.setStyle("-fx-background-color: rgba(0,181,212,0.10);");
                flash.setOpacity(0);
                flash.setMouseTransparent(true);
                root.getChildren().add(flash);
                flash.toFront();

                FadeTransition fadeIn = new FadeTransition(Duration.millis(80), flash);
                fadeIn.setToValue(1);
                FadeTransition fadeOut = new FadeTransition(Duration.millis(220), flash);
                fadeOut.setToValue(0);
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
                Region overlay = new Region();
                overlay.prefWidthProperty().bind(root.widthProperty());
                overlay.prefHeightProperty().bind(root.heightProperty());
                overlay.setStyle("-fx-background-color: rgba(120,0,0,0.22);");
                overlay.setOpacity(0);
                overlay.setMouseTransparent(true);

                Label title = TextUtil.createText("INFECTION\nPHASE", "hkmodular", 0.038, "#ff4444", root);
                title.setTextAlignment(TextAlignment.CENTER);
                GlowUtil.applyGlow(title, "#660000", 20);
                title.setOpacity(0);
                title.setScaleX(0.8);
                title.setScaleY(0.8);
                StackPane.setAlignment(title, Pos.CENTER);
                root.getChildren().addAll(overlay, title);
                overlay.toFront();
                title.toFront();

                FadeTransition overlayFade = new FadeTransition(Duration.millis(250), overlay);
                overlayFade.setToValue(1);

                FadeTransition textFade = new FadeTransition(Duration.millis(450), title);
                textFade.setToValue(1);

                ScaleTransition textScale = new ScaleTransition(Duration.millis(450), title);
                textScale.setToX(1);
                textScale.setToY(1);

                ParallelTransition intro = new ParallelTransition(overlayFade, textFade, textScale);

                PauseTransition hold = new PauseTransition(Duration.seconds(1.1));

                FadeTransition overlayOut = new FadeTransition(Duration.millis(500), overlay);
                overlayOut.setToValue(0);

                FadeTransition textOut = new FadeTransition(Duration.millis(500), title);

                textOut.setToValue(0);
                ParallelTransition outro = new ParallelTransition(overlayOut, textOut);

                SequentialTransition full = new SequentialTransition(intro, hold, outro);

                full.setOnFinished(e -> {
                        root.getChildren().removeAll(overlay, title);
                        if (onFinished != null) {
                                onFinished.run();
                        }
                });
                AnimationSpeedUtil.play(full);
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
                Region overlay = new Region();
                overlay.prefWidthProperty().bind(root.widthProperty());
                overlay.prefHeightProperty().bind(root.heightProperty());
                overlay.setStyle("-fx-background-color: rgba(255,0,0,0.16);");
                overlay.setOpacity(0);
                overlay.setMouseTransparent(true);
                Label title = TextUtil.createText("OUTBREAK","hkmodular", 0.045, "#ff2222", root);
                title.setTextAlignment(TextAlignment.CENTER);
                title.setOpacity(0);
                title.setScaleX(0.6);
                title.setScaleY(0.6);
                GlowUtil.applyGlow(title, "#000000", 25);
                StackPane.setAlignment(title, Pos.CENTER);

                root.getChildren().addAll(overlay, title);
                overlay.toFront();
                title.toFront();

                Timeline shake = new Timeline();
                for (int i = 0; i < 24; i++) {
                        double offsetX = (Math.random() * 30) - 15;
                        double offsetY = (Math.random() * 18) - 9;
                        shake.getKeyFrames().add(new KeyFrame(Duration.millis(i * 22), new KeyValue(root.translateXProperty(), offsetX),  new KeyValue(root.translateYProperty(), offsetY)));
                }
                shake.getKeyFrames().add(new KeyFrame(Duration.millis(600), new KeyValue(root.translateXProperty(), 0), new KeyValue(root.translateYProperty(), 0)));

                DropShadow outbreakGlow = new DropShadow();
                outbreakGlow.setColor(Color.RED);
                outbreakGlow.setRadius(0);
                cityNode.setEffect(outbreakGlow);

                Timeline cityPulse = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(outbreakGlow.radiusProperty(), 0)),
                                new KeyFrame(Duration.millis(250), new KeyValue(outbreakGlow.radiusProperty(), 55)),
                                new KeyFrame(Duration.millis(650), new KeyValue(outbreakGlow.radiusProperty(), 0)));


                FadeTransition fadeIn = new FadeTransition(Duration.millis(180), title);
                fadeIn.setToValue(1);
                ScaleTransition scale = new ScaleTransition(Duration.millis(350), title);
                scale.setToX(1);
                scale.setToY(1);
                ParallelTransition titleIntro = new ParallelTransition(fadeIn, scale);

                FadeTransition overlayFlash = new FadeTransition(Duration.millis(500), overlay);
                overlayFlash.setToValue(1);
                overlayFlash.setAutoReverse(true);
                overlayFlash.setCycleCount(2);

                PauseTransition hold = new PauseTransition(Duration.seconds(0.4));
                FadeTransition fadeOut = new FadeTransition(Duration.millis(250), title);
                fadeOut.setToValue(0);

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

                Region overlay = new Region();
                overlay.prefWidthProperty().bind(root.widthProperty());
                overlay.prefHeightProperty().bind(root.heightProperty());
                overlay.setStyle("-fx-background-color: rgba(0,0,0,0.28);");
                overlay.setOpacity(0);
                overlay.setMouseTransparent(true);

                Label title = TextUtil.createText("CURE\nDISCOVERED", "hkmodular", 0.042, fxColor, root);
                title.setTextAlignment(TextAlignment.CENTER);
                title.setOpacity(0);
                title.setScaleX(0.5);
                title.setScaleY(0.5);

                GlowUtil.applyGlow(title, "#000000", 35);
                StackPane.setAlignment(title, Pos.CENTER);
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

                FadeTransition overlayFade = new FadeTransition(Duration.millis(400), overlay);
                overlayFade.setToValue(1);
                FadeTransition titleFade = new FadeTransition(Duration.millis(500), title);
                titleFade.setToValue(1);
                ScaleTransition titleScale = new ScaleTransition(Duration.millis(500), title);
                titleScale.setToX(1);
                titleScale.setToY(1);
                Timeline ringExpand = new Timeline(new KeyFrame(Duration.ZERO,
                                                new KeyValue(ring.radiusProperty(), 0),
                                                new KeyValue(ring.opacityProperty(), 0.9)),
                                                new KeyFrame(Duration.seconds(1.2), new KeyValue(ring.radiusProperty(), 340), new KeyValue(ring.opacityProperty(), 0)));

                PauseTransition hold = new PauseTransition(Duration.seconds(1));
                FadeTransition titleOut = new FadeTransition(Duration.millis(500), title);
                titleOut.setToValue(0);
                FadeTransition overlayOut = new FadeTransition(Duration.millis(500), overlay);
                overlayOut.setToValue(0);

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
                Region overlay = new Region();
                overlay.prefWidthProperty().bind(root.widthProperty());
                overlay.prefHeightProperty().bind(root.heightProperty());
                overlay.setStyle("-fx-background-color: rgba(0,0,0,0.55);");
                overlay.setOpacity(0);

                Label title = TextUtil.createText("MISSION\nACCOMPLISHED", "hkmodular", 0.050, "#cfc900", root);
                title.setTextAlignment(TextAlignment.CENTER);
                title.setOpacity(0);
                title.setScaleX(0.45);
                title.setScaleY(0.45);
                GlowUtil.applyGlow(title, "#00b5d4", 35);
                StackPane.setAlignment(title, Pos.CENTER);
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

                FadeTransition overlayFade = new FadeTransition(Duration.millis(600), overlay);
                overlayFade.setToValue(1);


                FadeTransition titleFade = new FadeTransition(Duration.millis(700), title);
                titleFade.setToValue(1);
                ScaleTransition titleScale = new ScaleTransition(Duration.millis(700), title);
                titleScale.setToX(1);
                titleScale.setToY(1);

                ScaleTransition pulse = new ScaleTransition(Duration.seconds(1.5), title);
                pulse.setToX(1.06);
                pulse.setToY(1.06);
                pulse.setAutoReverse(true);
                pulse.setCycleCount(2);

                Timeline ringExpand = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(ring.radiusProperty(),0), new KeyValue(ring.opacityProperty(),0.8)), new KeyFrame(Duration.seconds(2),new KeyValue(ring.radiusProperty(), 420),new KeyValue(ring.opacityProperty(), 0)));

                PauseTransition hold = new PauseTransition(Duration.seconds(1.2));

                FadeTransition titleOut = new FadeTransition(Duration.millis(700), title);

                titleOut.setToValue(0);
                FadeTransition overlayOut = new FadeTransition(Duration.millis(700), overlay);
                overlayOut.setToValue(0);

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
                Region overlay = new Region();
                overlay.prefWidthProperty().bind(root.widthProperty());
                overlay.prefHeightProperty().bind(root.heightProperty());
                overlay.setStyle("-fx-background-color: rgba(120,0,0,0.72);");
                overlay.setOpacity(0);

                Label title = TextUtil.createText( "SYSTEM\nFAILURE", "hkmodular",0.055,"#ff0909",root);
                title.setTextAlignment(TextAlignment.CENTER);
                title.setOpacity(0);
                title.setScaleX(0.55);
                title.setScaleY(0.55);
                GlowUtil.applyGlow(title,  "#000000",45);
                StackPane.setAlignment(title, Pos.CENTER);

                root.getChildren().addAll(overlay, title);
                overlay.toFront();
                title.toFront();
                Timeline shake = new Timeline();
                for (int i = 0; i < 30; i++) {
                        double offsetX = (Math.random() * 40) - 20;
                        double offsetY = (Math.random() * 24) - 12;
                        shake.getKeyFrames().add(new KeyFrame(Duration.millis(i * 22), new KeyValue(root.translateXProperty(), offsetX), new KeyValue(root.translateYProperty(), offsetY)));
                }

                shake.getKeyFrames().add(new KeyFrame(Duration.millis(700), new KeyValue(root.translateXProperty(), 0),new KeyValue( root.translateYProperty(), 0)));

                FadeTransition overlayFade = new FadeTransition(Duration.millis(180), overlay);
                overlayFade.setToValue(1);

                FadeTransition titleFade = new FadeTransition(Duration.millis(250), title);
                titleFade.setToValue(1);
                ScaleTransition titleScale = new ScaleTransition(Duration.millis(250), title);
                titleScale.setToX(1);
                titleScale.setToY(1);

                ParallelTransition intro = new ParallelTransition(overlayFade, titleFade, titleScale, shake);

                PauseTransition hold = new PauseTransition(Duration.seconds(1.6));

                FadeTransition fadeOut = new FadeTransition(Duration.millis(900), title);
                fadeOut.setToValue(0);

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
                FadeTransition fadeIn = new FadeTransition(Duration.millis(260), title);
                fadeIn.setToValue(1);
                ScaleTransition scale = new ScaleTransition(Duration.millis(420), title);
                scale.setToX(1);
                scale.setToY(1);

                ParallelTransition titleIntro = new ParallelTransition( fadeIn, scale);
                Timeline ringExpand = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(ring.radiusProperty(), 0),new KeyValue(ring.opacityProperty(), 0.8)), new KeyFrame(Duration.seconds(1.2),new KeyValue(ring.radiusProperty(),280), new KeyValue(ring.opacityProperty(), 0)));
                PauseTransition hold = new PauseTransition(Duration.seconds(0.5));

                FadeTransition fadeOut = new FadeTransition(Duration.millis(250), title);
                fadeOut.setToValue(0);
                SequentialTransition full = new SequentialTransition(new ParallelTransition(titleIntro, ringExpand, cityPulse), hold, fadeOut);

                full.setOnFinished(e -> {
                        cityNode.setEffect(null);
                        root.getChildren().removeAll(ring, title);
                        if (onFinished != null) { onFinished.run(); }
                });
                AnimationSpeedUtil.play(full);
        }
}
