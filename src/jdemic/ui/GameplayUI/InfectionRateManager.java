package jdemic.ui.GameplayUI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import jdemic.GameLogic.GameManager;

public class InfectionRateManager {
    private final StackPane root;
    private final GameManager gameManager;
    private HBox trackBox;
    private StackPane[] slots;
    private final int[] rates;
    int infection_stage;

    public InfectionRateManager(StackPane root, GameManager gameManager)
    {
        this.root = root;
        this.gameManager = gameManager;
        this.rates = gameManager.getInfectionRateTrack();
        infection_stage=1;
        setupTrack();
        updateTrack();
    }

    public void setupTrack()
    {
        trackBox = new HBox(0);
        trackBox.setAlignment(Pos.TOP_CENTER);
        trackBox.setPickOnBounds(false);
        trackBox.setMouseTransparent(true);

        trackBox.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        StackPane.setAlignment(trackBox, Pos.TOP_CENTER);

        trackBox.paddingProperty().bind(javafx.beans.binding.Bindings.createObjectBinding(
                () -> new Insets(root.getHeight() * 0.01, 0, 0, 0), root.heightProperty()
        ));

        slots = new StackPane[rates.length];
        for (int i = 0 ; i < rates.length ; i++)
        {
            StackPane slot = new StackPane();
            Rectangle bg = new Rectangle(45,40);
            bg.setStroke(Color.web("#333333"));
            bg.setStrokeWidth(1);
            bg.setFill(Color.rgb(20,20,20));

            Label label = new Label(String.valueOf(rates[i]));
            label.setStyle("-fx-text-fill: #555555; -fx-font-family: 'hkmodular'; -fx-font-size: 18px;");

            slot.getChildren().addAll(bg, label);
            slots[i] = slot;
            trackBox.getChildren().add(slot);
        }

        root.getChildren().add(trackBox);
    }

    public void updateTrack() {
        if (gameManager == null || gameManager.getState() == null) return;

        int currentIndex = gameManager.getState().getInfectionRate();

        for (int i = 0; i < slots.length; i++) {
            StackPane segment = slots[i];
            Rectangle bg = (Rectangle) segment.getChildren().get(0);
            Label label = (Label) segment.getChildren().get(1);

            if (i == currentIndex) {
                //Active
                bg.setFill(Color.web("#ff0000")); // Roșu pur
                bg.setStroke(Color.WHITE);
                label.setStyle("-fx-text-fill: black; -fx-font-family: 'hkmodular'; -fx-font-weight: bold;");

                DropShadow glow = new DropShadow();
                glow.setColor(Color.web("#ff0000"));
                glow.setRadius(20);
                bg.setEffect(glow);
            } else if (i < currentIndex) {
                //Filled
                bg.setFill(Color.web("#8b0000"));
                bg.setStroke(Color.web("#ff0000", 0.3));
                label.setStyle("-fx-text-fill: white; -fx-font-family: 'hkmodular';");
                bg.setEffect(null);
            } else {
                //Empty
                bg.setFill(Color.rgb(30, 30, 30));
                bg.setStroke(Color.web("#444444"));
                label.setStyle("-fx-text-fill: #666666; -fx-font-family: 'hkmodular';");
                bg.setEffect(null);
            }
        }
    }

    public int getInfection_stage()
    {
        return infection_stage;
    }
}
