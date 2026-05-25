package jdemic.ui.GameplayUI;

import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import jdemic.GameLogic.CityNode;
import jdemic.ui.GlowUtil;
import jdemic.ui.SafeResourceLoader;
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
        ImageView card;
        try {
            String colorPrefix = switch (city.getNativeColor()) {
                case BLUE -> "Blue";
                case YELLOW -> "Yellow";
                case BLACK -> "Green"; // MISMATCHH (Black should be changed Green everywhere to not cause confusion)
                case RED -> "Red";
            };

            String cityName = city.getName().replace(" ", "").replace(".", "");
            String path = "/cityCards/" + colorPrefix + cityName + ".png";
            ImageView iv = UIImageUtil.loadResponsive(root, path, 48, 84, 0.055);
            if (iv == null || iv.getImage() == null) { return new StackPane(new javafx.scene.control.Label(city.getName())); }
            card = iv;

        } catch (Exception e) { return new StackPane(new javafx.scene.control.Label(city.getName()));}

        card.setPreserveRatio(true);
        card.fitWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(48, Math.min(84, root.getWidth() * 0.055)),
                root.widthProperty()
        ));

        StackPane wrapper = new StackPane(card);
        wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        GlowUtil.applyGlow(wrapper, "#00b5d4", 5);

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
        ImageView card;
        try {
            String colorPrefix = switch (city.getNativeColor()) {
                case BLUE -> "Blue";
                case YELLOW -> "Yellow";
                case BLACK -> "Green"; // ?? black/green? It's functional, but confusing.
                case RED -> "Red";
            };

            String cityName = city.getName().replace(" ", "").replace(".", "");
            String path ="/epidemicCards/" + colorPrefix + cityName + ".png";
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
        GlowUtil.applyGlow(wrapper, "#ff2d2d", 10);
        return wrapper;
    }
}
