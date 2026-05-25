package jdemic.ui;

import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CardType;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Fixed-size collectible card layout (170x245). Styling matches the rest of the app (inline setStyle strings + effects).
 */
public class CardView extends StackPane {
    private static final String FONT = "hkmodular";

    private final Card gameCard;
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private Consumer<CardView> onSelected;

    private final Duration animDuration;
    private final DoubleProperty selectedTranslateY;

    private StackPane frame;
    private Label titleLabel;
    private Label glyphLabel;

    private final DropShadow frameShadowBase = new DropShadow();
    private final DropShadow frameShadowHover = new DropShadow();
    private final DropShadow frameShadowSelected = new DropShadow();

    private String titleStyleNormal;
    private String titleStyleBright;

    public CardView(Card card, DoubleProperty cardWidth, DoubleProperty cardHeight,
                    DoubleProperty selectedTranslateY, Duration animDuration) {
        this.gameCard = Objects.requireNonNull(card, "card");
        this.selectedTranslateY = Objects.requireNonNull(selectedTranslateY, "selectedTranslateY");
        this.animDuration = Objects.requireNonNull(animDuration, "animDuration");

        initFrameShadows();

        setPickOnBounds(false);
        setCursor(Cursor.HAND);
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: transparent;");

        prefWidthProperty().bind(cardWidth);
        prefHeightProperty().bind(cardHeight);
        minWidthProperty().bind(cardWidth);
        minHeightProperty().bind(cardHeight);
        maxWidthProperty().bind(cardWidth);
        maxHeightProperty().bind(cardHeight);

        getChildren().add(buildContent());

        installTooltip();
        installHoverAnimation();
        installSelectAnimation();
        refreshFrameEffect();
        refreshTitleStyle();

        setOnMouseClicked(e -> {
            if (onSelected != null) onSelected.accept(this);
        });
    }

    private void initFrameShadows() {
        frameShadowBase.setColor(Color.rgb(0, 0, 0, 0.55));
        frameShadowBase.setRadius(18);
        frameShadowBase.setSpread(0.22);
        frameShadowBase.setOffsetY(10);

        frameShadowHover.setColor(Color.rgb(0, 217, 255, 0.20));
        frameShadowHover.setRadius(22);
        frameShadowHover.setSpread(0.24);

        frameShadowSelected.setColor(Color.rgb(0, 217, 255, 0.40));
        frameShadowSelected.setRadius(28);
        frameShadowSelected.setSpread(0.25);
    }

