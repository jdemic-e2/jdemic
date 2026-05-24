package jdemic.ui.GameplayUI;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.GameManager;
import jdemic.ui.AnimationSpeedUtil;
import jdemic.ui.GlowUtil;
import jdemic.ui.SafeResourceLoader;
import jdemic.ui.TextUtil;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Queue;

public class OutbreakManager {

    private final StackPane root;
    private final GameManager gameManager;

    private VBox container;
    private StackPane[] nodes;
    private ImageView[] icons;
    private final int MAX_OUTBREAKS = 8;
    private final Queue<String> outbreakQueue = new ArrayDeque<>();
    private boolean showingOutbreak;

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

        URL iconUrl = getClass().getResource("/icons/outbreakicon.png");
        if (iconUrl == null) {
            System.err.println("[OutbreakManager] Missing outbreak icon resource.");
            return;
        }

        for (int i = 0; i < nodes.length; i++) {
            StackPane containerIcon = new StackPane();
            ImageView icon = new ImageView(SafeResourceLoader.loadImage(iconUrl));

            icon.fitWidthProperty().bind(Bindings.createDoubleBinding(
                    () -> Math.max(16, Math.min(28, root.getWidth() * 0.022)),
                    root.widthProperty()
            ));
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
        wrapper.prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(140, Math.min(260, root.getWidth() * 0.24)),
                root.widthProperty()
        ));

        GlowUtil.applyGlow(wrapper, "#00eaff", Math.max(8, root.getWidth() * 0.01));

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

    public void queueOutbreak(CityNode city) {
        if (city == null) {
            queueOutbreak("UNKNOWN CITY");
            return;
        }
        queueOutbreak(city.getName());
    }

    public void queueOutbreak(String cityName) {
        String displayCity = cityName == null || cityName.isBlank() ? "UNKNOWN CITY" : cityName.trim();
        outbreakQueue.add(displayCity);
        showNextOutbreak();
    }

    private void showNextOutbreak() {
        if (showingOutbreak || outbreakQueue.isEmpty()) {
            return;
        }

        showingOutbreak = true;
        String cityName = outbreakQueue.poll();

        Region overlay = new Region();
        overlay.prefWidthProperty().bind(root.widthProperty());
        overlay.prefHeightProperty().bind(root.heightProperty());
        overlay.setStyle("-fx-background-color: rgba(120,0,0,0.22);");
        overlay.setOpacity(0);
        overlay.setMouseTransparent(true);

        Label title = TextUtil.createText("Outbreak on\n" + cityName, "hkmodular", 0.040, "#ff2d2d", root);
        title.setTextAlignment(TextAlignment.CENTER);
        title.setAlignment(Pos.CENTER);
        title.setOpacity(0);
        title.setScaleX(0.65);
        title.setScaleY(0.65);
        GlowUtil.applyGlow(title, "#000000", 28);
        StackPane.setAlignment(title, Pos.CENTER);

        StackPane banner = new StackPane(title);
        banner.setMouseTransparent(true);
        banner.setPickOnBounds(false);
        banner.setTranslateY(root.getHeight() * -0.08);
        StackPane.setAlignment(banner, Pos.CENTER);

        root.getChildren().addAll(overlay, banner);
        overlay.toFront();
        banner.toFront();

        SequentialTransition full = new SequentialTransition(
                new ParallelTransition(
                        fade(overlay, 0.0, 1.0, 180),
                        fade(title, 0.0, 1.0, 180),
                        scale(title, 0.65, 1.05, 300),
                        translateY(banner, root.getHeight() * -0.13, root.getHeight() * -0.08, 300)
                ),
                new PauseTransition(Duration.millis(700)),
                new ParallelTransition(
                        fade(overlay, 1.0, 0.0, 260),
                        fade(title, 1.0, 0.0, 260),
                        scale(title, 1.05, 0.9, 260)
                )
        );

        full.setOnFinished(event -> {
            root.getChildren().removeAll(overlay, banner);
            showingOutbreak = false;
            showNextOutbreak();
        });
        AnimationSpeedUtil.play(full);
    }

    private FadeTransition fade(Node node, double from, double to, double millis) {
        FadeTransition transition = new FadeTransition(Duration.millis(millis), node);
        transition.setFromValue(from);
        transition.setToValue(to);
        return transition;
    }

    private ScaleTransition scale(Node node, double from, double to, double millis) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(millis), node);
        transition.setFromX(from);
        transition.setFromY(from);
        transition.setToX(to);
        transition.setToY(to);
        transition.setInterpolator(Interpolator.EASE_OUT);
        return transition;
    }

    private TranslateTransition translateY(Node node, double from, double to, double millis) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(millis), node);
        transition.setFromY(from);
        transition.setToY(to);
        transition.setInterpolator(Interpolator.EASE_OUT);
        return transition;
    }

    public VBox getContainer() { return container; }
    public void updateTrack() { updateUI(); }
}
