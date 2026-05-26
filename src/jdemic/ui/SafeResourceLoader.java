package jdemic.ui;

import javafx.scene.image.Image;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Null-safe resource loader for images and other classpath resources.
 * Prevents NullPointerException when a resource is missing or renamed.
 */
public class SafeResourceLoader {
    private static final Map<String, SoftReference<Image>> IMAGE_CACHE = new ConcurrentHashMap<>();

    /**
     * Loads an Image from the classpath. Returns null if the resource is missing.
     */
    public static Image loadImage(String path) {
        URL url = SafeResourceLoader.class.getResource(path);
        if (url == null) {
            System.err.println("[SafeResourceLoader] Missing resource: " + path);
            return null;
        }
        return loadImage(url);
    }

    /**
     * Loads an Image from a URL and reuses the decoded image while memory allows.
     */
    public static Image loadImage(URL url) {
        if (url == null) {
            return null;
        }

        String key = url.toExternalForm();
        SoftReference<Image> cachedReference = IMAGE_CACHE.get(key);
        Image cachedImage = cachedReference == null ? null : cachedReference.get();
        if (cachedImage != null) {
            return cachedImage;
        }

        Image image = new Image(key);
        IMAGE_CACHE.put(key, new SoftReference<>(image));
        return image;
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