    private StackPane buildContent() {
        frame = new StackPane();
        frame.setStyle(baseFrameStyleForType());

        StackPane surface = new StackPane();
        surface.setStyle(
                "-fx-background-radius: 20;" +
                "-fx-background-color: linear-gradient(to bottom, rgba(25,28,36,0.98), rgba(10,12,16,0.98));"
        );
        surface.setPadding(new Insets(10, 12, 10, 12));

        VBox layout = new VBox(7);
        layout.setFillWidth(true);
        layout.setAlignment(Pos.TOP_CENTER);

        titleLabel = new Label(gameCard.getCardName() == null ? "UNKNOWN" : gameCard.getCardName().toUpperCase());
        titleStyleNormal = labelBase(FONT, 15, "rgba(255,255,255,0.92)");
        titleStyleBright = labelBase(FONT, 15, "rgba(255,255,255,0.98)");
        titleLabel.setStyle(titleStyleNormal);
        titleLabel.setWrapText(false);
        titleLabel.setMaxWidth(146);
        titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        titleLabel.setAlignment(Pos.CENTER);

        Label typeBadge = new Label(safeTypeLabel());
        typeBadge.setStyle(
                labelBase(FONT, 14, "rgba(255,255,255,0.92)") +
                "-fx-padding: 4 10 4 10;" +
                "-fx-background-radius: 12;" +
                "-fx-border-radius: 12;" +
                "-fx-border-width: 1;" +
                "-fx-border-color: rgba(255,255,255,0.12);" +
                "-fx-background-color: rgba(0,0,0,0.30);"
        );
        typeBadge.setWrapText(false);
        typeBadge.setMaxWidth(146);
        typeBadge.setAlignment(Pos.CENTER);

        StackPane visual = new StackPane();
        visual.setStyle(
                "-fx-background-radius: 18;" +
                "-fx-background-color: rgba(0,0,0,0.26);" +
                "-fx-border-radius: 18;" +
                "-fx-border-color: rgba(255,255,255,0.08);" +
                "-fx-border-width: 1;"
        );
        visual.setPrefSize(146, 72);
        visual.setMaxSize(146, 72);
        glyphLabel = new Label(visualGlyph());
        glyphLabel.setStyle(labelBase(FONT, 14, glyphColorForType()) + "-fx-alignment: center;");
        visual.getChildren().add(glyphLabel);

        Label shortDesc = new Label(shortEffectPreview());
        shortDesc.setStyle(labelBase(FONT, 11, "rgba(255,255,255,0.84)"));
        shortDesc.setWrapText(true);
        shortDesc.setMaxWidth(146);
        shortDesc.setMinHeight(48);
        shortDesc.setMaxHeight(58);
        shortDesc.setTextOverrun(OverrunStyle.CLIP);

        Label footer = new Label(safeSubtitle());
        footer.setStyle(labelBase(FONT, 11, "rgba(255,255,255,0.68)"));
        footer.setWrapText(false);
        footer.setMaxWidth(146);
        footer.setTextOverrun(OverrunStyle.ELLIPSIS);
        footer.setAlignment(Pos.CENTER);

        layout.getChildren().addAll(titleLabel, typeBadge, visual, shortDesc, footer);
        surface.getChildren().add(layout);

        InnerShadow inner = new InnerShadow(12, Color.web("rgba(0,0,0,0.55)"));
        inner.setChoke(0.08);
        surface.setEffect(inner);

        frame.getChildren().add(surface);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        clip.setArcWidth(22);
        clip.setArcHeight(22);
        frame.setClip(clip);

        return frame;
    }

    private static String labelBase(String font, int sizePx, String textFill) {
        return "-fx-font-family: \"" + font + "\";" +
                "-fx-font-size: " + sizePx + "px;" +
                "-fx-text-fill: " + textFill + ";";
    }

    private String baseFrameStyleForType() {
        return "-fx-background-radius: 22;" +
                "-fx-border-radius: 22;" +
                "-fx-border-width: 1.6;" +
                "-fx-border-color: " + borderGradientForType() + ";" +
                "-fx-background-color: transparent;";
    }

    private String borderGradientForType() {
        final CardType kind = gameCard.getType();
        if (kind == null) {
            return "rgba(255,255,255,0.12)";
        }
        switch (kind) {
            case CITY:
                return "linear-gradient(to bottom, rgba(0,217,255,0.95), rgba(0,122,255,0.75))";
            case EVENT:
                return "linear-gradient(to bottom, rgba(255,214,0,0.92), rgba(255,122,0,0.70))";
            case EPIDEMIC:
                return "linear-gradient(to bottom, rgba(255,54,96,0.95), rgba(255,0,0,0.70))";
            case INFECTION:
                return "linear-gradient(to bottom, rgba(178,120,255,0.92), rgba(255,0,255,0.55))";
            default:
                return "rgba(255,255,255,0.12)";
        }
    }

    private String glyphColorForType() {
        final CardType kind = gameCard.getType();
        if (kind == null) {
            return "rgba(255,255,255,0.90)";
        }
        switch (kind) {
            case CITY:
                return "rgba(0,217,255,0.92)";
            case EVENT:
                return "rgba(255,214,0,0.92)";
            case EPIDEMIC:
                return "rgba(255,54,96,0.92)";
            case INFECTION:
                return "rgba(178,120,255,0.92)";
            default:
                return "rgba(255,255,255,0.90)";
        }
    }

