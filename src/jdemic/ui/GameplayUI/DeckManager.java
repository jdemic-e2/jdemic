package jdemic.ui.GameplayUI;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import jdemic.GameLogic.CityNode;
import jdemic.ui.GlowUtil;

public class DeckManager {

    private StackPane root;

    public DeckManager(StackPane root) { this.root = root; setupDecks(); }

    public void setupDecks() {
        HBox decks = new HBox(createCityDeck(), createEpidemicDeck());
        decks.setSpacing(30);

        StackPane wrapper = new StackPane(decks);
        wrapper.setPickOnBounds(false);
        wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        root.getChildren().add(wrapper);

        StackPane.setAlignment(wrapper, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(wrapper, new Insets(0, 30, 15, 0));
    }

    public StackPane createCardStack(Image topImage, int stackSize) {
        StackPane stack = new StackPane();
        stack.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        for (int i = 0; i < stackSize; i++) {
            ImageView card = new ImageView(topImage);
            card.setPreserveRatio(true);
            card.fitWidthProperty().bind(Bindings.createDoubleBinding(() -> Math.max(60, root.getWidth() * 0.06), root.widthProperty()));
            card.setTranslateX(i * 1);
            card.setTranslateY(-i * 1);
            card.setRotate((Math.random() - 0.5) * 2);
            stack.getChildren().add(card);
        }
        return stack;
    }

    public HBox createCityDeck() {
        Image verso = new Image(getClass().getResource("/cityCards/cityCardsVerso.png").toExternalForm());
        StackPane drawPile = createCardStack(verso, 6);
        Image topCard;
        try {
            String path = "/cityCards/BlueAtlanta.png"; //for test
            var res = getClass().getResource(path);
            topCard = (res != null) ? new Image(res.toExternalForm()) : verso;
        } catch (Exception e) { topCard = verso; }

        StackPane discardPile = createCardStack(topCard, 4);

        GlowUtil.applyGlow(drawPile, "#00d9ff", 8);
        GlowUtil.applyGlow(discardPile, "#00d9ff", 8);

        return new HBox(20, drawPile, discardPile);
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
            var resource = getClass().getResource(path);

            if (resource == null) { return new StackPane(new javafx.scene.control.Label(city.getName()));}
            card = new ImageView(new Image(resource.toExternalForm()));

        } catch (Exception e) { return new StackPane(new javafx.scene.control.Label(city.getName()));}

        card.setPreserveRatio(true);
        card.fitWidthProperty().bind(Bindings.createDoubleBinding(() -> Math.max(70, root.getWidth() * 0.06),root.widthProperty()));

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
        Image verso = new Image(getClass().getResource("/epidemicCards/epidemicCardsVerso.png").toExternalForm());
        StackPane drawPile = createCardStack(verso, 5);
        Image topCard;
        try {
            String path = "/epidemicCards/BlueAtlanta.png"; //for test
            var res = getClass().getResource(path);
            topCard = (res != null) ? new Image(res.toExternalForm()) : verso;
        } catch (Exception e) { topCard = verso; }

        StackPane discardPile = createCardStack(topCard, 3);
        GlowUtil.applyGlow(drawPile, "#ff2d2d", 8);
        GlowUtil.applyGlow(discardPile, "#ff2d2d", 8);

        return new HBox(20, drawPile, discardPile);
    }
}
