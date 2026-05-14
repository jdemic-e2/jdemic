package jdemic.ui.GameplayUI;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.ui.GlowUtil;
import javafx.scene.Node;
import jdemic.ui.TextUtil;

import java.util.*;
import java.util.function.Consumer;

public class HandManager {

    private StackPane root;
    private DeckManager deckManager;

    private HBox handContainer;

    public HandManager(StackPane root, DeckManager deckManager) {
        this.root = root;
        this.deckManager = deckManager;
        setupPlayerHand();
    }

    public void setupPlayerHand() {
        handContainer = new HBox();
        handContainer.setAlignment(Pos.BOTTOM_LEFT);
        handContainer.setPickOnBounds(false);
        handContainer.spacingProperty().bind(Bindings.createDoubleBinding(() -> {
                    int cardCount = handContainer.getChildren().size();
                    if (cardCount <= 1) return -20.0;
                    double cardWidth = Math.max(70, root.getWidth() * 0.06);
                    return -cardWidth * 0.25;
                }, root.widthProperty(), Bindings.size(handContainer.getChildren()))
        );

        handContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(root.getHeight() * 0.015, root.getWidth() * 0.02, root.getHeight() * 0.015, root.getWidth() * 0.02), root.heightProperty(), root.widthProperty()));

        ScrollPane handScroller = new ScrollPane(handContainer);
        handScroller.setFitToHeight(true);
        handScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        handScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        handScroller.setPannable(true);
        handScroller.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        handScroller.prefViewportHeightProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(92, root.getHeight() * 0.16),
                root.heightProperty()
        ));
        handScroller.prefViewportWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(220, Math.min(root.getWidth() * 0.46, root.getWidth() - 360)),
                root.widthProperty()
        ));

        StackPane wrapper = createGlowBox(handScroller, "#00b5d4", 15);

        wrapper.prefWidthProperty().bind(
                Bindings.createDoubleBinding(() -> {
                    int count = handContainer.getChildren().size();
                    double maxWidth = Math.max(220, Math.min(root.getWidth() * 0.46, root.getWidth() - 360));
                    if (count == 0) return Math.min(220.0, maxWidth);

                    double cardWidth = Math.max(70, root.getWidth() * 0.06);
                    double spacing = handContainer.getSpacing();

                    double contentWidth = cardWidth + (count - 1) * (cardWidth + spacing) + cardWidth * 0.3;
                    return Math.min(maxWidth, contentWidth);

                }, root.widthProperty(), handContainer.spacingProperty(), Bindings.size(handContainer.getChildren()))
        );

        wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        wrapper.translateXProperty().bind(root.widthProperty().multiply(0.018));
        wrapper.translateYProperty().bind(root.heightProperty().multiply(-0.018));

        StackPane.setAlignment(wrapper, Pos.BOTTOM_LEFT);

        root.getChildren().add(wrapper);
    }

    public void updateHand(PlayerState player, boolean discardMode, Consumer<Integer> discardHandler) {
        handContainer.getChildren().clear();
        if (player == null || player.getHand() == null) {
            return;
        }

        List<Card> hand = player.getHand();
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            StackPane cardNode = createCardNode(card);
            int cardIndex = i;

            if (discardMode && discardHandler != null) {
                cardNode.setCursor(Cursor.HAND);
                cardNode.setOnMouseClicked(e -> discardHandler.accept(cardIndex));
            }

            handContainer.getChildren().add(cardNode);
        }
    }

    private StackPane createCardNode(Card card) {
        CityNode city = card.getTargetCity();
        if (city != null) {
            return deckManager.createCityCard(city);
        }

        Label title = TextUtil.createText(card.getCardName(), "hkmodular", 0.010, "#ffffff", root);
        Label type = TextUtil.createText(card.getType().name(), "hkmodular", 0.008, "#cfc900", root);

        VBox content = new VBox(6, title, type);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(8));

        StackPane wrapper = new StackPane(content);
        wrapper.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> Math.max(70, root.getWidth() * 0.06), root.widthProperty()));
        wrapper.prefHeightProperty().bind(wrapper.prefWidthProperty().multiply(1.4));
        wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        wrapper.setStyle("-fx-background-color: black; -fx-border-color: #00b5d4; -fx-border-width: 1; -fx-background-radius: 6; -fx-border-radius: 6;");
        GlowUtil.applyGlow(wrapper, "#00b5d4", 5);
        return wrapper;
    }

    private StackPane createGlowBox(Node content, String color, double glowRadius) {
        StackPane box = new StackPane(content);
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.setStyle("-fx-background-color: black;" + "-fx-border-color: transparent " + color + " transparent transparent;" + "-fx-border-width: 0 1 0 0;");
        GlowUtil.applyGlow(box, color, glowRadius);
        return box;
    }
}
