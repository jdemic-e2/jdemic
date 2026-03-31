package jdemic.Rendering;

import jdemic.GameLogic.CityNode;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EdgeRenderer
{
    public static final int MAX_HOVER_EDGE_SEGMENTS = 8;
    private static final float WRAP_THRESHOLD = 0.45f;
    private static final Color EDGE_COLOR = new Color(0, 162, 255, 1);
    private static final Color EDGE_OUTLINE_COLOR = new Color(12, 35, 70, 150);

    public static void drawEdges(Graphics2D g2d, List<MapRenderer.AlignedCityNode> alignedNodes,
                                 int imageWidth, int imageHeight, float borderFraction)
    {
        int borderW = Math.max(6, (int)(imageWidth * borderFraction));
        int borderH = Math.max(6, (int)(imageHeight * borderFraction));
        int mapW = imageWidth - 2 * borderW;
        int mapH = imageHeight - 2 * borderH;

        float strokeWidth = Math.max(2.5f, imageWidth / 600.0f);
        BasicStroke edgeOutlineStroke = new BasicStroke(strokeWidth + 1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke edgeStroke = new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Set<String> drawnEdges = new HashSet<>();

        for (MapRenderer.AlignedCityNode alignedNode : alignedNodes)
        {
            CityNode city = alignedNode.city;
            int x1 = borderW + (int)((float) alignedNode.pixelX / imageWidth * mapW);
            int y1 = borderH + (int)((float) alignedNode.pixelY / imageHeight * mapH);

            for (CityNode neighbor : city.getConnectedCities())
            {
                String edgeKey = edgeKey(city.getName(), neighbor.getName());
                if (!drawnEdges.add(edgeKey))
                {
                    continue;
                }

                MapRenderer.AlignedCityNode neighborAligned = findAlignedNode(alignedNodes, neighbor);
                if (neighborAligned == null)
                {
                    continue;
                }

                int x2 = borderW + (int)((float) neighborAligned.pixelX / imageWidth * mapW);
                int y2 = borderH + (int)((float) neighborAligned.pixelY / imageHeight * mapH);

                if (isWrapAroundEdge(alignedNode, neighborAligned))
                {
                    drawWrapAroundEdge(g2d, x1, y1, x2, y2, imageWidth, borderW, borderW + mapW,
                                       edgeOutlineStroke, edgeStroke);
                }
                else
                {
                    g2d.setStroke(edgeOutlineStroke);
                    g2d.setColor(EDGE_OUTLINE_COLOR);
                    g2d.drawLine(x1, y1, x2, y2);

                    g2d.setStroke(edgeStroke);
                    g2d.setColor(EDGE_COLOR);
                    g2d.drawLine(x1, y1, x2, y2);
                }
            }
        }
    }

    public static List<float[]> computeHoverEdgeSegments(List<MapRenderer.AlignedCityNode> alignedNodes, int hoveredIndex, float borderFraction)
    {
        List<float[]> segments = new ArrayList<>();
        if (hoveredIndex < 0 || hoveredIndex >= alignedNodes.size())
        {
            return segments;
        }

        MapRenderer.AlignedCityNode hoveredNode = alignedNodes.get(hoveredIndex);
        float hU = borderFraction + hoveredNode.mapU * (1.0f - 2.0f * borderFraction);
        float hV = borderFraction + hoveredNode.mapV * (1.0f - 2.0f * borderFraction);

        for (CityNode neighbor : hoveredNode.city.getConnectedCities())
        {
            MapRenderer.AlignedCityNode neighborAligned = findAlignedNode(alignedNodes, neighbor);
            if (neighborAligned == null)
            {
                continue;
            }

            float nU = borderFraction + neighborAligned.mapU * (1.0f - 2.0f * borderFraction);
            float nV = borderFraction + neighborAligned.mapV * (1.0f - 2.0f * borderFraction);

            if (isWrapAroundEdge(hoveredNode, neighborAligned))
            {
                float leftU, leftV, rightU, rightV;
                if (hU < nU)
                {
                    leftU = hU; leftV = hV;
                    rightU = nU; rightV = nV;
                }
                else
                {
                    leftU = nU; leftV = nV;
                    rightU = hU; rightV = hV;
                }

                float wrapDist = leftU + (1.0f - rightU);
                if (wrapDist > 0.001f)
                {
                    float leftFrac = leftU / wrapDist;
                    float edgeV = leftV + leftFrac * (rightV - leftV);

                    if (segments.size() < MAX_HOVER_EDGE_SEGMENTS)
                    {
                        segments.add(new float[]{leftU, leftV, 0.0f, edgeV});
                    }
                    if (segments.size() < MAX_HOVER_EDGE_SEGMENTS)
                    {
                        segments.add(new float[]{rightU, rightV, 1.0f, edgeV});
                    }
                }
            }
            else
            {
                if (segments.size() < MAX_HOVER_EDGE_SEGMENTS)
                {
                    segments.add(new float[]{hU, hV, nU, nV});
                }
            }
        }

        return segments;
    }

    private static boolean isWrapAroundEdge(MapRenderer.AlignedCityNode a, MapRenderer.AlignedCityNode b)
    {
        float du = Math.abs(a.mapU - b.mapU);
        return du > WRAP_THRESHOLD;
    }

    private static void drawWrapAroundEdge(Graphics2D g2d, int x1, int y1, int x2, int y2,
                                           int imageWidth, int mapLeft, int mapRight,
                                           BasicStroke outlineStroke, BasicStroke edgeStroke)
    {
        int leftNode_X, leftNode_Y, rightNode_X, rightNode_Y;
        if (x1 < x2)
        {
            leftNode_X = x1; leftNode_Y = y1;
            rightNode_X = x2; rightNode_Y = y2;
        }
        else
        {
            leftNode_X = x2; leftNode_Y = y2;
            rightNode_X = x1; rightNode_Y = y1;
        }

        int wrapDist = (leftNode_X - mapLeft) + (mapRight - rightNode_X);
        if (wrapDist == 0)
        {
            return;
        }

        float leftFraction = (float)(leftNode_X - mapLeft) / wrapDist;
        int edgeY = leftNode_Y + (int)(leftFraction * (rightNode_Y - leftNode_Y));

        drawEdgeLine(g2d, leftNode_X, leftNode_Y, 0, edgeY, outlineStroke, edgeStroke);
        drawEdgeLine(g2d, rightNode_X, rightNode_Y, imageWidth, edgeY, outlineStroke, edgeStroke);
    }

    private static void drawEdgeLine(Graphics2D g2d, int x1, int y1, int x2, int y2,
                                     BasicStroke outlineStroke, BasicStroke edgeStroke)
    {
        g2d.setStroke(outlineStroke);
        g2d.setColor(EDGE_OUTLINE_COLOR);
        g2d.drawLine(x1, y1, x2, y2);

        g2d.setStroke(edgeStroke);
        g2d.setColor(EDGE_COLOR);
        g2d.drawLine(x1, y1, x2, y2);
    }

    static MapRenderer.AlignedCityNode findAlignedNode(List<MapRenderer.AlignedCityNode> alignedNodes, CityNode target)
    {
        for (MapRenderer.AlignedCityNode aligned : alignedNodes)
        {
            if (aligned.city == target)
            {
                return aligned;
            }
        }
        return null;
    }

    private static String edgeKey(String a, String b)
    {
        if (a.compareTo(b) < 0)
        {
            return a + "|" + b;
        }
        return b + "|" + a;
    }
}