    private void installTooltip() {
        Tooltip tip = new Tooltip();
        String name = gameCard.getCardName() == null ? "-" : gameCard.getCardName();
        String type = safeTypeLabel();
        String full = safeDescription();
        tip.setText(name + "\nType: " + type + "\n\n" + full);
        tip.setWrapText(true);
        tip.setMaxWidth(420);
        tip.setShowDelay(Duration.millis(100));
        tip.setShowDuration(Duration.seconds(30));
        tip.setStyle(
                "-fx-background-color: rgba(18,18,24,0.96);" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-size: 12px;" +
                "-fx-padding: 12;" +
                "-fx-background-radius: 10;"
        );
        Tooltip.install(this, tip);
    }

    private String visualGlyph() {
        final CardType kind = gameCard.getType();
        if (kind == null) {
            return "◆";
        }
        switch (kind) {
            case CITY:
                return "⌖";
            case EVENT:
                return "⎔";
            case EPIDEMIC:
                return "☣";
            case INFECTION:
                return "◎";
            default:
                return "◆";
        }
    }

    private String shortEffectPreview() {
        String d = safeDescription();
        if (d.length() <= 90) return d;
        return d.substring(0, 87) + "…";
    }

    private String safeSubtitle() {
        final CardType kind = gameCard.getType();
        if (kind == null) {
            return "CARD";
        }
        switch (kind) {
            case CITY:
                return "CITY NODE";
            case EVENT:
                return "GLOBAL EVENT";
            case EPIDEMIC:
                return "SYSTEM BREACH";
            case INFECTION:
                return "INFECTION VECTOR";
            default:
                return "CARD";
        }
    }

    private String safeTypeLabel() {
        try {
            return gameCard.getType() == null ? "CARD" : gameCard.getType().toString();
        } catch (Exception e) {
            return "CARD";
        }
    }

    private String safeDescription() {
        try {
            String d = gameCard.getEffectDescription();
            if (d == null || d.isBlank()) return "-";
            return d;
        } catch (Exception e) {
            return "-";
        }
    }

    private void refreshFrameEffect() {
        if (frame == null) return;
        if (selected.get()) {
            frame.setEffect(frameShadowSelected);
        } else if (isHover()) {
            frame.setEffect(frameShadowHover);
        } else {
            frame.setEffect(frameShadowBase);
        }
    }

    private void refreshTitleStyle() {
        if (titleLabel == null) return;
        titleLabel.setStyle(selected.get() ? titleStyleBright : titleStyleNormal);
    }

    private void installHoverAnimation() {
        ScaleTransition hoverIn = new ScaleTransition(animDuration, this);
        hoverIn.setInterpolator(Interpolator.EASE_OUT);
        hoverIn.setToX(1.02);
        hoverIn.setToY(1.02);

        ScaleTransition hoverOut = new ScaleTransition(animDuration, this);
        hoverOut.setInterpolator(Interpolator.EASE_OUT);
        hoverOut.setToX(1.0);
        hoverOut.setToY(1.0);

        setOnMouseEntered(e -> {
            if (!selected.get()) {
                hoverOut.stop();
                hoverIn.playFromStart();
            }
            refreshFrameEffect();
        });
        setOnMouseExited(e -> {
            if (!selected.get()) {
                hoverIn.stop();
                hoverOut.playFromStart();
            }
            refreshFrameEffect();
        });
    }

    private void installSelectAnimation() {
        selected.addListener((obs, oldV, newV) -> {
            double targetY = newV ? selectedTranslateY.get() : 0.0;
            double targetScale = newV ? 1.03 : 1.0;

            TranslateTransition move = new TranslateTransition(animDuration, this);
            move.setInterpolator(Interpolator.EASE_OUT);
            move.setToY(targetY);

            ScaleTransition scale = new ScaleTransition(animDuration, this);
            scale.setInterpolator(Interpolator.EASE_OUT);
            scale.setToX(targetScale);
            scale.setToY(targetScale);

            new ParallelTransition(move, scale).playFromStart();

            refreshFrameEffect();
            refreshTitleStyle();
        });
    }

    public Card getCard() {
        return gameCard;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean value) {
        selected.set(value);
    }

    public void setOnSelected(Consumer<CardView> onSelected) {
        this.onSelected = onSelected;
    }
}
