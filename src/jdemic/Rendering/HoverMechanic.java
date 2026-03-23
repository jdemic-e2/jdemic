package jdemic.Rendering;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;

import java.awt.Color;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class HoverMechanic
{
    private static final float MAP_HOVER_RADIUS_PX = 18.0f;
    private static final int MAP_NODE_HOVER_RADIUS_PX = 8 * 5;
    private static final float MAP_BORDER_FRACTION = 0.025f;

    private int hoveredCityIndex = -1;
    private boolean mapFocusMode;
    private boolean tabPressed;
    private boolean leftClickPressed;

    public int getHoveredCityIndex()
    {
        return hoveredCityIndex;
    }

    public boolean isMapFocusMode()
    {
        return mapFocusMode;
    }

    public void updateInputState(long window, VkExtent2D swapChainExtent, List<MapRenderer.AlignedCityNode> alignedCityNodes)
    {
        boolean isTabDown = glfwGetKey(window, GLFW_KEY_TAB) == GLFW_PRESS;
        if(isTabDown && !tabPressed)
        {
            mapFocusMode = !mapFocusMode;
        }
        tabPressed = isTabDown;

        boolean isLeftClickDown = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        int hoveredCity = detectHoveredCityIndex(window, swapChainExtent, alignedCityNodes);

        if(isLeftClickDown && !leftClickPressed && hoveredCity != -1)
        {
            var City = alignedCityNodes.get(hoveredCity).city;
            City.clickEvent();
        }
        leftClickPressed = isLeftClickDown;

        updateHoveredCityState(window, swapChainExtent, alignedCityNodes);
    }

    private void updateHoveredCityState(long window, VkExtent2D swapChainExtent, List<MapRenderer.AlignedCityNode> alignedCityNodes)
    {
        if(alignedCityNodes == null || alignedCityNodes.isEmpty())
        {
            return;
        }

        hoveredCityIndex = detectHoveredCityIndex(window, swapChainExtent, alignedCityNodes);
    }

    private int detectHoveredCityIndex(long window, VkExtent2D swapChainExtent, List<MapRenderer.AlignedCityNode> alignedCityNodes)
    {
        if(alignedCityNodes == null || alignedCityNodes.isEmpty())
        {
            return -1;
        }

        try(MemoryStack stack = stackPush())
        {
            DoubleBuffer cursorX = stack.mallocDouble(1);
            DoubleBuffer cursorY = stack.mallocDouble(1);
            glfwGetCursorPos(window, cursorX, cursorY);

            IntBuffer windowWidth = stack.mallocInt(1);
            IntBuffer windowHeight = stack.mallocInt(1);
            IntBuffer framebufferWidth = stack.mallocInt(1);
            IntBuffer framebufferHeight = stack.mallocInt(1);
            glfwGetWindowSize(window, windowWidth, windowHeight);
            glfwGetFramebufferSize(window, framebufferWidth, framebufferHeight);

            if(windowWidth.get(0) == 0 || windowHeight.get(0) == 0)
            {
                return -1;
            }

            float cursorFramebufferX = (float) (cursorX.get(0) * framebufferWidth.get(0) / (double) windowWidth.get(0));
            float cursorFramebufferY = (float) (cursorY.get(0) * framebufferHeight.get(0) / (double) windowHeight.get(0));

            Matrix4f model = new Matrix4f().identity();
            Matrix4f view = buildCurrentViewMatrix();
            Matrix4f projection = buildCurrentProjectionMatrix(swapChainExtent);
            Matrix4f mvp = new Matrix4f(projection).mul(view).mul(model);

            int bestIndex = -1;
            float bestDistanceSquared = MAP_HOVER_RADIUS_PX * MAP_HOVER_RADIUS_PX;

            for(int i = 0; i < alignedCityNodes.size(); i++)
            {
                MapRenderer.AlignedCityNode alignedNode = alignedCityNodes.get(i);
                Vector2f screen = projectNodeToScreen(alignedNode, mvp, swapChainExtent);
                if(screen == null)
                {
                    continue;
                }

                float dx = screen.x - cursorFramebufferX;
                float dy = screen.y - cursorFramebufferY;
                float distanceSquared = (dx * dx) + (dy * dy);
                if(distanceSquared < bestDistanceSquared)
                {
                    bestDistanceSquared = distanceSquared;
                    bestIndex = i;
                }
            }

            return bestIndex;
        }
    }

    private Vector2f projectNodeToScreen(MapRenderer.AlignedCityNode alignedNode, Matrix4f mvp, VkExtent2D swapChainExtent)
    {
        float insetU = MAP_BORDER_FRACTION + alignedNode.mapU * (1.0f - 2.0f * MAP_BORDER_FRACTION);
        float insetV = MAP_BORDER_FRACTION + alignedNode.mapV * (1.0f - 2.0f * MAP_BORDER_FRACTION);
        float worldX = -1.00f + (insetU * 2.00f);
        float worldY = 0.145f;
        float worldZ = -0.52f + (insetV * 1.04f);

        Vector4f clip = new Vector4f(worldX, worldY, worldZ, 1.0f);
        mvp.transform(clip);

        if(clip.w <= 0.0f)
        {
            return null;
        }

        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;

        float screenX = (ndcX * 0.5f + 0.5f) * swapChainExtent.width();
        float screenY = (ndcY * 0.5f + 0.5f) * swapChainExtent.height();

        return new Vector2f(screenX, screenY);
    }

    public Matrix4f buildCurrentViewMatrix()
    {
        Matrix4f view = new Matrix4f();
        if(mapFocusMode)
        {
            view.lookAt(0.0f, 1.25f, 0.0f, 0.0f, 0.145f, 0.0f, 0.0f, 0.0f, -1.0f);
        }
        else
        {
            view.lookAt(0.0f, 2.4f, 2.8f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        }
        return view;
    }

    public Matrix4f buildCurrentProjectionMatrix(VkExtent2D swapChainExtent)
    {
        Matrix4f projection = new Matrix4f();
        projection.perspective((float) Math.toRadians(mapFocusMode ? 50.0f : 45.0f),
                               (float) swapChainExtent.width() / (float) swapChainExtent.height(),
                               0.1f,
                               10.0f);
        projection.m11(projection.m11() * -1);
        return projection;
    }

    public float getHoverU(List<MapRenderer.AlignedCityNode> alignedCityNodes)
    {
        if(hoveredCityIndex >= 0 && alignedCityNodes != null && hoveredCityIndex < alignedCityNodes.size())
        {
            MapRenderer.AlignedCityNode node = alignedCityNodes.get(hoveredCityIndex);
            return MAP_BORDER_FRACTION + node.mapU * (1.0f - 2.0f * MAP_BORDER_FRACTION);
        }
        return 0;
    }

    public float getHoverV(List<MapRenderer.AlignedCityNode> alignedCityNodes)
    {
        if(hoveredCityIndex >= 0 && alignedCityNodes != null && hoveredCityIndex < alignedCityNodes.size())
        {
            MapRenderer.AlignedCityNode node = alignedCityNodes.get(hoveredCityIndex);
            return MAP_BORDER_FRACTION + node.mapV * (1.0f - 2.0f * MAP_BORDER_FRACTION);
        }
        return 0;
    }

    public float getHoverRadiusU(int mapTextureWidth)
    {
        return (float) MAP_NODE_HOVER_RADIUS_PX / (float) mapTextureWidth;
    }

    public float getHoverRadiusV(int mapTextureHeight)
    {
        return (float) MAP_NODE_HOVER_RADIUS_PX / (float) mapTextureHeight;
    }

    public Color getHoverColor(List<MapRenderer.AlignedCityNode> alignedCityNodes)
    {
        if(hoveredCityIndex >= 0 && alignedCityNodes != null && hoveredCityIndex < alignedCityNodes.size())
        {
            MapRenderer.AlignedCityNode node = alignedCityNodes.get(hoveredCityIndex);
            return MapRenderer.colorForDisease(node.city.getNativeColor());
        }
        return null;
    }
}
