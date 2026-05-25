package jdemic.util;

import javafx.scene.control.TextFormatter;
import jdemic.Scenes.Settings.SettingsManager;

import java.util.function.UnaryOperator;

public class PlayerNameUtil {
    public static void savePlayerName(String nickname) {
        SettingsManager settingsManager = SettingsManager.getInstance();
        settingsManager.playerNameProperty().set(nickname);
        settingsManager.saveSettings();
    }

    public static String savedPlayerName() {
        String savedName = SettingsManager.getInstance().playerNameProperty().get();
        return normalizeNickname(savedName);
    }

    public static UnaryOperator<TextFormatter.Change> nicknameFilter() {
        return change -> {
            String text = change.getControlNewText();
            return text.matches("[a-zA-Z0-9]*") && text.length() <= 16 ? change : null;
        };
    }

    public static String normalizeNickname(String nickname) {
        if (nickname == null) {
            return "Player";
        }
        String normalized = nickname.replaceAll("[^a-zA-Z0-9]", "");
        if (normalized.length() > 16) {
            normalized = normalized.substring(0, 16);
        }
        return normalized.isBlank() ? "Player" : normalized;
    }
}
