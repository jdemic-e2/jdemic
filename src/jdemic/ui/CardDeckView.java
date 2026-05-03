package jdemic.ui;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom deck strip: horizontally centered row of fixed-size cards.
 * Styled like the rest of the app (inline {@code setStyle} + effects), no external stylesheet.
 */
public class CardDeckView extends StackPane {
    private static final double CARD_W = 170;
    private static final double CARD_H = 245;
    private static final double PANEL_MIN_H = 300;
    private static final String FONT = "hkmodular";

    private final ObservableList<Card> cards = FXCollections.observableArrayList();
    private final javafx.beans.property.ObjectProperty<CardView> selectedView =
            new javafx.beans.property.SimpleObjectProperty<>(null);

    private final StackPane panel = new StackPane();
    private final HBox row = new HBox();
    private final StackPane rowHost = new StackPane();
    private final ScrollPane scroller = new ScrollPane();

    private final javafx.beans.property.SimpleDoubleProperty cardWidth =
            new javafx.beans.property.SimpleDoubleProperty(CARD_W);
    private final javafx.beans.property.SimpleDoubleProperty cardHeight =
            new javafx.beans.property.SimpleDoubleProperty(CARD_H);
    private final javafx.beans.property.SimpleDoubleProperty selectedTranslateY =
            new javafx.beans.property.SimpleDoubleProperty(-22);

    private final Duration animDuration = Duration.millis(150);

    public CardDeckView(javafx.beans.value.ObservableNumberValue sceneWidth,
                        javafx.beans.value.ObservableNumberValue sceneHeight) {
        setPickOnBounds(false);
        StackPane.setAlignment(panel, Pos.CENTER);

        panel.maxWidthProperty().bind(Bindings.min(Bindings.multiply(sceneWidth, 0.92), 980));
        panel.prefWidthProperty().bind(panel.maxWidthProperty());
        panel.setMinHeight(PANEL_MIN_H);
        panel.prefHeightProperty().bind(Bindings.max(PANEL_MIN_H, Bindings.multiply(sceneHeight, 0.26)));

        buildPanel();
        getChildren().add(panel);

        cards.addListener((ListChangeListener<Card>) c -> rebuild());
    }

    private void buildPanel() {
        panel.setStyle(
                "-fx-background-color: rgba(20,20,25,0.72);" +
                "-fx-background-radius: 18;" +
                "-fx-border-radius: 18;" +
                "-fx-border-color: rgba(255,255,255,0.18);" +
                "-fx-border-width: 1;"
        );
        DropShadow deckShadow = new DropShadow();
        deckShadow.setColor(Color.rgb(0, 0, 0, 0.55));
        deckShadow.setRadius(26);
        deckShadow.setSpread(0.18);
        deckShadow.setOffsetY(8);
        panel.setEffect(deckShadow);

        panel.setPadding(new Insets(24));

        row.setAlignment(Pos.CENTER);
        row.setSpacing(22);
        row.setPickOnBounds(false);

        rowHost.getChildren().add(row);
        StackPane.setAlignment(row, Pos.CENTER);
        rowHost.prefWidthProperty().bind(scroller.widthProperty());
        rowHost.minHeightProperty().bind(scroller.heightProperty());

        scroller.setContent(rowHost);
        scroller.setFitToHeight(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setPannable(true);
        scroller.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroller.setPadding(Insets.EMPTY);

        Label hint = new Label("YOUR DECK");
        hint.setStyle(
                "-fx-font-family: \"" + FONT + "\";" +
                "-fx-font-size: 12px;" +
                "-fx-text-fill: rgba(255,255,255,0.65);"
        );
        StackPane.setAlignment(hint, Pos.TOP_LEFT);
        StackPane.setMargin(hint, new Insets(8, 8, 0, 8));

        StackPane.setAlignment(scroller, Pos.CENTER);
        panel.getChildren().addAll(scroller, hint);
    }

    private void rebuild() {
        row.getChildren().clear();

        if (cards.isEmpty()) {
            Label empty = new Label("NO CARDS");
            empty.setStyle(
                    "-fx-font-family: \"" + FONT + "\";" +
                    "-fx-font-size: 13px;" +
                    "-fx-text-fill: rgba(255,255,255,0.75);" +
                    "-fx-padding: 16 0 0 0;"
            );
            row.getChildren().add(empty);
            selectedView.set(null);
            return;
        }

        for (Card card : cards) {
            CardView view = new CardView(card, cardWidth, cardHeight, selectedTranslateY, animDuration);
            view.setOnSelected(this::select);
            row.getChildren().add(view);
        }

        if (selectedView.get() != null) {
            Card prev = selectedView.get().getCard();
            for (var n : row.getChildren()) {
                if (n instanceof CardView cv && cv.getCard() == prev) {
                    select(cv);
                    return;
                }
            }
            selectedView.set(null);
        }
    }

    private void select(CardView next) {
        CardView prev = selectedView.get();
        if (prev == next) return;
        if (prev != null) prev.setSelected(false);
        selectedView.set(next);
        if (next != null) next.setSelected(true);
    }

    public ObservableList<Card> getCards() {
        return cards;
    }

    public void setCards(List<Card> list) {
        cards.setAll(list == null ? List.of() : list);
    }

    public void setDummyCardsIfEmpty() {
        if (!cards.isEmpty()) return;
        List<Card> demo = new ArrayList<>();
        demo.add(new Card("Atlanta", CardType.CITY, null));
        Card threat = new Card("Threat Scan", CardType.EVENT, null);
        threat.setEventType(Card.EventType.THREAT);
        demo.add(threat);
        demo.add(new Card("System Breach", CardType.EPIDEMIC, null));
        demo.add(new Card("Tokyo", CardType.CITY, null));
        demo.add(new Card("Infection: Lima", CardType.INFECTION, null));
        setCards(demo);
    }
}
