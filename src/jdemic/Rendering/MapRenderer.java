package jdemic.Rendering;

import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.PandemicMapGraph;

import org.joml.Vector2f;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.memAlloc;

import org.lwjgl.system.MemoryStack;

public class MapRenderer
{
    public static final String PANDEMIC_MAP_TEXTURE_PATH = "textures/pandemicmap.png";
    private static final float MAP_NODE_ALIGNMENT_OFFSET_X = 0.006f;
    private static final float MAP_NODE_ALIGNMENT_OFFSET_Y = -0.004f;
    private static final int MAP_NODE_RADIUS_PX = 5 * 5;
    private static final float MAP_BORDER_FRACTION = 0.025f;

    private BufferedImage pandemicMapBaseImage;
    private int mapTextureWidth;
    private int mapTextureHeight;
    private List<CityNode> pandemicCities;
    private List<AlignedCityNode> alignedCityNodes;

    public List<AlignedCityNode> getAlignedCityNodes()
    {
        return alignedCityNodes;
    }

    public int getMapTextureWidth()
    {
        return mapTextureWidth;
    }

    public int getMapTextureHeight()
    {
        return mapTextureHeight;
    }

    public static class AlignedCityNode
    {
        public final CityNode city;
        public final int pixelX;
        public final int pixelY;
        public final float mapU;
        public final float mapV;

        public AlignedCityNode(CityNode city, int pixelX, int pixelY, int imageWidth, int imageHeight)
        {
            this.city = city;
            this.pixelX = pixelX;
            this.pixelY = pixelY;
            this.mapU = (float) pixelX / (float) imageWidth;
            this.mapV = (float) pixelY / (float) imageHeight;
        }
    }

    public static class LoadedTextureData
    {
        public final ByteBuffer pixels;
        public final int width;
        public final int height;
        public final boolean stbOwned;

        public LoadedTextureData(ByteBuffer pixels, int width, int height, boolean stbOwned)
        {
            this.pixels = pixels;
            this.width = width;
            this.height = height;
            this.stbOwned = stbOwned;
        }
    }

    public LoadedTextureData loadTextureData(String resourcePath, String filename, MemoryStack stack)
    {
        if(PANDEMIC_MAP_TEXTURE_PATH.equals(resourcePath))
        {
            return createAnnotatedPandemicMapTextureData(filename);
        }

        IntBuffer pWidth = stack.mallocInt(1);
        IntBuffer pHeight = stack.mallocInt(1);
        IntBuffer pChannels = stack.mallocInt(1);
        ByteBuffer pixels = stbi_load(filename, pWidth, pHeight, pChannels, STBI_rgb_alpha);

        if(pixels == null)
        {
            throw new RuntimeException("Failed to load texture image " + filename);
        }

        return new LoadedTextureData(pixels, pWidth.get(0), pHeight.get(0), true);
    }

    private LoadedTextureData createAnnotatedPandemicMapTextureData(String filename)
    {
        try
        {
            BufferedImage source = ImageIO.read(new File(filename));
            if(source == null)
            {
                throw new IOException("Could not decode map image file");
            }

            pandemicMapBaseImage = source;
            mapTextureWidth = source.getWidth();
            mapTextureHeight = source.getHeight();
            var tempCity = new PandemicMapGraph();
            pandemicCities = tempCity.getCityList();
            alignedCityNodes = alignNodesToMapBackground(source, pandemicCities);

            BufferedImage annotated = createAnnotatedMapImage(-1);
            return new LoadedTextureData(toRgbaByteBuffer(annotated), annotated.getWidth(), annotated.getHeight(), false);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to annotate pandemic map texture: " + filename, e);
        }
    }

    public BufferedImage createAnnotatedMapImage(int hoveredIndex)
    {
        int w = pandemicMapBaseImage.getWidth();
        int h = pandemicMapBaseImage.getHeight();

        BufferedImage annotated = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = annotated.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int borderW = Math.max(6, (int)(w * MAP_BORDER_FRACTION));
        int borderH = Math.max(6, (int)(h * MAP_BORDER_FRACTION));

        g2d.setComposite(java.awt.AlphaComposite.Clear);
        g2d.fillRect(0, 0, w, h);
        g2d.setComposite(java.awt.AlphaComposite.SrcOver);

        g2d.drawImage(pandemicMapBaseImage, borderW, borderH, w - 2 * borderW, h - 2 * borderH, null);

        EdgeRenderer.drawEdges(g2d, alignedCityNodes, w, h, MAP_BORDER_FRACTION);

        int fontSize = Math.max(12, w / 150);
        g2d.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        FontMetrics metrics = g2d.getFontMetrics();

        int mapW = w - 2 * borderW;
        int mapH = h - 2 * borderH;

        for(int i = 0; i < alignedCityNodes.size(); i++)
        {
            AlignedCityNode alignedNode = alignedCityNodes.get(i);
            CityNode city = alignedNode.city;
            int nodeX = borderW + (int)((float) alignedNode.pixelX / w * mapW);
            int nodeY = borderH + (int)((float) alignedNode.pixelY / h * mapH);

            int nodeRadius = MAP_NODE_RADIUS_PX;
            Color nodeColor = colorForDisease(city.getNativeColor());
            g2d.setColor(nodeColor);
            g2d.fillOval(nodeX - nodeRadius, nodeY - nodeRadius, nodeRadius * 2, nodeRadius * 2);
            g2d.setColor(new Color(10, 10, 10, 220));
            g2d.drawOval(nodeX - nodeRadius, nodeY - nodeRadius, nodeRadius * 2, nodeRadius * 2);

            String label = city.getName();
            int textWidth = metrics.stringWidth(label);
            int labelX = Math.max(0, Math.min(nodeX - (textWidth / 2), w - textWidth));
            int labelY = nodeY - 10;
            if(labelY < metrics.getAscent())
            {
                labelY = nodeY + metrics.getAscent() + 8;
            }

            drawCityLabel(g2d, metrics, label, labelX, labelY, w, h);
            g2d.drawString(label, labelX, labelY);
        }

        g2d.dispose();
        return annotated;
    }

