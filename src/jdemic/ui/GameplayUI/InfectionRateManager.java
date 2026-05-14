package jdemic.ui.GameplayUI;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import jdemic.GameLogic.GameManager;
import jdemic.ui.GlowUtil;
import jdemic.ui.TextUtil;

public class InfectionRateManager {
    private final StackPane root;
    private final GameManager gameManager;
    private HBox trackBox;
    private StackPane[] slots;
    private final int[] rates;
    private VBox container;

    int infection_stage;

    public InfectionRateManager(StackPane root, GameManager gameManager)
    {
        this.root = root;
        this.gameManager = gameManager;
        this.rates = gameManager.getInfectionRateTrack();
        infection_stage = 1;
        setupTrack();
        updateTrack();
    }

    public void setupTrack() {
        VBox content = new VBox();
        content.setAlignment(Pos.TOP_CENTER);
        content.spacingProperty().bind(root.heightProperty().multiply(0.015));

        Label title = TextUtil.createText("INFECTION RATE", "hkmodular", 0.010, "#00d9ff", root);

        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        content.getChildren().add(title);

        trackBox = new HBox();
        trackBox.spacingProperty().bind(root.widthProperty().multiply(0.004));
        trackBox.setAlignment(Pos.CENTER);

        slots = new StackPane[rates.length];
        for (int i = 0 ; i < rates.length ; i++)
        {
            StackPane slot = new StackPane();

            Rectangle diamond = new Rectangle();
            diamond.widthProperty().bind(root.widthProperty().multiply(0.018));
            diamond.heightProperty().bind(root.widthProperty().multiply(0.018));
            diamond.setRotate(45);
            diamond.setFill(Color.rgb(20, 20, 20));
            diamond.setStroke(Color.web("#444444"));
            diamond.setStrokeWidth(1);

            Label label = new Label(String.valueOf(rates[i]));
            label.setStyle("-fx-text-fill: #666666; -fx-font-family: 'hkmodular';");

            label.fontProperty().bind(Bindings.createObjectBinding(() -> javafx.scene.text.Font.font("hkmodular", root.getHeight() * 0.018),root.heightProperty()));
            label.setRotate(0);
            slot.getChildren().addAll(diamond, label);

            slots[i] = slot;
            trackBox.getChildren().add(slot);
        }

        content.getChildren().add(trackBox);

        content.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(
                                root.getHeight() * 0.015,
                                root.getWidth() * 0.015,
                                root.getHeight() * 0.03,
                                root.getWidth() * 0.015),
                        root.widthProperty(),
                        root.heightProperty()));

        StackPane wrapper = new StackPane(content);
        wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        wrapper.setStyle("-fx-background-color: rgba(0,0,0,0.9);" + "-fx-border-color: transparent transparent transparent #00eaff;" + "-fx-border-width: 0 0 0 2;");
        wrapper.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> Math.max(140, Math.min(440, root.getWidth() * 0.28)),root.widthProperty()));
        GlowUtil.applyGlow(wrapper, "#00eaff", Math.max(8, root.getWidth() * 0.01));

        StackPane.setAlignment(wrapper, Pos.TOP_LEFT);
        wrapper.translateXProperty().bind(root.widthProperty().multiply(0.40));
        wrapper.translateYProperty().bind(root.heightProperty().multiply(-0.06));
        container = new VBox(wrapper);
        container.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    }

    public void updateTrack() {
        if (gameManager == null || gameManager.getState() == null) return;

        int currentIndex = gameManager.getState().getInfectionRate();

        for (int i = 0; i < slots.length; i++) {

            StackPane slot = slots[i];
            Rectangle diamond = (Rectangle) slot.getChildren().get(0);
            Label label = (Label) slot.getChildren().get(1);

            if (i == currentIndex) {
                diamond.setFill(Color.web("#b10606"));
                diamond.setStroke(Color.WHITE);
                label.setStyle("-fx-text-fill: white; -fx-font-family: 'hkmodular';");
                DropShadow glow = new DropShadow(20, Color.web("#ff8b8b"));
                diamond.setEffect(glow);

            } else if (i < currentIndex) {
                diamond.setFill(Color.web("#006a80"));
                diamond.setStroke(Color.web("#00eaff", 0.3));
                label.setStyle("-fx-text-fill: white; -fx-font-family: 'hkmodular';");
                diamond.setEffect(null);

            } else {
                diamond.setFill(Color.rgb(20, 20, 20));
                diamond.setStroke(Color.web("#444444"));
                label.setStyle("-fx-text-fill: #666666; -fx-font-family: 'hkmodular';");
                diamond.setEffect(null);
            }
        }
    }

    public int getInfection_stage()
    {
        return infection_stage;
    }

    public VBox getContainer()
    {
        return container; 
    }
}
