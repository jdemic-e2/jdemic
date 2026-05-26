package jdemic.ui.GameplayUI;

import javafx.animation.Interpolator;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import jdemic.ui.AnimationUiUtil;
import jdemic.ui.AnimationSpeedUtil;
import jdemic.ui.SafeResourceLoader;
import jdemic.GameLogic.PlayerRoles;

public class PawnUI {
    private final VBox root;
    private final ImageView pawnImage;
    private final String playerName;
    private final Label nameLabel;
    private final Label roleLabel;

    public PawnUI(String playerName, ReadOnlyDoubleProperty mapHeightProperty, String imagePath, PlayerRoles role) {
        this.playerName = playerName;
        this.root = new VBox(2);
        this.root.setAlignment(Pos.CENTER);

        nameLabel = new Label(playerName);
        nameLabel.setStyle("-fx-font-family: hkmodular; -fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #333333;");
        nameLabel.setMouseTransparent(true);

        roleLabel = new Label(role == null ? "" : formatRole(role));
        roleLabel.setStyle("-fx-font-family: hkmodular; -fx-font-size: 10px; -fx-text-fill: #333333;");
        roleLabel.setMouseTransparent(true);

        DropShadow labelShadow = new DropShadow();
        // Make the text glow whiter and a bit bigger for readability over the map
        labelShadow.setRadius(8);
        labelShadow.setSpread(0.75);
        labelShadow.setColor(Color.rgb(255,255,255,0.95));
        labelShadow.setOffsetX(0);
        labelShadow.setOffsetY(0);
        nameLabel.setEffect(labelShadow);
        roleLabel.setEffect(labelShadow);

        Image img = SafeResourceLoader.loadImage(imagePath);
        pawnImage = new ImageView(img);

        pawnImage.fitHeightProperty().bind(mapHeightProperty.multiply(0.08));
        pawnImage.setPreserveRatio(true);

        DropShadow glow = new DropShadow();
        // Make the pawn glow larger and whiter for better contrast on the map
        glow.setRadius(16);
        glow.setSpread(0.6);
        glow.setColor(Color.rgb(255,255,255,0.92));
        glow.setOffsetX(0);
        glow.setOffsetY(0);
        pawnImage.setEffect(glow);

        root.getChildren().addAll(nameLabel, roleLabel, pawnImage);
        this.root.setMouseTransparent(true);
    }

    private String formatRole(PlayerRoles role) {
        String s = role.name().toLowerCase().replace('_', ' ');
        String[] parts = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    public VBox getNode() {
        return root;
    }

    public void unbindPosition() {
        root.layoutXProperty().unbind();
        root.layoutYProperty().unbind();
    }

    public void bindToCenter(DoubleExpression cityX, DoubleExpression cityY) {
        root.layoutXProperty().bind(cityX.subtract(root.widthProperty().divide(2)));
        root.layoutYProperty().bind(cityY.subtract(root.heightProperty()));
    }

    public void bindWithOffset(DoubleExpression cityX, DoubleExpression cityY, double angle, double radiusOffset) {
        root.layoutXProperty().bind(cityX.add(Math.cos(angle) * radiusOffset).subtract(root.widthProperty().divide(2)));
        root.layoutYProperty().bind(cityY.add(Math.sin(angle) * radiusOffset).subtract(root.heightProperty()));
    }
    
    public void animateMoveTo(double targetX, double targetY, Runnable onFinished) {
        javafx.animation.ParallelTransition full = AnimationUiUtil.createMoveWithPulse(
                root,
                Duration.millis(850),
                root.getTranslateX(),
                root.getTranslateY(),
                targetX,
                targetY,
                1.18,
                Duration.millis(420),
                Interpolator.EASE_BOTH
        );
        full.setOnFinished(e -> {
            root.setTranslateX(0);
            root.setTranslateY(0);
            if (onFinished != null) { onFinished.run(); }
        });

        AnimationSpeedUtil.play(full);
    }

    public void animateMoveFrom(double startX, double startY, Runnable onFinished) {
        javafx.animation.ParallelTransition full = AnimationUiUtil.createMoveWithPulse(
                root,
                Duration.millis(850),
                startX,
                startY,
                0,
                0,
                1.18,
                Duration.millis(420),
                Interpolator.EASE_BOTH
        );
        full.setOnFinished(e -> {
            root.setTranslateX(0);
            root.setTranslateY(0);
            if (onFinished != null) { onFinished.run(); }
        });

        AnimationSpeedUtil.play(full);
    }

    public ImageView getImage() { return pawnImage; }
    public void setRole(PlayerRoles role) {
        if (role == null) {
            roleLabel.setText("");
        } else {
            roleLabel.setText(formatRole(role));
        }
    }
}
