package jdemic.Scenes.Settings;

import javafx.beans.property.*;

public class SettingsManager {
    // Singleton Instance
    private static SettingsManager instance;

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
    private SettingsManager() {}

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
    public void saveSettings() {
        // It creates a json file with the settings
    }

    public void loadSettings() {
        // Loads a json file with the settings
    }
}