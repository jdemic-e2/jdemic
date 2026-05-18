package jdemic.ui.gameplay;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import jdemic.ui.GlowUtil;
import jdemic.ui.TextUtil;

/**
 * Player deck pile indicator (remaining draw pile count).
 */
public final class CardDeckView extends VBox {

    private final Label countLabel;

    public CardDeckView(int remainingCards, Region scaleRoot) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        setPickOnBounds(false);
        prefWidthProperty().bind(Bindings.max(96, scaleRoot.widthProperty().multiply(0.16)));
        minWidthProperty().bind(Bindings.max(88, scaleRoot.widthProperty().multiply(0.14)));
        maxWidthProperty().bind(scaleRoot.widthProperty().multiply(0.22));
        paddingProperty().bind(Bindings.createObjectBinding(
                () -> new Insets(
                        scaleRoot.getHeight() * 0.012,
                        scaleRoot.getWidth() * 0.018,
                        scaleRoot.getHeight() * 0.012,
                        scaleRoot.getWidth() * 0.018),
                scaleRoot.widthProperty(), scaleRoot.heightProperty()));

        setStyle(
                "-fx-background-color: linear-gradient(to bottom right, rgba(6,14,28,0.95), rgba(0,0,0,0.88)); "
                        + "-fx-border-color: #00d9ff; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");

        DropShadow deckGlow = new DropShadow();
        deckGlow.setColor(Color.color(0, 0.85, 1, 0.35));
        deckGlow.setRadius(14);
        setEffect(deckGlow);

        Label title = TextUtil.createText("PLAYER DECK", "hkmodular", 0.013, "#d1d412", scaleRoot);
        title.setWrapText(true);
        title.maxWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(40, getWidth() - 8),
                widthProperty()));
        GlowUtil.applyGlow(title, "#d1d412", 8);

        countLabel = TextUtil.createText(String.valueOf(remainingCards), "hkmodular", 0.026, "#00d9ff", scaleRoot);
        countLabel.maxWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(40, getWidth() - 8),
                widthProperty()));
        GlowUtil.applyGlow(countLabel, "#00d9ff", 10);
        Tooltip.install(this, new Tooltip("Cards remaining in the player draw pile."));

        getChildren().addAll(title, countLabel);
    }

    public void setRemainingCount(int n) {
        countLabel.setText(String.valueOf(n));
    }
}
