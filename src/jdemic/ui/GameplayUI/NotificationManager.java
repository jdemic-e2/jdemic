package jdemic.ui.GameplayUI;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class NotificationManager {

    private final VBox container;
    private static final int MAX_NOTIFICATIONS = 3;
    private static final int NOTIFICATION_DURATION = 2;
    public NotificationManager(VBox container)
    {
        this.container = container;
        this.container.setMouseTransparent(true);
    }

    public void showNotification(String message)
    {
        Platform.runLater(() -> {
            Label toast = new Label(message);

            toast.fontProperty().bind(Bindings.createObjectBinding(() ->
                            Font.font("System", FontWeight.BOLD, container.getHeight() * 0.02),
                    container.heightProperty()
            ));

            container.heightProperty().addListener((obs, oldVal, newVal) -> {
                double scale = newVal.doubleValue() / 1000.0;
                double paddingVertical = 10 * scale;
                double paddingHorizontal = 20 * scale;
                double borderWidth = 2 * scale;

                toast.setStyle(String.format(
                        "-fx-background-color: rgba(0, 181, 212, 0.5); " +
                                "-fx-text-fill: black; " +
                                "-fx-padding: %.1fpx %.1fpx; " +
                                "-fx-border-color: #00b5d4; " +
                                "-fx-border-width: %.1fpx; " +
                                "-fx-background-radius: %.1fpx; " +
                                "-fx-border-radius: %.1fpx;",
                        paddingVertical, paddingHorizontal, borderWidth, 5 * scale, 5 * scale
                ));
            });

            updateToastStyle(toast, container.getHeight());

            container.getChildren().add(0,toast);

            if (container.getChildren().size() > MAX_NOTIFICATIONS)
            {
                container.getChildren().remove(MAX_NOTIFICATIONS);
            }

            PauseTransition delay = new PauseTransition(Duration.seconds(NOTIFICATION_DURATION));
            delay.setOnFinished(e -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(1000),toast);
                fadeOut.setToValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(event -> container.getChildren().remove(toast));
                fadeOut.play();
            });
            delay.play();
        });

    }

    private void updateToastStyle(Label toast, double height) {
        double scale = Math.max(0.5, height / 1000.0);
        toast.setStyle(String.format(
                "-fx-background-color: rgba(0, 181, 212, 0.5); " +
                        "-fx-text-fill: black; " +
                        "-fx-padding: %.1fpx %.1fpx; " +
                        "-fx-border-color: #00b5d4; " +
                        "-fx-border-width: %.1fpx; " +
                        "-fx-background-radius: %.1fpx; " +
                        "-fx-border-radius: %.1fpx;",
                10 * scale, 20 * scale, 2 * scale, 5 * scale, 5 * scale
        ));
    }
}
