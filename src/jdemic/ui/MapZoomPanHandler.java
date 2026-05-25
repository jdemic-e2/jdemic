package jdemic.ui;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public class MapZoomPanHandler {

    private static final double MIN_SCALE   = 0.5;
    private static final double MAX_SCALE   = 4.0;
    private static final double ZOOM_FACTOR = 1.12;   // per scroll tick

    private final Pane      mapPane;
    private final Scale     scale;
    private final Translate translate;

    private boolean isDragging       = false;
    private double  dragStartSceneX;
    private double  dragStartSceneY;
    private double  txAtDragStart;
    private double  tyAtDragStart;

    private MapZoomPanHandler(Pane mapPane) {
        this.mapPane   = mapPane;
        this.scale     = new Scale(1, 1, 0, 0);
        this.translate = new Translate(0, 0);

        mapPane.getTransforms().addAll(translate, scale);

        mapPane.addEventFilter(ScrollEvent.SCROLL,         this::handleScroll);
        mapPane.addEventFilter(MouseEvent.MOUSE_PRESSED,   this::handleMousePressed);
        mapPane.addEventFilter(MouseEvent.MOUSE_DRAGGED,   this::handleMouseDragged);
        mapPane.addEventFilter(MouseEvent.MOUSE_RELEASED,  this::handleMouseReleased);
    }

    public static MapZoomPanHandler attach(Pane mapPane) {
        return new MapZoomPanHandler(mapPane);
    }

    private void handleScroll(ScrollEvent e) {
        double delta    = e.getDeltaY();
        double oldScale = scale.getX();
        double newScale = (delta > 0)
                ? Math.min(oldScale * ZOOM_FACTOR, MAX_SCALE)
                : Math.max(oldScale / ZOOM_FACTOR, MIN_SCALE);

        if (newScale == oldScale) { e.consume(); return; }

        // keep the point under the cursor fixed after scaling
        double mouseX = e.getX();
        double mouseY = e.getY();
        double worldX = (mouseX - translate.getX()) / oldScale;
        double worldY = (mouseY - translate.getY()) / oldScale;

        scale.setX(newScale);
        scale.setY(newScale);
        translate.setX(mouseX - worldX * newScale);
        translate.setY(mouseY - worldY * newScale);

        clampTranslation();
        e.consume();
    }

    private void handleMousePressed(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY) return;

        // only start a drag when the user clicks the map background,
        // not on a city node
        if (e.getTarget() instanceof Circle) return;

        isDragging       = true;
        dragStartSceneX  = e.getSceneX();
        dragStartSceneY  = e.getSceneY();
        txAtDragStart    = translate.getX();
        tyAtDragStart    = translate.getY();

        mapPane.setCursor(javafx.scene.Cursor.CLOSED_HAND);
        // do NOT consume
    }

    private void handleMouseDragged(MouseEvent e) {
        if (!isDragging || e.getButton() != MouseButton.PRIMARY) return;

        translate.setX(txAtDragStart + (e.getSceneX() - dragStartSceneX));
        translate.setY(tyAtDragStart + (e.getSceneY() - dragStartSceneY));

        clampTranslation();
        e.consume();   // consume only while actively dragging
    }

    private void handleMouseReleased(MouseEvent e) {
        if (!isDragging || e.getButton() != MouseButton.PRIMARY) return;
        isDragging = false;
        mapPane.setCursor(javafx.scene.Cursor.DEFAULT);
        // do NOT consume
    }

    // clamping to not zoom in or out too much
    private void clampTranslation() {
        double s = scale.getX();

        double mapW    = mapPane.getWidth()  * s;
        double mapH    = mapPane.getHeight() * s;
        double parentW = parentDimension(true);
        double parentH = parentDimension(false);

        double marginX = parentW * 0.10;
        double marginY = parentH * 0.10;

        double minTx = -(mapW  - marginX);    // map right edge stays visible
        double maxTx =   parentW - marginX;   // map left edge stays visible
        double minTy = -(mapH  - marginY);
        double maxTy =   parentH - marginY;

        translate.setX(Math.max(minTx, Math.min(maxTx, translate.getX())));
        translate.setY(Math.max(minTy, Math.min(maxTy, translate.getY())));
    }

    private double parentDimension(boolean width) {
        if (mapPane.getParent() instanceof javafx.scene.layout.Region r) {
            return width ? r.getWidth() : r.getHeight();
        }
        return width ? mapPane.getWidth() : mapPane.getHeight();
    }

    // helpers

    public void reset() {
        scale.setX(1); scale.setY(1);
        translate.setX(0); translate.setY(0);
    }

    public double getZoom() { return scale.getX(); }
}