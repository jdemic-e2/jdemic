package jdemic.ui.GameplayUI;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;
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
                    if (cardCount <= 1) return 0.0;
                    if (cardCount <= 4) return -12.0;
                    if (cardCount == 5) return -20.0;
                    if (cardCount == 6) return -28.0;
                    if (cardCount == 7) return -34.0;
                    return -40.0;
                }, Bindings.size(handContainer.getChildren())));

        handContainer.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(root.getHeight() * 0.015, root.getWidth() * 0.020, root.getHeight() * 0.015, root.getWidth() * 0.020), root.heightProperty(), root.widthProperty()));

        StackPane wrapper = createGlowBox(handContainer, "#00b5d4", 15);

        wrapper.prefWidthProperty().bind(
                Bindings.createDoubleBinding(() -> {
                    double maxWidth = Math.max(180, Math.min(420, root.getWidth() * 0.38));
                    int count = handContainer.getChildren().size();
                    if (count == 0) return 200.0;
                    double scale = Math.max(0.038, 0.055 - (count  * 0.001));
                    double cardWidth = Math.max(48, Math.min(82, root.getWidth() * scale));
                    double spacing = handContainer.getSpacing();
                    double calculatedWidth = cardWidth + (count - 1) * (cardWidth + spacing);
                    return Math.min(calculatedWidth + cardWidth * 0.4, maxWidth);
                }, root.widthProperty(), handContainer.spacingProperty(), Bindings.size(handContainer.getChildren())));
        wrapper.prefHeightProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(96, Math.min(150, root.getHeight() * 0.19)),
                root.heightProperty()
        ));
        wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane.setAlignment(wrapper, Pos.BOTTOM_LEFT);
        StackPane.setMargin(wrapper, Insets.EMPTY);
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
        if (card.getType() == CardType.EVENT) {
            return deckManager.createEventCard(card);
        }

        Label title = TextUtil.createText(card.getCardName(), "hkmodular", 0.010, "#ffffff", root);
        Label type = TextUtil.createText(card.getType().name(), "hkmodular", 0.008, "#cfc900", root);

        VBox content = new VBox(6, title, type);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(8));

        StackPane wrapper = new StackPane(content);
        wrapper.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> {
                    int cardCount = handContainer.getChildren().size();
                    double scale = Math.max(0.038, 0.055 - (cardCount * 0.002));
                    return Math.max(48, Math.min(82, root.getWidth() * scale));
                }, root.widthProperty(),  Bindings.size(handContainer.getChildren()))
        );
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
