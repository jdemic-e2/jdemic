package jdemic.GameLogic.ServerRelatedClasses;

public class LobbyChatMessage {
    private String playerName;
    private String message;
    private long timestamp;

    public LobbyChatMessage() {
    }

    public LobbyChatMessage(String playerName, String message, long timestamp) {
        this.playerName = playerName;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
