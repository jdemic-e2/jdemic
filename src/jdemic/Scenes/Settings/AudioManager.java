package jdemic.Scenes.Settings;

import javafx.scene.Scene;
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
        try {
            musicLibrary.put(key, new Media(getClass().getResource(path).toExternalForm()));
        } catch (Exception e) { System.out.println("Error loading music: " + key); }
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
        try {
            Media sound = new Media(getClass().getResource(path).toExternalForm());
            MediaPlayer sfxPlayer = new MediaPlayer(sound);

            SettingsManager sm = SettingsManager.getInstance();
            sfxPlayer.setVolume(sm.masterVolumeProperty().get());

            sfxPlayer.play();

            sfxPlayer.setOnEndOfMedia(sfxPlayer::dispose);
        } catch (Exception e) {
            System.out.println("Error playing SFX: " + e.getMessage());
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