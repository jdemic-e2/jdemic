package jdemic.ui.GameplayUI;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import jdemic.ui.CardResourceUtil;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CityNode;
import jdemic.ui.GlowUtil;
import jdemic.ui.UIImageUtil;

public class DeckManager {

    private StackPane root;

    public DeckManager(StackPane root) { this.root = root; setupDecks(); }

    public void setupDecks() {
        HBox decks = new HBox(createCityDeck(), createEpidemicDeck());
        decks.spacingProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(12, root.getWidth() * 0.018),
                root.widthProperty()
        ));

        StackPane wrapper = new StackPane(decks);
        wrapper.setPickOnBounds(false);
        wrapper.setMouseTransparent(true); // decorative; don't intercept clicks
        wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        root.getChildren().add(wrapper);

        StackPane.setAlignment(wrapper, Pos.BOTTOM_RIGHT);
    }

    public StackPane createCardStack(Image topImage, int stackSize) {
        StackPane stack = new StackPane();
        stack.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        for (int i = 0; i < stackSize; i++) {
            ImageView card = new ImageView(topImage);
            card.setPreserveRatio(true);
            card.fitWidthProperty().bind(Bindings.createDoubleBinding(
                    () -> Math.max(48, Math.min(84, root.getWidth() * 0.055)),
                    root.widthProperty()
            ));
            card.setTranslateX((double) i);
            card.setTranslateY(-(double) i);
            card.setRotate(((i % 3) - 1) * 0.75);
            stack.getChildren().add(card);
        }
        return stack;
    }

    public HBox createCityDeck() {
        Image verso = UIImageUtil.load("/cityCards/cityCardsVerso.png");
        if (verso == null) {
            return new HBox();
        }
        StackPane drawPile = createCardStack(verso, 6);
        Image topCard;
        try {
            String path = "/cityCards/BlueAtlanta.png"; //for test
            topCard = UIImageUtil.load(path);
            if (topCard == null) topCard = verso;
        } catch (Exception e) { topCard = verso; }

        StackPane discardPile = createCardStack(topCard, 4);

        GlowUtil.applyGlow(drawPile, "#00d9ff", 8);
        GlowUtil.applyGlow(discardPile, "#00d9ff", 8);

        HBox cityDeck = new HBox(drawPile, discardPile);
        cityDeck.spacingProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(8, root.getWidth() * 0.012),
                root.widthProperty()
        ));
        return cityDeck;
    }

    public StackPane createCityCard(CityNode city) {
        StackPane wrapper = createCardWrapper(city, CardResourceUtil.cityCardPath(city), "#00b5d4", 5);

        wrapper.setOnMouseEntered(e -> {
            wrapper.setViewOrder(-1);
            wrapper.setTranslateY(-50);
            wrapper.setScaleX(1.5);
            wrapper.setScaleY(1.5);
        });

        wrapper.setOnMouseExited(e -> {
            wrapper.setViewOrder(0);
            wrapper.setTranslateY(0);
            wrapper.setScaleX(1);
            wrapper.setScaleY(1);
        });
        return wrapper;
    }

    public HBox createEpidemicDeck() {
        Image verso = UIImageUtil.load("/epidemicCards/epidemicCardsVerso.png");
        if (verso == null) {
            return new HBox();
        }
        StackPane drawPile = createCardStack(verso, 5);
        Image topCard;
        try {
            String path = "/epidemicCards/BlueAtlanta.png"; //for test
            topCard = UIImageUtil.load(path);
            if (topCard == null) topCard = verso;
        } catch (Exception e) { topCard = verso; }

        StackPane discardPile = createCardStack(topCard, 3);
        GlowUtil.applyGlow(drawPile, "#ff2d2d", 8);
        GlowUtil.applyGlow(discardPile, "#ff2d2d", 8);

        HBox epidemicDeck = new HBox(drawPile, discardPile);
        epidemicDeck.spacingProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(8, root.getWidth() * 0.012),
                root.widthProperty()
        ));
        return epidemicDeck;
    }

    //for shuffling animation, creates a single card back to be animated from the deck to the player's hand
    public StackPane createBackCard() {
        ImageView imageView = UIImageUtil.loadResponsive(root, "/cityCards/cityCardsVerso.png", 44, 76, 0.05);
        if (imageView == null || imageView.getImage() == null) return new StackPane();
        imageView.setPreserveRatio(true);
        StackPane wrapper = new StackPane(imageView);
        wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        GlowUtil.applyGlow(wrapper, "#00b5d4", 8);
        return wrapper;
    }

    public StackPane createEpidemicCard(CityNode city) {
        return createCardWrapper(city, CardResourceUtil.epidemicCardPath(city), "#ff2d2d", 10);
    }

    public StackPane createEventCard(Card card) {
        String path = CardResourceUtil.eventCardPath(card);
        if (path == null) {
            return fallbackCard(card == null ? "EVENT" : card.getCardName());
        }

        ImageView imageView = UIImageUtil.loadResponsive(root, path, 48, 84, 0.055);
        if (imageView == null || imageView.getImage() == null) {
            return fallbackCard(card.getCardName());
        }

        imageView.setPreserveRatio(true);
        StackPane wrapper = new StackPane(imageView);
        wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        GlowUtil.applyGlow(wrapper, "#cfc900", 8);
        // hover behavior: match city cards (enlarge and lift on hover)
        wrapper.setOnMouseEntered(e -> {
            wrapper.setViewOrder(-1);
            wrapper.setTranslateY(-50);
            wrapper.setScaleX(1.5);
            wrapper.setScaleY(1.5);
        });

        wrapper.setOnMouseExited(e -> {
            wrapper.setViewOrder(0);
            wrapper.setTranslateY(0);
            wrapper.setScaleX(1);
            wrapper.setScaleY(1);
        });

        return wrapper;
    }

    private StackPane createCardWrapper(CityNode city, String path, String glowColor, double glowRadius) {
        ImageView card;
        try {
            ImageView iv = UIImageUtil.loadResponsive(root, path, 48, 84, 0.055);
            if (iv == null || iv.getImage() == null) { return new StackPane(new javafx.scene.control.Label(city.getName())); }
            card = iv;

        } catch (Exception e) { return new StackPane(new javafx.scene.control.Label(city.getName())); }

        card.setPreserveRatio(true);
        card.fitWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(48, Math.min(84, root.getWidth() * 0.055)),
                root.widthProperty()
        ));
        StackPane wrapper = new StackPane(card);
        wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        GlowUtil.applyGlow(wrapper, glowColor, glowRadius);
        return wrapper;
    }

    private StackPane fallbackCard(String label) {
        return new StackPane(new javafx.scene.control.Label(label == null ? "CARD" : label));
    }
}
