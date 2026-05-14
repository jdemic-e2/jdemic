package jdemic.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;

public class ButtonsUtil extends StackPane {

    private Label label;
    private Rectangle border;

    public ButtonsUtil(
            String text,
            String textColor,
            String backgroundColor,
            String borderColor,
            String glowColor,
            double borderWidth,
            double cornerRadius,
            double glowRadius,
            double widthRatio,
            double heightRatio,
            double fontRatio,
            StackPane root
    ) {
        setPickOnBounds(false);

        // Bind size to scene
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                prefWidthProperty().bind(newScene.widthProperty().multiply(widthRatio));
                prefHeightProperty().bind(newScene.heightProperty().multiply(heightRatio));
                setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            }
        });

        // Set the background style
        setStyle("-fx-background-color: " + backgroundColor + ";" +
                "-fx-background-radius: " + cornerRadius + ";" +
                "-fx-cursor: hand;");

        // Main button glow
        DropShadow baseGlow = new DropShadow();
        baseGlow.setColor(Color.web(glowColor));
        baseGlow.setRadius(glowRadius);
        setEffect(baseGlow);

        // Create the text
        label = TextUtil.createText(text, "hkmodular", fontRatio, textColor, root);
        label.setMouseTransparent(true);

        // --- FIXED BORDER LOGIC ---
        // Instead of 4 lines, we use 1 Rounded Rectangle
        border = new Rectangle();
        border.setStrokeWidth(borderWidth);
        border.setFill(Color.TRANSPARENT);
        border.setStrokeType(StrokeType.INSIDE); // Keeps border from leaking outside bounds

        // Match the background curves exactly
        border.setArcWidth(cornerRadius * 2);
        border.setArcHeight(cornerRadius * 2);

        // Bind size to the button size
        border.widthProperty().bind(widthProperty());
        border.heightProperty().bind(heightProperty());

        // Cyberpunk Gradient Stroke
        border.setStroke(new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web(glowColor, 0.4)),
                new Stop(0.5, Color.WHITE),
                new Stop(1.0, Color.web(glowColor, 0.4))
        ));

        border.setMouseTransparent(true);

        // Apply secondary glow specifically to the border line
        GlowUtil.applyGlow(border, glowColor, glowRadius * 0.8);

        getChildren().addAll(border, label);
        setAlignment(Pos.CENTER);

        // --- ANIMATIONS ---
        setOnMouseEntered(e -> {
            setScaleX(1.07);
            setScaleY(1.07);
            baseGlow.setColor(Color.web("#cfc900"));
            baseGlow.setRadius(glowRadius * 2);
        });

        setOnMouseExited(e -> {
            setScaleX(1);
            setScaleY(1);
            baseGlow.setRadius(glowRadius);
            baseGlow.setColor(Color.web(glowColor));
        });

        setOnMousePressed(e -> {
            setScaleX(0.94);
            setScaleY(0.94);
            baseGlow.setColor(Color.web("#cfc900"));
            baseGlow.setRadius(glowRadius * 2);
        });

        setOnMouseReleased(e -> {
            setScaleX(1.07);
            setScaleY(1.07);
            baseGlow.setColor(Color.web(glowColor));
            baseGlow.setRadius(glowRadius * 1.6);
        });
    }

    public void setText(String text) {
        label.setText(text);
    }

    /**
     * Disables the button and applies a greyed-out effect
     */
    public void setActionDisabled(boolean disabled) {
        setMouseTransparent(disabled);
        setOpacity(disabled ? 0.5 : 1.0);
        
        if (disabled) {
            if (getEffect() instanceof DropShadow) {
                ((DropShadow) getEffect()).setRadius(0);
            }
            border.setStroke(Color.GRAY);
        } else {
            if (getEffect() instanceof DropShadow) {
                ((DropShadow) getEffect()).setRadius(5);
            }
            border.setStroke(new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#00b5d4", 0.4)),
                new Stop(0.5, Color.WHITE),
                new Stop(1.0, Color.web("#00b5d4", 0.4))
            ));
        }
    }

    /**
     * Returns whether this button is currently action-disabled
     */
    public boolean isActionDisabled() {
        return isMouseTransparent();
    }
}