package jdemic.Scenes.Settings;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.stage.Stage;

public class SettingsManager {
    // Singleton Instance
    private static SettingsManager instance;

    //JSON
    private final String FILE_PATH;
    private final Gson gson;

    // GENERAL
    private final StringProperty playerName = new SimpleStringProperty("Newbie");
    private final StringProperty language = new SimpleStringProperty("ENGLISH");

    // AUDIO
    private final DoubleProperty masterVolume = new SimpleDoubleProperty(0.5);
    private final DoubleProperty musicVolume = new SimpleDoubleProperty(0.5);
    private final DoubleProperty sfxVolume = new SimpleDoubleProperty(0.5);
    private final BooleanProperty isMuted = new SimpleBooleanProperty(false);

    // DISPLAY
    private final StringProperty resolution = new SimpleStringProperty("1280x720");
    private final DoubleProperty uiScale = new SimpleDoubleProperty(1.0);
    private final BooleanProperty isFullscreen = new SimpleBooleanProperty(false);

    // GAMEPLAY
    private final StringProperty animationSpeed = new SimpleStringProperty("FAST");

    // Constructors
    private SettingsManager() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        String appDir = System.getProperty("user.home") + File.separator + ".jdemic";
        File folder = new File(appDir);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        FILE_PATH = appDir + File.separator + "settings.json";

        loadSettings();
    }

    public static SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }

    //Getter functions

    public StringProperty playerNameProperty() { return playerName; }
    public StringProperty languageProperty() { return language; }

    public DoubleProperty masterVolumeProperty() { return masterVolume; }
    public DoubleProperty musicVolumeProperty() { return musicVolume; }
    public DoubleProperty sfxVolumeProperty() { return sfxVolume; }
    public BooleanProperty isMutedProperty() { return isMuted; }

    public StringProperty resolutionProperty() { return resolution; }
    public DoubleProperty uiScaleProperty() { return uiScale; }
    public BooleanProperty isFullscreenProperty() { return isFullscreen; }

    public StringProperty animationSpeedProperty() { return animationSpeed; }

    public void printAll()
    {
        System.out.println("Player name: " + playerName.get());
        System.out.println("Language: " + language.get());

        System.out.println("Master Volume: " + masterVolume.get());
        System.out.println("Music Volume: " + musicVolume.get());
        System.out.println("SFX Volume: " + sfxVolume.get());
        System.out.println("Is muted:" + isMuted.get());

        System.out.println("Resolution: " + resolution.get());
        System.out.println("UI Scale: " + uiScale.get());
        System.out.println("Fullscreen: " + isFullscreen.get());

        System.out.println("AnimationSpeed: " + animationSpeed.get());
    }

    // Those should probably be a json so the settings can be saved and loaded between instances
    public void loadSettings() {
        File file  = new File(FILE_PATH);
        if (!file.exists()) {
            System.out.println("Settings file does not exist. Creating new settings file");
            saveSettings();
            return;
        }
        try (FileReader reader = new FileReader(file)) {
            SettingsData settingsData = gson.fromJson(reader, SettingsData.class);
            if (settingsData != null) {
                this.playerName.set(settingsData.playerName);
                this.resolution.set(settingsData.resolution);
                this.isFullscreen.set(settingsData.isFullScreen);
                this.masterVolume.set(settingsData.masterVolume);
                this.musicVolume.set(settingsData.musicVolume);
                this.animationSpeed.set(settingsData.animationSpeed);
                System.out.println("Settings loaded successfully");
            }
        } catch (IOException e) {
            System.err.println("Settings file could not be read: " + e.getMessage());
        }
    }

    public void saveSettings() {
        SettingsData settingsData = new SettingsData();

        settingsData.playerName = this.playerName.get();
        settingsData.resolution = this.resolution.get();
        settingsData.isFullScreen = this.isFullscreen.get();
        settingsData.masterVolume = this.masterVolume.get();
        settingsData.musicVolume = this.musicVolume.get();
        settingsData.animationSpeed = this.animationSpeed.get();

        File file = new File(FILE_PATH);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileWriter settingsWriter = new FileWriter(file)) {
            gson.toJson(settingsData, settingsWriter);
            System.out.println("Settings file written to: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Settings file could not be written: " + e.getMessage());
        }
    }

    //I made those because the resolution would break if i tried not putting default settings
    public double getSavedWidth() {
        return Double.parseDouble(resolution.get().split("x")[0]);
    }

    public double getSavedHeight() {
        return Double.parseDouble(resolution.get().split("x")[1]);
    }

    public void applySettingsToStage(Stage stage) {
        stage.setWidth(getSavedWidth());
        stage.setHeight(getSavedHeight());
        stage.setFullScreen(isFullscreen.get());
        stage.setFullScreenExitHint("");
    }
}