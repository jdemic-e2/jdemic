package integration;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ExampleIT {

    @Test
    void testMathLibraryIntegration() {
        // Minimal smoke check: Validar que las dependencias core (como JOML) cargan
        // correctamente
        Vector3f vec = new Vector3f(1.0f, 2.0f, 3.0f);

        assertNotNull(vec, "Vector instance should not be null");
        assertEquals(3.0f, vec.z, "Vector Z coordinate should be 3.0f");
    }
}
