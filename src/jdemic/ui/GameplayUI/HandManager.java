package jdemic.ui.GameplayUI;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.PandemicMapGraph;
import jdemic.ui.GlowUtil;
import javafx.scene.Node;
import java.util.*;

public class HandManager {

    private StackPane root;
    private DeckManager deckManager;

    private HBox handContainer;

    public HandManager(StackPane root, DeckManager deckManager) {
        this.root = root;
        this.deckManager = deckManager;
        setupPlayerHand();
    }
    private void generateRandomCards(HBox container) {
        PandemicMapGraph graph = new PandemicMapGraph();
        List<CityNode> cities = new ArrayList<>(graph.getCityList());
        System.out.println("Total cities: " + cities.size()); //debu
        Collections.shuffle(cities);
        int count = Math.min(7, cities.size());
        System.out.println("Generating: " + count + " cards"); //debug

        for (int i = 0; i < count; i++) {
            CityNode city = cities.get(i);
            System.out.println("Card " + i + ": " + city.getName()); //debug
            StackPane card = deckManager.createCityCard(city);
            container.getChildren().add(card);
        }
        System.out.println("Children in container: " + container.getChildren().size()); //debug
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

        StackPane wrapper = createGlowBox(handContainer, "#00b5d4", 15);

        wrapper.prefWidthProperty().bind(
                Bindings.createDoubleBinding(() -> {
                    int count = handContainer.getChildren().size();
                    if (count == 0) return 200.0;

                    double cardWidth = Math.max(70, root.getWidth() * 0.06);
                    double spacing = handContainer.getSpacing();

                    return cardWidth + (count - 1) * (cardWidth + spacing) + cardWidth * 0.3;

                }, root.widthProperty(), handContainer.spacingProperty(), Bindings.size(handContainer.getChildren()))
        );

        wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane.setAlignment(wrapper, Pos.BOTTOM_LEFT);
        StackPane.setMargin(wrapper,new Insets(0, 0, root.getHeight() * 0.02, root.getWidth() * 0.02));

        root.getChildren().add(wrapper);

        generateRandomCards(handContainer);
    }

    private StackPane createGlowBox(Node content, String color, double glowRadius) {
        StackPane box = new StackPane(content);
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.setStyle("-fx-background-color: black;" + "-fx-border-color: transparent " + color + " transparent transparent;" + "-fx-border-width: 0 1 0 0;");
        GlowUtil.applyGlow(box, color, glowRadius);
        return box;
    }
}