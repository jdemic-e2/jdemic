package jdemic.ui;

import javafx.scene.image.Image;
import java.net.URL;

/**
 * Null-safe resource loader for images and other classpath resources.
 * Prevents NullPointerException when a resource is missing or renamed.
 */
public class SafeResourceLoader {

    /**
     * Loads an Image from the classpath. Returns null if the resource is missing.
     */
    public static Image loadImage(String path) {
        URL url = SafeResourceLoader.class.getResource(path);
        if (url == null) {
            System.err.println("[SafeResourceLoader] Missing resource: " + path);
            return null;
        }
        return new Image(url.toExternalForm());
    }

    /**
     * Loads a resource URL from the classpath. Returns null if missing.
     */
    public static URL loadResource(String path) {
        URL url = SafeResourceLoader.class.getResource(path);
        if (url == null) {
            System.err.println("[SafeResourceLoader] Missing resource: " + path);
        }
        return url;
    }
}
