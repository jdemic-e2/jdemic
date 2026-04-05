package jdemic.ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;

public class ConfirmationOverlay {

    private StackPane root;

    public ConfirmationOverlay(StackPane parent, String message, String yesText, String noText, Runnable onYes, Runnable onNo) {
        root = new StackPane();

        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(parent.widthProperty());
        bg.heightProperty().bind(parent.heightProperty());
        bg.setFill(Color.rgb(0, 0, 0, 0.35));

        StackPane panelWrapper = new StackPane();
        StackPane.setAlignment(panelWrapper, Pos.CENTER);

        panelWrapper.prefWidthProperty().bind(parent.widthProperty().multiply(0.4));
        panelWrapper.prefHeightProperty().bind(parent.heightProperty().multiply(0.28));

        Region glow = new Region();
        glow.prefWidthProperty().bind(panelWrapper.prefWidthProperty());
        glow.prefHeightProperty().bind(panelWrapper.prefHeightProperty());

        glow.setStyle("-fx-background-color: transparent;" + "-fx-border-color: #00b5d4;" +"-fx-border-width: 2;" + "-fx-background-radius: 10;" + "-fx-border-radius: 10;");
        GlowUtil.applyGlow(glow, "#00b5d4", 15);

        StackPane panel = new StackPane();
        panel.prefWidthProperty().bind(panelWrapper.prefWidthProperty());
        panel.prefHeightProperty().bind(panelWrapper.prefHeightProperty());
        panelWrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        panelWrapper.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(panelWrapper, Pos.CENTER);
        panel.setStyle( "-fx-background-color: rgba(0,0,0,0.85);" + "-fx-border-color: #ffffff;" + "-fx-border-width: 2;" + "-fx-background-radius: 10;" + "-fx-border-radius: 10;");

        panelWrapper.getChildren().addAll(glow, panel);

        Label text = TextUtil.createText(message, "hkmodular", 0.02, "#ffff00", parent);
        text.setWrapText(true);
        text.setAlignment(Pos.CENTER);
        text.setTextAlignment(TextAlignment.CENTER);
        text.maxWidthProperty().bind(panel.widthProperty().multiply(0.9));
        text.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);

        ButtonsUtil yesBtn = new ButtonsUtil(yesText, "#ff0000", "black", "#ff0000", "#ff0000", 2, 10, 10,0.12, 0.06, 0.02, parent);
        yesBtn.setOnMouseClicked(e -> {
            parent.getChildren().remove(root);
            if (onYes != null) onYes.run();
        });

        ButtonsUtil noBtn = new ButtonsUtil(noText, "#ff0000", "black", "#ff0000", "#ff0000", 2, 10, 10,0.12, 0.06, 0.02,parent);
        noBtn.setOnMouseClicked(e -> {
            parent.getChildren().remove(root);
            if (onNo != null) onNo.run();
        });

        HBox buttons = new HBox();
        buttons.setAlignment(Pos.CENTER);
        buttons.spacingProperty().bind(panel.widthProperty().multiply(0.08));
        buttons.getChildren().addAll(noBtn, yesBtn);

        VBox layout = new VBox();
        layout.setAlignment(Pos.CENTER);

        layout.spacingProperty().bind(panel.heightProperty().multiply(0.12));
        layout.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(
                                        panel.getHeight() * 0.12,
                                        panel.getWidth() * 0.08,
                                        panel.getHeight() * 0.12,
                                        panel.getWidth() * 0.08),
                        panel.widthProperty(),
                        panel.heightProperty()
                )
        );
        layout.getChildren().addAll(text, buttons);
        panel.getChildren().add(layout);

        root.getChildren().addAll(bg, panelWrapper);
    }

    public StackPane getRoot() {
        return root;
    }
}