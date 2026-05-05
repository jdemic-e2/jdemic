package jdemic.ui.GameplayUI;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import jdemic.GameLogic.GameManager;
import jdemic.ui.GlowUtil;
import jdemic.ui.TextUtil;

public class OutbreakManager {

    private final StackPane root;
    private final GameManager gameManager;

    private VBox container;
    private StackPane[] nodes;
    private ImageView[] icons;
    private final int MAX_OUTBREAKS = 8;

    public OutbreakManager(StackPane root, GameManager gameManager) {
        this.root = root;
        this.gameManager = gameManager;
        setupUI();
        updateUI();
    }

    private void setupUI() {

        VBox content = new VBox();
        content.spacingProperty().bind(root.heightProperty().multiply(0.015));
        content.setAlignment(Pos.TOP_CENTER);

        Label title = TextUtil.createText("OUTBREAKS", "hkmodular", 0.010, "#00d9ff", root);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);
        content.getChildren().add(title);

        HBox row = new HBox();
        row.spacingProperty().bind(root.widthProperty().multiply(0.005));
        row.setAlignment(Pos.CENTER);

        nodes = new StackPane[MAX_OUTBREAKS + 1];
        icons = new ImageView[MAX_OUTBREAKS + 1];

        for (int i = 0; i < nodes.length; i++) {
            StackPane containerIcon = new StackPane();
            ImageView icon = new ImageView(new Image(getClass().getResource("/icons/outbreakIcon.png").toExternalForm()));

            icon.fitWidthProperty().bind(Bindings.createDoubleBinding(() -> Math.max(28, root.getWidth() * 0.025),root.widthProperty()));
            icon.setPreserveRatio(true);
            icon.setOpacity(0.2);
            containerIcon.getChildren().add(icon);

            nodes[i] = containerIcon;
            icons[i] = icon;
            row.getChildren().add(containerIcon);
        }

        content.getChildren().add(row);

        content.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(
                                root.getHeight() * 0.015,
                                root.getWidth() * 0.015,
                                root.getHeight() * 0.015,
                                root.getWidth() * 0.015),
                        root.widthProperty(),
                        root.heightProperty()));

        StackPane wrapper = new StackPane(content);
        wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        wrapper.setStyle("-fx-background-color: rgba(0,0,0,0.9);" + "-fx-border-color: transparent transparent transparent #00eaff;" + "-fx-border-width: 0 0 0 2;");
        wrapper.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> Math.max(120, Math.min(220, root.getWidth() * 0.14)), root.widthProperty()));

        GlowUtil.applyGlow(wrapper, "#00eaff", Math.max(8, root.getWidth() * 0.01));

        StackPane.setAlignment(wrapper, Pos.TOP_LEFT);
        wrapper.translateXProperty().bind(root.widthProperty().multiply(0.32));
        wrapper.translateYProperty().bind(root.heightProperty().multiply(-0.06));

        container = new VBox(wrapper);
        container.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    }

    public void updateUI() {
        if (gameManager == null || gameManager.getState() == null) return;
        int outbreaks = gameManager.getState().getDiseaseManager().getOutbreakScore();

        for (int i = 0; i < nodes.length; i++) {
            if (i <= outbreaks) {
                icons[i].setOpacity(1.0);
                icons[i].setScaleX(1.1);
                icons[i].setScaleY(1.1);
                DropShadow glow = new DropShadow(20, Color.RED);
                icons[i].setEffect(glow);

            } else {
                icons[i].setOpacity(0.2);
                icons[i].setEffect(null);
            }
        }
    }

    public VBox getContainer() { return container; }
    public void updateTrack() { updateUI(); }
}