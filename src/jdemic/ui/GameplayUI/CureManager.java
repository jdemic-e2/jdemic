package jdemic.ui.GameplayUI;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import jdemic.GameLogic.DiseaseColor;
import jdemic.GameLogic.GameManager;
import jdemic.ui.GlowUtil;
import jdemic.ui.TextUtil;
import javafx.scene.Node;

public class CureManager {
    private final StackPane root;
    private final GameManager gameManager;
    private VBox container;
    private HBox[] rows;
    private ImageView[] icons;
    private ImageView[] curedOverlays;

    public CureManager(StackPane root, GameManager gameManager) {
        this.root = root;
        this.gameManager = gameManager;
        setupUI();
        updateUI();
    }

    private void setupUI() {

        VBox content = new VBox();
        content.spacingProperty().bind(root.heightProperty().multiply(0.015));
        content.setAlignment(Pos.TOP_LEFT);

        Label title = TextUtil.createText("CURES FOUND", "hkmodular", 0.010, "#00d9ff", root);
        content.getChildren().add(title);

        DiseaseColor[] colors = DiseaseColor.values();
        rows = new HBox[colors.length];
        icons = new ImageView[colors.length];
        curedOverlays = new ImageView[colors.length];

        for (int i = 0; i < colors.length; i++) {

            HBox row = new HBox();
            row.spacingProperty().bind(root.widthProperty().multiply(0.01));
            row.setAlignment(Pos.CENTER_LEFT);

            StackPane iconContainer = new StackPane();
            ImageView icon = new ImageView();
            icon.fitWidthProperty().bind(Bindings.createDoubleBinding(() -> Math.max(30, root.getWidth() * 0.025), root.widthProperty()));
            icon.setPreserveRatio(true);

            String imagePath = switch (colors[i]) {
                case BLUE -> "/icons/IconVirusConficker.png";
                case YELLOW -> "/icons/IconVirusWannaCry.png";
                case BLACK -> "/icons/IconVirusZeus.png";
                case RED -> "/icons/IconVirusStuxnet.png";
            };

            var resource = getClass().getResource(imagePath);
            if (resource != null) {
                icon.setImage(new Image(resource.toExternalForm()));
            }

            ImageView curedIcon = new ImageView(new Image(getClass().getResource("/icons/curedIcon.png").toExternalForm()));

            curedIcon.fitWidthProperty().bind(icon.fitWidthProperty());
            curedIcon.setPreserveRatio(true);
            curedIcon.setVisible(false); // important

            iconContainer.getChildren().addAll(icon, curedIcon);

            String nameText = switch (colors[i]) {
                case BLUE -> "Conficker";
                case YELLOW -> "WannaCry";
                case BLACK -> "Zeus";
                case RED -> "Stuxnet";
            };

            Label name = TextUtil.createText(nameText,"hkmodular",0.008, "#ffffff", root);

            icon.setOnMouseEntered(e -> {
                icon.setScaleX(1.1);
                icon.setScaleY(1.1);
            });

            icon.setOnMouseExited(e -> {
                icon.setScaleX(1);
                icon.setScaleY(1);
            });

            row.getChildren().addAll(iconContainer, name);

            icons[i] = icon;
            curedOverlays[i] = curedIcon;
            rows[i] = row;

            content.getChildren().add(row);
        }

        content.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(
                                root.getHeight() * 0.02,
                                root.getWidth() * 0.015,
                                root.getHeight() * 0.02,
                                root.getWidth() * 0.015),
                        root.widthProperty(),
                        root.heightProperty()));

        StackPane wrapper = new StackPane(content);
        wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        wrapper.setStyle("-fx-background-color: rgba(0,0,0,1);" + "-fx-border-color: transparent transparent transparent #00eaff;" + "-fx-border-width: 0 0 0 2;");
        wrapper.prefWidthProperty().bind( Bindings.createDoubleBinding(() -> Math.max(120, Math.min(220, root.getWidth() * 0.14)), root.widthProperty()));
        GlowUtil.applyGlow(wrapper, "#00eaff", Math.max(8, root.getWidth() * 0.01));
        StackPane.setAlignment(wrapper, Pos.BOTTOM_RIGHT);
        wrapper.translateYProperty().bind(Bindings.createDoubleBinding(() -> -root.getHeight() * -0.21,root.heightProperty()));
        wrapper.translateXProperty().bind(Bindings.createDoubleBinding(() -> Math.max(30, root.getWidth() * 0.04),root.widthProperty()));

        container = new VBox(wrapper);
        container.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    }

    public void updateUI() {
        if (gameManager == null)
            return;

        DiseaseColor[] colors = DiseaseColor.values();

        for (int i = 0; i < colors.length; i++) {

            boolean cured = colors[i] == DiseaseColor.BLUE //debug conficker always cured-should have the curedIcon overlay
                    || gameManager.getState().getDiseaseManager().isCured(colors[i]);

            if (cured) {
                Color fxColor = getFxColor(colors[i]);
                icons[i].setOpacity(1.0);
                icons[i].setEffect(new DropShadow(25, fxColor));
                curedOverlays[i].setVisible(true);
            } else {
                icons[i].setOpacity(0.4);
                icons[i].setEffect(null);
                curedOverlays[i].setVisible(false);
            }
        }
    }

    private Color getFxColor(DiseaseColor color) {
        return switch (color) {
            case BLUE -> Color.web("#00b5d4");
            case YELLOW -> Color.web("#cfc900");
            case BLACK -> Color.web("#111111");
            case RED -> Color.web("#ff2d2d");
        };
    }

    public VBox getContainer() { return container; }
}