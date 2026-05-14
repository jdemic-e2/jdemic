package jdemic.ui.gameplay;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import jdemic.GameLogic.Card;
import jdemic.ui.FontUtil;

/**
 * Hand / overlay card tile with wrap, tooltip, and hover scale + glow (cyber theme).
 */
public final class CardView extends StackPane {

    private static final Duration HOVER_MS = Duration.millis(160);

    private final VBox inner;
    private final ScaleTransition hoverIn;
    private final ScaleTransition hoverOut;

    public CardView(Card card, Region scaleRoot) {
        this(card, scaleRoot, 0.125, 0.175);
    }

    /**
     * @param widthRatio  fraction of {@code scaleRoot} width for preferred card width
     * @param heightRatio fraction of {@code scaleRoot} height for preferred card height
     */
    public CardView(Card card, Region scaleRoot, double widthRatio, double heightRatio) {
        setAlignment(Pos.CENTER);
        getStyleClass().add("gameplay-card");

        DropShadow baseShadow = new DropShadow();
        baseShadow.setColor(Color.color(0, 0.85, 1, 0.45));
        baseShadow.setRadius(10);
        baseShadow.setOffsetY(3);

        inner = new VBox(6);
        inner.setAlignment(Pos.TOP_CENTER);
        inner.setEffect(baseShadow);
        inner.setStyle(
                "-fx-border-color: #00b5d4; -fx-border-radius: 10; -fx-background-radius: 10; "
                        + "-fx-border-width: 2; -fx-background-color: linear-gradient(to bottom, rgba(8,18,32,0.97), rgba(0,0,0,0.92)); "
                        + "-fx-padding: 10 10 12 10;");
        inner.prefWidthProperty().bind(scaleRoot.widthProperty().multiply(widthRatio));
        inner.prefHeightProperty().bind(scaleRoot.heightProperty().multiply(heightRatio));
        inner.minWidthProperty().bind(scaleRoot.widthProperty().multiply(widthRatio * 0.85));
        inner.minHeightProperty().bind(scaleRoot.heightProperty().multiply(heightRatio * 0.72));

        Label name = new Label(card.getCardName());
        name.setWrapText(true);
        name.setTextFill(Color.web("#ffffff"));
        name.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        name.setAlignment(Pos.CENTER);
        name.maxWidthProperty().bind(inner.widthProperty().subtract(8));
        name.prefWidthProperty().bind(inner.widthProperty().subtract(8));
        name.fontProperty().bind(Bindings.createObjectBinding(() -> {
            double w = scaleRoot.getWidth() * 0.022;
            double h = scaleRoot.getHeight() * 0.028;
            double size = Math.clamp(Math.min(w, h), 10, 22);
            return FontUtil.getFont("hkmodular", size);
        }, scaleRoot.widthProperty(), scaleRoot.heightProperty()));

        Label typeTag = new Label(card.getType().name().replace('_', ' '));
        typeTag.setTextFill(Color.web("#00d9ff", 0.85));
        typeTag.fontProperty().bind(Bindings.createObjectBinding(() -> {
            double size = Math.clamp(Math.min(scaleRoot.getWidth(), scaleRoot.getHeight()) * 0.016, 8, 14);
            return FontUtil.getFont("hkmodular", size);
        }, scaleRoot.widthProperty(), scaleRoot.heightProperty()));
        typeTag.setWrapText(true);
        typeTag.setMaxWidth(Double.MAX_VALUE);
        typeTag.setAlignment(Pos.CENTER);

        inner.getChildren().addAll(typeTag, name);
        getChildren().add(inner);

        String tip = card.getCardName() + "\n\n" + card.getEffectDescription();
        Tooltip tipUi = new Tooltip(tip);
        tipUi.setWrapText(true);
        tipUi.setMaxWidth(360);
        Tooltip.install(this, tipUi);

        DropShadow hoverShadow = new DropShadow();
        hoverShadow.setColor(Color.color(0.2, 1, 1, 0.75));
        hoverShadow.setRadius(22);
        hoverShadow.setOffsetY(6);

        hoverIn = new ScaleTransition(HOVER_MS, inner);
        hoverIn.setInterpolator(Interpolator.EASE_OUT);
        hoverIn.setToX(1.07);
        hoverIn.setToY(1.07);

        hoverOut = new ScaleTransition(HOVER_MS, inner);
        hoverOut.setInterpolator(Interpolator.EASE_OUT);
        hoverOut.setToX(1.0);
        hoverOut.setToY(1.0);

        setOnMouseEntered(e -> {
            hoverOut.stop();
            inner.setEffect(hoverShadow);
            hoverIn.setFromX(inner.getScaleX());
            hoverIn.setFromY(inner.getScaleY());
            hoverIn.playFromStart();
        });
        setOnMouseExited(e -> {
            hoverIn.stop();
            inner.setEffect(baseShadow);
            hoverOut.setFromX(inner.getScaleX());
            hoverOut.setFromY(inner.getScaleY());
            hoverOut.playFromStart();
        });

        setCursor(javafx.scene.Cursor.HAND);
    }
}
