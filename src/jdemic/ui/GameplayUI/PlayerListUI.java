package jdemic.ui.GameplayUI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import java.util.List;

public class PlayerListUI {
    private final VBox container;

    public PlayerListUI(List<PlayerState> players, Color[] playerColors) {
        this.container = new VBox(15);
        this.container.setAlignment(Pos.CENTER);
        this.container.setPadding(new Insets(30));
        this.container.setMaxSize(400, Region.USE_PREF_SIZE);

        // Styling the "Scoreboard" box
        this.container.setStyle(
                "-fx-background-color: rgba(5, 10, 20, 0.9); " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-color: #00b5d4; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 15;"
        );

        // Header
        Text header = new Text("TEAM OVERVIEW");
        header.setFill(Color.web("#00b5d4"));
        header.setFont(Font.font("System", FontWeight.BOLD, 20));
        container.getChildren().add(header);

        // Create a row for each player
        int colorIdx = 0;
        for (PlayerState player : players) {
            Color pColor = playerColors[colorIdx % playerColors.length];
            container.getChildren().add(createPlayerRow(player, pColor));
            colorIdx++;
        }

        // Hidden by default
        this.container.setVisible(false);
        this.container.setMouseTransparent(true);
    }

    private HBox createPlayerRow(PlayerState player, Color pColor) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 10;");

        // 1. Class Icon Placeholder (Rectangle)
        Rectangle iconPlaceholder = new Rectangle(30, 30);
        iconPlaceholder.setFill(Color.DARKGRAY);
        iconPlaceholder.setArcWidth(5);
        iconPlaceholder.setArcHeight(5);

        // 2. Player Name
        Text nameText = new Text(player.getPlayerName().toUpperCase());
        nameText.setFill(Color.WHITE);
        nameText.setFont(Font.font("System", FontWeight.BOLD, 14));

        // Use HGrow to push the pawn to the far right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 3. Pawn Visual (Replica of the PawnUI circle)
        Circle pawnPreview = new Circle(8);
        pawnPreview.setFill(pColor);
        pawnPreview.setStroke(Color.WHITE);
        pawnPreview.setStrokeWidth(1);

        row.getChildren().addAll(iconPlaceholder, nameText, spacer, pawnPreview);
        return row;
    }

    public VBox getContainer() {
        return container;
    }

    public void setVisible(boolean visible) {
        container.setVisible(visible);
    }
}