    private List<AlignedCityNode> alignNodesToMapBackground(BufferedImage source, List<CityNode> cities)
    {
        List<AlignedCityNode> alignedNodes = new ArrayList<>(cities.size());

        for(CityNode city : cities)
        {
            float adjustedX = city.getRenderX() + MAP_NODE_ALIGNMENT_OFFSET_X;
            float adjustedY = city.getRenderY() + MAP_NODE_ALIGNMENT_OFFSET_Y;
            int expectedX = Math.round(adjustedX * source.getWidth());
            int expectedY = Math.round(adjustedY * source.getHeight());

            Vector2f snapped = snapNodePosition(source, city.getNativeColor(), expectedX, expectedY, 18);
            alignedNodes.add(new AlignedCityNode(city,
                                                 Math.round(snapped.x),
                                                 Math.round(snapped.y),
                                                 source.getWidth(),
                                                 source.getHeight()));
        }

        return alignedNodes;
    }

    private Vector2f snapNodePosition(BufferedImage image, CityNode.DiseaseColor diseaseColor, int expectedX, int expectedY, int radius)
    {
        int width = image.getWidth();
        int height = image.getHeight();
        int clampedExpectedX = Math.max(0, Math.min(expectedX, width - 1));
        int clampedExpectedY = Math.max(0, Math.min(expectedY, height - 1));

        float bestScore = Float.NEGATIVE_INFINITY;
        int bestX = clampedExpectedX;
        int bestY = clampedExpectedY;

        for(int y = Math.max(0, clampedExpectedY - radius); y <= Math.min(height - 1, clampedExpectedY + radius); y++)
        {
            for(int x = Math.max(0, clampedExpectedX - radius); x <= Math.min(width - 1, clampedExpectedX + radius); x++)
            {
                int argb = image.getRGB(x, y);
                int red = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue = argb & 0xFF;
                float score = colorMatchScore(diseaseColor, red, green, blue);

                int dx = x - clampedExpectedX;
                int dy = y - clampedExpectedY;
                float distancePenalty = (dx * dx + dy * dy) * 0.22f;
                score -= distancePenalty;

                if(score > bestScore)
                {
                    bestScore = score;
                    bestX = x;
                    bestY = y;
                }
            }
        }

        return new Vector2f(bestX, bestY);
    }

    private float colorMatchScore(CityNode.DiseaseColor diseaseColor, int red, int green, int blue)
    {
        return switch (diseaseColor)
        {
            case BLUE -> (blue * 2.2f) - (red * 0.8f) - (green * 0.5f);
            case YELLOW -> ((red + green) * 1.25f) - (blue * 1.4f);
            case RED -> (red * 2.3f) - (green * 0.9f) - (blue * 1.1f);
            case BLACK -> ((red + green + blue) * 0.95f) - Math.abs(red - green) - Math.abs(green - blue);
        };
    }

    public ByteBuffer toRgbaByteBuffer(BufferedImage image)
    {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argbPixels = new int[width * height];
        image.getRGB(0, 0, width, height, argbPixels, 0, width);

        ByteBuffer rgbaPixels = memAlloc(width * height * 4);
        for(int argbPixel : argbPixels)
        {
            rgbaPixels.put((byte) ((argbPixel >> 16) & 0xFF));
            rgbaPixels.put((byte) ((argbPixel >> 8) & 0xFF));
            rgbaPixels.put((byte) (argbPixel & 0xFF));
            rgbaPixels.put((byte) ((argbPixel >> 24) & 0xFF));
        }
        rgbaPixels.flip();
        return rgbaPixels;
    }

    public static Color colorForDisease(CityNode.DiseaseColor diseaseColor)
    {
        return switch (diseaseColor)
        {
            case BLUE -> new Color(69, 130, 236);
            case YELLOW -> new Color(235, 197, 62);
            case BLACK -> new Color(64, 64, 64);
            case RED -> new Color(225, 69, 69);
        };
    }

    private void drawCityLabel(Graphics2D g2d, FontMetrics metrics, String label, int labelX, int labelY, int imageWidth, int imageHeight)
    {
        int paddingX = 3;
        int paddingY = 2;
        int textWidth = metrics.stringWidth(label);
        int textHeight = metrics.getAscent() + metrics.getDescent();
        int boxX = Math.max(0, labelX - paddingX);
        int boxY = Math.max(0, labelY - metrics.getAscent() - paddingY);
        int boxWidth = Math.min(imageWidth - boxX, textWidth + (paddingX));
        int boxHeight = Math.min(imageHeight - boxY, textHeight + (paddingY));

        g2d.setColor(new Color(12, 24, 46, 145));
        g2d.fill(new RoundRectangle2D.Float(boxX, boxY, boxWidth, boxHeight, 6, 6));
        g2d.setColor(new Color(122, 169, 216, 150));
        g2d.draw(new RoundRectangle2D.Float(boxX, boxY, boxWidth, boxHeight, 6, 6));
        g2d.setColor(new Color(15, 20, 28, 210));
        g2d.drawString(label, labelX + 1, labelY + 1);
        g2d.setColor(Color.WHITE);
    }
}
