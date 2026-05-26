package jdemic.ui;

import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;

public final class PlayerNameUtil {
    private static final int MAX_NICKNAME_LENGTH = 16;
    private static final String DEFAULT_NICKNAME = "Player";
    private static final String ALLOWED_NICKNAME_PATTERN = "[a-zA-Z0-9]*";
    private static final String DISALLOWED_NICKNAME_CHARS = "[^a-zA-Z0-9]";

    private PlayerNameUtil() {
    }

    public static UnaryOperator<TextFormatter.Change> nicknameFilter() {
        return change -> {
            String text = change.getControlNewText();
            return text.matches(ALLOWED_NICKNAME_PATTERN) && text.length() <= MAX_NICKNAME_LENGTH ? change : null;
        };
    }

    public static String normalizeNickname(String nickname) {
        if (nickname == null) {
            return DEFAULT_NICKNAME;
        }
        String normalized = nickname.replaceAll(DISALLOWED_NICKNAME_CHARS, "");
        if (normalized.length() > MAX_NICKNAME_LENGTH) {
            normalized = normalized.substring(0, MAX_NICKNAME_LENGTH);
        }
        return normalized.isBlank() ? DEFAULT_NICKNAME : normalized;
    }
}
