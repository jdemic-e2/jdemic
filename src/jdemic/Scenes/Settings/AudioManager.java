package jdemic.Scenes.Settings;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class AudioManager {
    private static AudioManager instance;

    // Channels
    private MediaPlayer musicPlayer;
    private Map<String, Media> musicLibrary = new HashMap<>();

    private AudioManager() {
        // Load the songs here
        loadMusic("MENU", "/audio/menu_theme.mp3");
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
        if (musicPlayer != null) {
            musicPlayer.stop(); // Stops the current song
        }

        Media nextTrack = musicLibrary.get(key);
        if (nextTrack != null) {
            musicPlayer = new MediaPlayer(nextTrack);
            musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            updateVolume(); // Apply the settings volume
            musicPlayer.play();
        }
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