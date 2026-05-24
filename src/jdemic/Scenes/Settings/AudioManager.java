package jdemic.Scenes.Settings;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AudioManager {
    private static AudioManager instance;
    private static final List<String> GAME_TRACK_KEYS = List.of("GAME1", "GAME2", "GAME3");

    // Channels
    private MediaPlayer musicPlayer;
    private Map<String, Media> musicLibrary = new HashMap<>();
    private String currentMusicMode;
    private List<String> gamePlaylist = new ArrayList<>();
    private int gamePlaylistIndex;

    private AudioManager() {
        // Load the songs here
        loadMusic("MENU", "/audio/menu.mp3");
        loadMusic("BUTTON", "/audio/button.mp3");
        loadMusic("GAME1", "/audio/game1.mp3");
        loadMusic("GAME2", "/audio/game2.mp3");
        loadMusic("GAME3", "/audio/game3.mp3");
    }

    private void loadMusic(String key, String path) {
        URL res = getClass().getResource(path);
        if (res == null) {
            System.err.println("[AudioManager] Missing music resource: " + path);
            return;
        }
        try {
            musicLibrary.put(key, new Media(res.toExternalForm()));
        } catch (Exception e) {
            System.err.println("[AudioManager] Error loading media: " + e.getMessage());
        }
    }

    // MUSIC LOGIC
    public void playMusic(String key) {
        currentMusicMode = key;
        gamePlaylist.clear();
        if (musicPlayer != null) {
            musicPlayer.stop(); // Stops the current song
        }

        Media nextTrack = musicLibrary.get(key);
        if (nextTrack != null) {
            try {
                musicPlayer = new MediaPlayer(nextTrack);
                musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                musicPlayer.setOnEndOfMedia(null);
                updateVolume(); // Apply the settings volume
                musicPlayer.play();
            } catch (Exception e) {
                System.err.println("[AudioManager] Error playing music: " + e.getMessage());
            }
        }
    }

    public void playGameMusic() {
        if ("GAME".equals(currentMusicMode) && musicPlayer != null) {
            return;
        }

        currentMusicMode = "GAME";
        gamePlaylist = new ArrayList<>(GAME_TRACK_KEYS);
        Collections.shuffle(gamePlaylist);
        gamePlaylistIndex = 0;
        playNextGameTrack();
    }

    private void playNextGameTrack() {
        if (!"GAME".equals(currentMusicMode) || gamePlaylist.isEmpty()) {
            return;
        }

        if (musicPlayer != null) {
            musicPlayer.stop();
        }

        Media nextTrack = null;
        for (int attempts = 0; attempts < gamePlaylist.size(); attempts++) {
            String key = gamePlaylist.get(gamePlaylistIndex);
            gamePlaylistIndex = (gamePlaylistIndex + 1) % gamePlaylist.size();
            nextTrack = musicLibrary.get(key);
            if (nextTrack != null) {
                break;
            }
        }

        if (nextTrack == null) {
            System.err.println("[AudioManager] No game music tracks are available.");
            return;
        }

        try {
            musicPlayer = new MediaPlayer(nextTrack);
            musicPlayer.setCycleCount(1);
            musicPlayer.setOnEndOfMedia(this::playNextGameTrack);
            updateVolume();
            musicPlayer.play();
        } catch (Exception e) {
            System.err.println("[AudioManager] Error playing game music: " + e.getMessage());
        }
    }

    public void playButtonSFX() {
        playSFX("/audio/button.mp3");
    }

    // SFX LOGIC
    public void playSFX(String path) {
        URL res = getClass().getResource(path);
        if (res == null) {
            System.err.println("[AudioManager] Missing SFX resource: " + path);
            return;
        }
        try {
            SettingsManager sm = SettingsManager.getInstance();
            if (sm.isMutedProperty().get()) return;

            AudioClip sfx = new AudioClip(res.toExternalForm());
            double volume = sm.masterVolumeProperty().get() * sm.sfxVolumeProperty().get();
            sfx.setVolume(volume);
            sfx.play();
        } catch (Exception e) {
            System.err.println("[AudioManager] Error playing SFX: " + e.getMessage());
        }
    }

    public void updateVolume() {
        if (musicPlayer != null) {
            SettingsManager sm = SettingsManager.getInstance();
            double finalVol = sm.masterVolumeProperty().get() * sm.musicVolumeProperty().get();
            musicPlayer.setVolume(finalVol);
        }
    }

    public static AudioManager getInstance() {
        if (instance == null) instance = new AudioManager();
        return instance;
    }
}
