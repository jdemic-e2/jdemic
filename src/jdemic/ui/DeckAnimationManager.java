package jdemic.ui;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.ui.GameplayUI.DeckManager;
import jdemic.ui.GameplayUI.HandManager;
import jdemic.GameLogic.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//Class contains the following animations related to the deck and hand:
//- Initial hand shuffle and deal animation at the start of the game
//- Epidemic card reveal animation
//- Infection card draw animation


public class DeckAnimationManager {

        private final StackPane root;
        private final DeckManager deckManager;
        private final HandManager handManager;

        public DeckAnimationManager(StackPane root, DeckManager deckManager, HandManager handManager) {
                this.root = root;
                this.deckManager = deckManager;
                this.handManager = handManager;
        }

        public void playInitialHandAnimation(PlayerState player, Runnable onFinished) {

                if (player == null || player.getHand() == null) {
                        if (onFinished != null) {
                                onFinished.run();
                        }
                        return;
                }

                List<StackPane> animationCards = new ArrayList<>();
                for (Card card : player.getHand()) {
                        CityNode city = card.getTargetCity();
                        if (city != null) {
                                StackPane cardNode = deckManager.createCityCard(city);
                                animationCards.add(cardNode);
                        }
                }
                playShuffleAnimation(animationCards, onFinished);
        }

        public void playStartupShuffleAnimation(Runnable onFinished) {
                List<StackPane> animationCards = new ArrayList<>();
                animationCards.add(deckManager.createBackCard());
                playShuffleAnimation(animationCards, onFinished);
        }

