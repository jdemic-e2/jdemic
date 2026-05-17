package jdemic.Scenes.Tutorial;

import javafx.beans.binding.Bindings;
import javafx.geometry.*;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import jdemic.Scenes.SceneManager.SceneManager;
import jdemic.ui.*;

public class TutorialWinLoseScene {

    private StackPane root;
    private Stage stage;

    public TutorialWinLoseScene(Stage stage) {
        this.stage = stage;
        root = new StackPane();
        TutorialUtil.setBackground(root);
        setupUI();
    }

    private void setupUI() {

        TutorialUtil.createTutorialTitle(root,"TUTORIAL",0.02,"#00b5d4",Pos.TOP_LEFT,0.03,0.03,null,0);
        TutorialUtil.createTutorialTitle(root,"8. WIN / LOSE",0.05,"#cfc900",Pos.TOP_CENTER,0.14,0.08,"#cfc900",5);

        HBox mainLayout = new HBox();
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.prefWidthProperty().bind(root.widthProperty());
        mainLayout.prefHeightProperty().bind(root.heightProperty());
        mainLayout.spacingProperty().bind(root.widthProperty().multiply(0.05));

        StackPane winPanel = TutorialUtil.createTutorialPanel(root);
        winPanel.prefWidthProperty().bind(root.widthProperty().multiply(0.3));
        winPanel.prefHeightProperty().bind(root.heightProperty().multiply(0.45));

        Label winTitle = TextUtil.createText("WIN","neotechprobold",0.03,"#00b5d4",root);
        Label winText = TextUtil.createText("All 4 cures are discovered:\n\n" +"Malware has been stopped from spreading\n" +"and now the systems are cleared.", "neotechprobold", 0.016,"#ffffff",root);
        winText.setWrapText(true);
        winText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        winText.maxWidthProperty().bind(winPanel.widthProperty().multiply(0.75));

        VBox winContent = new VBox(winTitle, winText);
        winContent.setAlignment(Pos.CENTER);
        winContent.spacingProperty().bind(root.heightProperty().multiply(0.02));

        StackPane winWrapper = new StackPane(winContent);
        winWrapper.setAlignment(Pos.CENTER);

        winWrapper.paddingProperty().bind(Bindings.createObjectBinding(() ->new Insets(
                                        root.getHeight() * 0.04,
                                        root.getWidth() * 0.02,
                                        root.getHeight() * 0.04,
                                        root.getWidth() * 0.02
                                ),
                        root.widthProperty(),
                        root.heightProperty()
                )
        );
        winPanel.getChildren().add(winWrapper);

        StackPane losePanel = TutorialUtil.createTutorialPanel(root);
        losePanel.prefWidthProperty().bind(root.widthProperty().multiply(0.3));
        losePanel.prefHeightProperty().bind(root.heightProperty().multiply(0.45));

        Label loseTitle = TextUtil.createText("LOSE","neotechprobold",0.03,"#00b5d4",root);

        Label loseText = TextUtil.createText("• Too many outbreaks occur\n" + "OR\n" + "• You run out of disease cubes\n" +"OR\n" +"• The player deck runs out", "neotechprobold",0.016,"#ffffff",root);
        loseText.setWrapText(true);
        loseText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        loseText.maxWidthProperty().bind(losePanel.widthProperty().multiply(0.75));

        VBox loseContent = new VBox(loseTitle, loseText);
        loseContent.setAlignment(Pos.CENTER);
        loseContent.spacingProperty().bind(root.heightProperty().multiply(0.02));

        StackPane loseWrapper = new StackPane(loseContent);
        loseWrapper.setAlignment(Pos.CENTER);
        loseWrapper.paddingProperty().bind(Bindings.createObjectBinding(() ->new Insets(
                                        root.getHeight() * 0.04,
                                        root.getWidth() * 0.02,
                                        root.getHeight() * 0.04,
                                        root.getWidth() * 0.02
                                ),
                        root.widthProperty(),
                        root.heightProperty()
                )
        );
        losePanel.getChildren().add(loseWrapper);

        mainLayout.getChildren().addAll(winPanel, losePanel);
        root.getChildren().add(mainLayout);

        TutorialUtil.addBottomButtons(root, root, stage,
                () -> SceneManager.switchScene("TUT_TURN"),
                () -> {
                    SceneManager.clearCache();
                    SceneManager.switchScene("MAIN_MENU");
                }
        );
        for (var node : root.getChildren()) {
            if (node instanceof ButtonsUtil btn) {
                for (var child : btn.getChildren()) {
                    if (child instanceof Label label && label.getText().equals("NEXT")) {
                        label.setText("FINISH");
                        label.setStyle("-fx-text-fill: #00ff88;");
                    }
                }
            }
        }
    }
    public StackPane getRoot() {
        return root;
    }
}