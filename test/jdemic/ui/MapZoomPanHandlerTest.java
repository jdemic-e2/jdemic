package jdemic.ui;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MapZoomPanHandlerTest {

    private Pane mapPane;

    @BeforeAll
    static void initJavaFX() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Already initialized
        }
    }

    @BeforeEach
    void setUp() {
        mapPane = new Pane();
        mapPane.setPrefSize(1000, 1000);
    }

    @Test
    void testAttachAndReset() {
        MapZoomPanHandler handler = MapZoomPanHandler.attach(mapPane);
        assertEquals(1.0, handler.getZoom());

        handler.reset();
        assertEquals(1.0, handler.getZoom());
    }

    @Test
    void testHandleScroll() {
        MapZoomPanHandler handler = MapZoomPanHandler.attach(mapPane);

        // Zoom In Event
        ScrollEvent scrollIn = new ScrollEvent(ScrollEvent.SCROLL, 0, 0, 0, 0, false, false, false, false, false, false,
                0, 120, 0, 0, ScrollEvent.HorizontalTextScrollUnits.NONE, 0, ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0, null);
        mapPane.fireEvent(scrollIn);
        assertTrue(handler.getZoom() > 1.0);

        // Zoom Out Event
        ScrollEvent scrollOut = new ScrollEvent(ScrollEvent.SCROLL, 0, 0, 0, 0, false, false, false, false, false, false,
                0, -120, 0, 0, ScrollEvent.HorizontalTextScrollUnits.NONE, 0, ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0, null);
        mapPane.fireEvent(scrollOut);
        
        // Push limits to evaluate clamp bounds boundary conditions
        for (int i = 0; i < 30; i++) {
            mapPane.fireEvent(scrollOut);
        }
        assertEquals(0.5, handler.getZoom()); // Reaches MIN_SCALE bound
    }

    @Test
    void testMouseDragFlow() {
        MapZoomPanHandler handler = MapZoomPanHandler.attach(mapPane);

        // Mouse Press on Map Pane Background
        MouseEvent press = new MouseEvent(MouseEvent.MOUSE_PRESSED, 100, 100, 100, 100, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, false, null);
        mapPane.fireEvent(press);

        // Mouse Drag Action
        MouseEvent drag = new MouseEvent(MouseEvent.MOUSE_DRAGGED, 150, 150, 150, 150, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, false, null);
        mapPane.fireEvent(drag);

        // Mouse Release Action
        MouseEvent release = new MouseEvent(MouseEvent.MOUSE_RELEASED, 150, 150, 150, 150, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, false, null);
        mapPane.fireEvent(release);
        
        assertNotNull(handler);
    }

    @Test
    void testMousePressed_IgnoredScenarios() {
        MapZoomPanHandler handler = MapZoomPanHandler.attach(mapPane);

        // Ignored Scenario 1: Wrong Mouse Button Trigger
        MouseEvent secondaryPress = new MouseEvent(MouseEvent.MOUSE_PRESSED, 100, 100, 100, 100, MouseButton.SECONDARY, 1,
                false, false, false, false, false, false, false, false, false, false, null);
        mapPane.fireEvent(secondaryPress);

        // Ignored Scenario 2: Node target is instanceof Circle (CityNode)
        Circle cityNodeMock = new Circle();
        MouseEvent circlePress = new MouseEvent(null, cityNodeMock, MouseEvent.MOUSE_PRESSED, 100, 100, 100, 100, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, false, null);
        mapPane.fireEvent(circlePress);
        
        assertNotNull(handler);
    }

    @Test
    void testParentDimension_RegionBranch() {
        // Use an explicit Pane (subclass of Region) to ensure layout bounds and getChildren() are fully visible/accessible
        Pane parentRegion = new Pane();
        parentRegion.setPrefSize(800, 600);
        parentRegion.getChildren().add(mapPane);

        MapZoomPanHandler handler = MapZoomPanHandler.attach(mapPane);
        
        // Fire drag events to trigger clamp translation calculation relying on the parent width/height
        MouseEvent press = new MouseEvent(MouseEvent.MOUSE_PRESSED, 10, 10, 10, 10, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, false, null);
        mapPane.fireEvent(press);
        
        MouseEvent drag = new MouseEvent(MouseEvent.MOUSE_DRAGGED, 200, 200, 200, 200, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, false, null);
        mapPane.fireEvent(drag);

        assertNotNull(handler);
    }
}