        public void playShuffleAnimation(List<StackPane> cardNodes, Runnable onFinished) {
                if (cardNodes == null || cardNodes.isEmpty()) {
                        if (onFinished != null) {
                                onFinished.run();
                        }
                        return;
                }

                List<StackPane> tempCards = new ArrayList<>();

                double centerX = 0;
                double centerY = 0;
                int visualCardCount = 18;

                for (int i = 0; i < visualCardCount; i++) {
                        StackPane tempCard = deckManager.createBackCard();
                        tempCard.setRotate((Math.random() * 40) - 20);
                        tempCard.setTranslateX(centerX + ((Math.random() * 140) - 70));
                        tempCard.setTranslateY(centerY + ((Math.random() * 90) - 45));
                        tempCard.setScaleX(0.9);
                        tempCard.setScaleY(0.9);
                        root.getChildren().add(tempCard);
                        tempCards.add(tempCard);
                }

                SequentialTransition fullSequence = new SequentialTransition();
                ParallelTransition chaosShuffle = new ParallelTransition();

                for (StackPane card : tempCards) {
                        SequentialTransition cardChaos = new SequentialTransition();
                        for (int j = 0; j < 8; j++) {
                                TranslateTransition shuffleMove = new TranslateTransition(Duration.millis(290), card);
                                shuffleMove.setByX((Math.random() * 260) - 130);
                                shuffleMove.setByY((Math.random() * 180) - 90);
                                RotateTransition rotate = new RotateTransition(Duration.millis(90), card);
                                rotate.setByAngle((Math.random() * 50) - 25);
                                ParallelTransition burst = new ParallelTransition(shuffleMove, rotate);
                                cardChaos.getChildren().add(burst);
                        }
                        chaosShuffle.getChildren().add(cardChaos);
                }

                fullSequence.getChildren().add(chaosShuffle);
                ParallelTransition compressDeck = new ParallelTransition();

                for (StackPane card : tempCards) {
                        TranslateTransition compress = new TranslateTransition(Duration.millis(70), card);
                        compress.setToX(centerX);
                        compress.setToY(centerY);
                        RotateTransition straighten = new RotateTransition(Duration.millis(70), card);
                        straighten.setToAngle(0);
                        compressDeck.getChildren().add(new ParallelTransition(compress, straighten));
                }
                fullSequence.getChildren().add(compressDeck);

                ParallelTransition dealAllCards = new ParallelTransition();

                for (int i = 0; i < tempCards.size(); i++) {

                        StackPane card = tempCards.get(i);
                        double targetX = -root.getWidth() * 0.38 + (i * root.getWidth() * 0.028);
                        double targetY = root.getHeight() * 0.34;
                        TranslateTransition moveToHand = new TranslateTransition(Duration.millis(220), card);
                        moveToHand.setToX(targetX);
                        moveToHand.setToY(targetY);
                        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(220), card);
                        scaleDown.setToX(0.72);
                        scaleDown.setToY(0.72);
                        FadeTransition glowFade = new FadeTransition(Duration.millis(220), card);
                        glowFade.setToValue(0.9);
                        ParallelTransition dealAnimation = new ParallelTransition(moveToHand, scaleDown, glowFade);
                        dealAnimation.setDelay(Duration.millis(i * 22.0));
                        dealAllCards.getChildren().add(dealAnimation);
                }
                fullSequence.getChildren().add(dealAllCards);
                fullSequence.setOnFinished(e -> {
                        root.getChildren().removeAll(tempCards);
                        if (onFinished != null) {
                                onFinished.run();
                        }
                });
                AnimationSpeedUtil.play(fullSequence);
        }

        public void playEpidemicAnimation(Runnable onFinished) {
                Image epidemicImage = SafeResourceLoader.loadImage(Objects.requireNonNull(getClass().getResource("/epidemicCard/SystemBreach.png")));
                ImageView epidemicView = new ImageView(epidemicImage);

                epidemicView.fitWidthProperty().bind(root.widthProperty().multiply(0.14));

                epidemicView.setPreserveRatio(true);

                StackPane epidemicCard = new StackPane(epidemicView);
                DropShadow redGlow = new DropShadow();

                redGlow.setRadius(40);
                redGlow.setSpread(0.45);
                redGlow.setColor(Color.rgb(255, 40, 40, 0.85));
                Timeline glowPulse = new Timeline(
                                new KeyFrame(Duration.ZERO, new KeyValue(redGlow.radiusProperty(), 25)),
                                new KeyFrame(Duration.millis(900), new KeyValue(redGlow.radiusProperty(), 55)));

                glowPulse.setAutoReverse(true);
                glowPulse.setCycleCount(Animation.INDEFINITE);
                AnimationSpeedUtil.play(glowPulse);

                epidemicCard.setEffect(redGlow);
                epidemicCard.setScaleX(0.2);
                epidemicCard.setScaleY(0.2);
                epidemicCard.setRotate(-18);
                epidemicCard.setTranslateX(0);

                epidemicCard.setTranslateY(root.getHeight() * 0.9);
                StackPane.setAlignment(epidemicCard, Pos.CENTER);
                epidemicCard.toFront();

                root.getChildren().add(epidemicCard);

                Region redOverlay = new Region();
                redOverlay.prefWidthProperty().bind(root.widthProperty());
                redOverlay.prefHeightProperty().bind(root.heightProperty());
                redOverlay.setStyle("-fx-background-color: rgba(120,0,0,0.35);");
                redOverlay.setMouseTransparent(true);

                root.getChildren().add(redOverlay);

                redOverlay.toFront();
                epidemicCard.toFront();

                TranslateTransition rise = new TranslateTransition(Duration.millis(850), epidemicCard);

                rise.setToY(0);

                ScaleTransition zoom = new ScaleTransition(Duration.millis(250), epidemicCard);

                zoom.setToX(1.1);
                zoom.setToY(1.1);

                RotateTransition rotate = new RotateTransition(Duration.millis(850), epidemicCard);

                rotate.setFromAngle(-18);
                rotate.setToAngle(0);

                ParallelTransition entrance = new ParallelTransition(rise, zoom, rotate);
                PauseTransition hold = new PauseTransition(Duration.seconds(0.4));

                Timeline flash = new Timeline(
                                new KeyFrame(Duration.ZERO, new KeyValue(redOverlay.opacityProperty(), 0.0)),
                                new KeyFrame(Duration.millis(600), new KeyValue(redOverlay.opacityProperty(), 0.08)),
                                new KeyFrame(Duration.millis(2400), new KeyValue(redOverlay.opacityProperty(), 0.05)),
                                new KeyFrame(Duration.millis(3200), new KeyValue(redOverlay.opacityProperty(), 0.0)));

                Timeline shake = new Timeline();

                for (int i = 0; i < 10; i++) {
                        double offsetX = (Math.random() * 14) - 7;
                        double offsetY = (Math.random() * 10) - 5;

                        shake.getKeyFrames().add(new KeyFrame(Duration.millis(i * 22.0),
                                                        new KeyValue(root.translateXProperty(), offsetX),
                                                        new KeyValue(root.translateYProperty(), offsetY)));
                }

                shake.getKeyFrames().add(new KeyFrame(Duration.millis(1800), new KeyValue(root.translateXProperty(), 0),
                                new KeyValue(root.translateYProperty(), 0)));

                SequentialTransition full = new SequentialTransition(entrance, new ParallelTransition(flash, shake),
                                hold);
                full.setOnFinished(e -> {
                        glowPulse.stop();
                        root.getChildren().remove(epidemicCard);
                        root.getChildren().remove(redOverlay);
                        if (onFinished != null) {
                                onFinished.run();
                        }
                });
                AnimationSpeedUtil.play(full);
        }

        public void playInfectionCardDraw(StackPane infectionCard, Runnable onFinished) {
                infectionCard.setScaleX(0.12);
                infectionCard.setScaleY(0.12);
                infectionCard.setOpacity(0);
                infectionCard.setRotate(0);
                infectionCard.setTranslateX(0);
                infectionCard.setTranslateY(0);
                StackPane.setAlignment(infectionCard, Pos.CENTER);

                DropShadow redGlow = new DropShadow();
                redGlow.setColor(Color.RED);
                redGlow.setRadius(45);
                infectionCard.setEffect(redGlow);

                root.getChildren().add(infectionCard);
                infectionCard.toFront();

                FadeTransition fadeIn = new FadeTransition(Duration.millis(180), infectionCard);
                fadeIn.setToValue(1);
                ScaleTransition scaleUp = new ScaleTransition(Duration.millis(900), infectionCard);
                scaleUp.setToX(2.8);
                scaleUp.setToY(2.8);
                scaleUp.setInterpolator(Interpolator.EASE_OUT);
                RotateTransition spin = new RotateTransition(Duration.millis(900), infectionCard);
                spin.setByAngle(1080);
                spin.setInterpolator(Interpolator.EASE_OUT);

                ParallelTransition intro = new ParallelTransition(fadeIn, scaleUp, spin);

                PauseTransition hold = new PauseTransition(Duration.seconds(1));

                FadeTransition fadeOut = new FadeTransition(Duration.millis(150), infectionCard);
                fadeOut.setToValue(0);

                ScaleTransition shrink = new ScaleTransition(Duration.millis(150), infectionCard);
                shrink.setToX(2.3);
                shrink.setToY(2.3);

                ParallelTransition outro = new ParallelTransition(fadeOut, shrink);

                SequentialTransition full = new SequentialTransition(intro, hold, outro);

                full.setOnFinished(e -> {
                        root.getChildren().remove(infectionCard);
                        infectionCard.setEffect(null);
                        if (onFinished != null) { onFinished.run(); }
                });

                AnimationSpeedUtil.play(full);
        }
}
