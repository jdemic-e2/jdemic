package jdemic.GameLogic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Screen-space hand / deck strip drawn into an offscreen image (uploaded as a Vulkan texture by the engine).
 * <p>
 * TODO: Replace {@link #fillMockCards()} with data from the active {@link Player} / session game state
 * when the rules layer and server (if any) expose the real hand for the local player.
 */
public final class CardDeckUI
{

    private final List<Card> cards = new ArrayList<>();
    private final List<Rectangle> cardBounds = new ArrayList<>();

    private int hoveredIndex = -1;
    private int selectedIndex = -1;
    private float selectionLiftPx;
    private float targetLiftPx;

    private int rasterW;
    private int rasterH;

    private static final float LIFT_SELECTED = 40f;
    private static final float HOVER_LIFT = 6f;
    private static final double LERP_PER_SEC = 14.0;

    public CardDeckUI()
    {
        fillMockCards();
    }

    /**
     * Call after swap chain (re)creation so layout matches framebuffer width and band height.
     */
    public void onSwapChainResize(int framebufferWidthPx, int framebufferHeightPx)
    {
        this.rasterW = framebufferWidthPx;
        // Hand strip height drives how large cards can appear. /4 was too small at full window.
        // Use a large fraction of the framebuffer (cap so tiny windows still work).
        // Align with Main deck quad: ~38% of framebuffer height (NDC span 0.76 / 2).
        int fbH = Math.max(1, framebufferHeightPx);
        int target = (int) Math.round(fbH * 0.38);
        int capped = Math.min(fbH - 24, target);
        this.rasterH = Math.max(160, capped);
    }

    public int getRasterWidth()
    {
        return rasterW;
    }

    public int getRasterHeight()
    {
        return rasterH;
    }

    /** Temporary hand until real game state is wired. */
    private void fillMockCards()
    {
        cards.clear();
        cards.add(new Card(1, "Atlanta", CardType.CITY, new Color(69, 130, 236), 0, 0));
        cards.add(new Card(2, "Resilient", CardType.EVENT, new Color(180, 90, 200), 0, 0));
        cards.add(new Card(3, "Tokyo", CardType.CITY, new Color(225, 69, 69), 0, 0));
        cards.add(new Card(4, "Infection", CardType.INFECTION, new Color(120, 120, 120), 0, 0));
    }

    /**
     * TODO: Invoke when game logic provides the authoritative list of cards for the current player.
     */
    public void setHandFromGameState(List<Card> hand)
    {
        cards.clear();
        cards.addAll(hand);
        selectedIndex = -1;
        targetLiftPx = 0f;
        selectionLiftPx = 0f;
    }

    public void update(double dt)
    {
        float diff = targetLiftPx - selectionLiftPx;
        if(Math.abs(diff) < 0.4f)
        {
            selectionLiftPx = targetLiftPx;
        }
        else
        {
            selectionLiftPx += diff * (float) Math.min(1.0, dt * LERP_PER_SEC);
        }
    }

    /**
     * @param localX x in deck raster space (0..rasterW)
     * @param localY y in deck raster space (0..rasterH)
     * @return true if hover state changed (caller should refresh GPU texture)
     */
    public boolean handleHover(int localX, int localY)
    {
        int nextHover = -1;
        for(int i = 0; i < cardBounds.size(); i++)
        {
            if(cardBounds.get(i).contains(localX, localY))
            {
                nextHover = i;
                break;
            }
        }
        if(nextHover != hoveredIndex)
        {
            hoveredIndex = nextHover;
            return true;
        }
        return false;
    }

    /** @return true if hover state changed (caller should refresh GPU texture) */
    public boolean clearHover()
    {
        if(hoveredIndex != -1)
        {
            hoveredIndex = -1;
            return true;
        }
        return false;
    }

    /**
     * @param localX x in deck raster space (0..rasterW)
     * @param localY y in deck raster space (0..rasterH)
     * @return true if the click was handled by the deck (including clear selection on empty area)
     */
    public boolean handleClick(int localX, int localY)
    {
        for(int i = 0; i < cardBounds.size(); i++)
        {
            if(cardBounds.get(i).contains(localX, localY))
            {
                if(selectedIndex == i)
                {
                    selectedIndex = -1;
                    targetLiftPx = 0f;
                }
                else
                {
                    selectedIndex = i;
                    targetLiftPx = LIFT_SELECTED;
                }
                return true;
            }
        }
        if(selectedIndex != -1)
        {
            selectedIndex = -1;
            targetLiftPx = 0f;
            return true;
        }
        return false;
    }

    public boolean isAnimatingSelection()
    {
        return Math.abs(targetLiftPx - selectionLiftPx) > 0.5f;
    }

    public boolean isHoveringCard()
    {
        return hoveredIndex != -1 && hoveredIndex < cards.size();
    }

    public String getHoveredEffectDescription()
    {
        if(!isHoveringCard())
        {
            return null;
        }
        return cards.get(hoveredIndex).getEffectDescription();
    }

    /**
     * Paints the current hand into {@code target} (expected size = rasterW x rasterH).
     */
    public void render(BufferedImage target)
    {
        Graphics2D g = target.createGraphics();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = target.getWidth();
            int h = target.getHeight();

            // Background panel (dark tabletop HUD)
            g.setPaint(new GradientPaint(0, 0, new Color(12, 13, 18), 0, h, new Color(6, 6, 9)));
            g.fillRect(0, 0, w, h);
            drawHexPattern(g, w, h);
            g.setColor(new Color(255, 255, 255, 10));
            g.fillRect(0, 0, w, Math.max(2, h / 16));
            g.setColor(new Color(255, 255, 255, 18));
            g.drawLine(0, 0, w, 0);

            cardBounds.clear();
            int n = cards.size();
            if(n == 0)
            {
                return;
            }

            // === Responsive layout (readable first, fit always) ===
            // Priority order:
            // 1) reduce gap
            // 2) allow slight overlap / fan
            // 3) small scale reduction (down to ~0.78)
            // 4) only last resort scale smaller than minScale

            int padX = Math.max(12, w / 42);
            int padTop = Math.max(8, h / 28);
            int padBottom = Math.max(8, h / 22);

            int availableWidth = Math.max(1, w - padX * 2);
            int availableHeight = Math.max(1, h - padTop - padBottom);

            // Base (full-size) card metrics. These define how big the hand looks in a normal window.
            // Responsive logic will shrink/overlap only as needed.
            float baseCardW = 248f;
            float baseCardH = 345f;
            int baseGapPx = 14;
            int minGapPx = 4;
            float maxOverlapFrac = 0.20f; // allow up to 20% overlap => step >= 0.8 * cardW

            // Reserve vertical space so selected/hovered card never clips at the top.
            float liftReserve = LIFT_SELECTED + HOVER_LIFT + 10f;

            float minScalePreferred = 0.78f; // don't go below unless absolutely necessary
            float hardMinScale = 0.50f;      // emergency fallback

            // Vertical constraint (independent of width)
            float scaleY = Math.min(1.0f, (availableHeight - liftReserve) / Math.max(1.0f, baseCardH));

            float chosenScale = -1f;
            int cardW = 0, cardH = 0, step = 0, totalSpan = 0;

            // Try scales from 1.0 down to preferred minScale, fitting mostly by gap/overlap.
            for(float s = 1.0f; s >= minScalePreferred - 1e-6; s -= 0.02f)
            {
                float scale = Math.min(s, scaleY);
                int cw = Math.max(64, Math.round(baseCardW * scale));
                int ch = Math.max(96, Math.round(baseCardH * scale));

                // Try with normal gap, then reduced gap.
                int bestStep = Integer.MAX_VALUE;
                int bestSpan = Integer.MAX_VALUE;

                for(int gapPx : new int[] { baseGapPx, minGapPx })
                {
                    int wantedStep = cw + gapPx;
                    int fitStep = (n <= 1) ? 0 : (int) Math.floor((availableWidth - cw) / (double) (n - 1));
                    int minStep = (int) Math.ceil(cw * (1.0f - maxOverlapFrac)); // overlap cap

                    int st = (n <= 1) ? 0 : Math.max(minStep, Math.min(wantedStep, fitStep));
                    int span = (n <= 1) ? cw : (cw + st * (n - 1));

                    if(span <= availableWidth && st < bestStep)
                    {
                        bestStep = st;
                        bestSpan = span;
                    }
                }

                if(bestSpan <= availableWidth)
                {
                    chosenScale = scale;
                    cardW = cw;
                    cardH = ch;
                    step = (n <= 1) ? 0 : bestStep;
                    totalSpan = (n <= 1) ? cardW : bestSpan;
                    break;
                }
            }

            // If still not fitting, allow scale below preferred minScale as last resort.
            if(chosenScale < 0f)
            {
                for(float s = minScalePreferred; s >= hardMinScale - 1e-6; s -= 0.03f)
                {
                    float scale = Math.min(s, scaleY);
                    int cw = Math.max(48, Math.round(baseCardW * scale));
                    int ch = Math.max(80, Math.round(baseCardH * scale));

                    int fitStep = (n <= 1) ? 0 : (int) Math.floor((availableWidth - cw) / (double) (n - 1));
                    // In emergency mode we allow more overlap than the preferred 20%.
                    int minStep = Math.max(1, (int) Math.ceil(cw * 0.60f)); // up to 40% overlap
                    int st = (n <= 1) ? 0 : Math.max(minStep, fitStep);

                    int span = (n <= 1) ? cw : (cw + st * (n - 1));
                    if(span <= availableWidth)
                    {
                        chosenScale = scale;
                        cardW = cw;
                        cardH = ch;
                        step = (n <= 1) ? 0 : st;
                        totalSpan = span;
                        break;
                    }
                }
            }

            // Absolute fallback: pack as tightly as needed.
            if(chosenScale < 0f)
            {
                chosenScale = Math.max(hardMinScale, Math.min(1.0f, scaleY));
                cardW = Math.max(42, Math.min(availableWidth, Math.round(baseCardW * chosenScale)));
                cardH = Math.max(72, Math.round(baseCardH * chosenScale));
                step = (n <= 1) ? 0 : Math.max(1, (availableWidth - cardW) / (n - 1));
                totalSpan = (n <= 1) ? cardW : (cardW + step * (n - 1));
            }

            int startX = padX + Math.max(0, (availableWidth - totalSpan) / 2);
            int baseY = padTop + Math.round(liftReserve);
            float lift = selectionLiftPx;

            for(int i = 0; i < n; i++)
            {
                int liftNow = (i == selectedIndex) ? Math.round(lift) : 0;
                int hoverNow = (i == hoveredIndex) ? (int) HOVER_LIFT : 0;
                int x = startX + i * step;
                int y = baseY - liftNow - hoverNow;

                boolean selected = i == selectedIndex;
                boolean hovered = i == hoveredIndex;

                // Slight “scale” feeling by drawing a bigger rect but keeping layout stable.
                int inflate = hovered ? Math.max(4, cardW / 24) : 0;
                int dx = hovered ? -inflate / 2 : 0;
                int dy = hovered ? -inflate / 2 : 0;
                int rw = cardW + inflate;
                int rh = cardH + inflate;

                // Clamp X so hover inflate never causes horizontal clipping.
                int clampedX = Math.max(padX, Math.min(x + dx, w - padX - rw));
                int clampedY = Math.max(padTop, Math.min(y + dy, h - padBottom - rh));

                // Hitbox should match what we draw
                cardBounds.add(new Rectangle(clampedX, clampedY, rw, rh));
                drawCard(g, cards.get(i), clampedX, clampedY, rw, rh, selected, hovered);
            }

            // Tooltip: show hovered card effect description
            if(isHoveringCard())
            {
                Rectangle hb = cardBounds.get(hoveredIndex);
                drawCardTooltip(g, cards.get(hoveredIndex), hb.x + hb.width / 2, hb.y - 12, w, h);
            }
        }
        finally
        {
            g.dispose();
        }
    }

    private static void drawCard(Graphics2D g, Card c, int x, int y, int w, int h, boolean selected, boolean hovered)
    {
        CardPalette pal = paletteFor(c);
        int r = 18;

        RoundRectangle2D outer = new RoundRectangle2D.Float(x, y, w, h, r, r);
        RoundRectangle2D inner = new RoundRectangle2D.Float(x + 4, y + 4, w - 8, h - 8, r - 4, r - 4);

        // Shadow + lift depth
        int shadowDx = selected ? 4 : 3;
        int shadowDy = selected ? 8 : 6;
        g.setColor(new Color(0, 0, 0, selected ? 110 : 85));
        g.fill(new RoundRectangle2D.Float(x + shadowDx, y + shadowDy, w, h, r, r));

        // Frame
        g.setPaint(new GradientPaint(x, y, pal.frameTop, x, y + h, pal.frameBottom));
        g.fill(outer);

        // Card paper / panel
        g.setPaint(new GradientPaint(x, y, pal.paperTop, x, y + h, pal.paperBottom));
        g.fill(inner);

        // Type-specific pattern layer (subtle)
        drawTypePattern(g, c.getCardType(), x + 6, y + 44, w - 12, h - 70, pal);

        // Header ribbon
        int headerH = Math.max(36, h / 7);
        g.setPaint(new GradientPaint(x, y, pal.ribbonTop, x, y + headerH, pal.ribbonBottom));
        g.fillRoundRect(x + 6, y + 6, w - 12, headerH, 14, 14);
        g.setColor(new Color(255, 255, 255, 22));
        g.drawRoundRect(x + 6, y + 6, w - 12, headerH, 14, 14);

        // Corner badge + icon
        int badgeSize = Math.max(26, w / 6);
        int bx = x + 12;
        int by = y + 12;
        drawTypeBadge(g, c.getCardType(), bx, by, badgeSize, pal);

        // Title: always fit inside ribbon (pixel-accurate; shrink font, wrap 2 lines, or ellipsis).
        int titleLeft = x + badgeSize + 20;
        int titleMaxW = Math.max(4, w - 12 - 10 - (titleLeft - x));
        int titleMaxPt = Math.max(18, w / 7);
        drawHeaderTitle(g, c.getName(), pal.titleColor, titleLeft, y, headerH, titleMaxW, titleMaxPt, 9);

        // Footer type plate
        String typeText = switch (c.getCardType())
        {
            case CITY -> "CITY";
            case EVENT -> "EVENT";
            case INFECTION -> "INFECTION";
        };
        Font plateFont = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(13, w / 9));
        g.setFont(plateFont);
        FontMetrics fm = g.getFontMetrics();
        int plateW = Math.min(w - 24, fm.stringWidth(typeText) + 22);
        int plateH = Math.max(22, fm.getAscent() + fm.getDescent() + 8);
        int plateX = x + Math.max(12, (w - plateW) / 2);
        int plateY = y + h - plateH - 12;
        g.setColor(new Color(10, 12, 18, 170));
        g.fillRoundRect(plateX, plateY, plateW, plateH, 14, 14);
        g.setColor(new Color(255, 255, 255, 30));
        g.drawRoundRect(plateX, plateY, plateW, plateH, 14, 14);
        g.setColor(new Color(245, 245, 245));
        g.drawString(typeText, plateX + 11, plateY + fm.getAscent() + 3);

        // Hover + selected glow
        if(hovered || selected)
        {
            Stroke prev = g.getStroke();
            g.setStroke(new BasicStroke(selected ? 4.5f : 3.2f));
            g.setColor(selected ? new Color(255, 220, 120, 230) : new Color(170, 220, 255, 175));
            g.draw(new RoundRectangle2D.Float(x - 2, y - 2, w + 4, h + 4, r + 4, r + 4));
            g.setStroke(prev);
        }

        // Thin ink outline
        g.setColor(new Color(0, 0, 0, 100));
        g.setStroke(new BasicStroke(2f));
        g.draw(outer);
    }

    /**
     * Draws the card name in the header ribbon so it never extends past {@code titleMaxW}:
     * smaller font, then two lines, then ellipsis — all measured in pixels (not character count).
     */
    private static void drawHeaderTitle(Graphics2D g, String rawName, Color color,
                                        int textLeft, int y, int headerH, int titleMaxW,
                                        int maxPt, int minPt)
    {
        String name = rawName == null ? "" : rawName.toUpperCase(Locale.ROOT);
        if(name.isEmpty())
        {
            return;
        }
        g.setColor(color);
        Object aa = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        for(int pt = maxPt; pt >= minPt; pt--)
        {
            Font f = new Font(Font.SANS_SERIF, Font.BOLD, pt);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            if(fm.stringWidth(name) <= titleMaxW)
            {
                int baseline = y + 6 + (headerH - fm.getHeight()) / 2 + fm.getAscent();
                g.drawString(name, textLeft, baseline);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, aa);
                return;
            }
        }

        for(int pt = Math.min(maxPt, 16); pt >= minPt; pt--)
        {
            Font f = new Font(Font.SANS_SERIF, Font.BOLD, pt);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            List<String> lines = breakTwoLines(name, fm, titleMaxW);
            int lh = fm.getHeight();
            int totalH = lines.size() * lh;
            if(totalH > headerH - 2 && pt > minPt)
            {
                continue;
            }
            int baseline = y + 6 + Math.max(0, (headerH - totalH) / 2) + fm.getAscent();
            for(String line : lines)
            {
                g.drawString(truncateToPixelWidth(line, fm, titleMaxW), textLeft, baseline);
                baseline += lh;
            }
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, aa);
            return;
        }

        Font f = new Font(Font.SANS_SERIF, Font.BOLD, minPt);
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        int baseline = y + 6 + (headerH - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(truncateToPixelWidth(name, fm, titleMaxW), textLeft, baseline);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, aa);
    }

    /** Longest {@code n} with {@code fm.stringWidth(s.substring(0,n)) <= maxW}. */
    private static int longestFittingPrefixLen(String s, FontMetrics fm, int maxW)
    {
        int lo = 0, hi = s.length();
        while(lo < hi)
        {
            int mid = (lo + hi + 1) / 2;
            if(fm.stringWidth(s.substring(0, mid)) <= maxW)
            {
                lo = mid;
            }
            else
            {
                hi = mid - 1;
            }
        }
        return lo;
    }

    private static List<String> breakTwoLines(String text, FontMetrics fm, int maxW)
    {
        text = text.trim();
        List<String> out = new ArrayList<>(2);
        if(text.isEmpty())
        {
            out.add("");
            return out;
        }
        if(fm.stringWidth(text) <= maxW)
        {
            out.add(text);
            return out;
        }
        for(int sp = text.indexOf(' '); sp != -1; sp = text.indexOf(' ', sp + 1))
        {
            String a = text.substring(0, sp);
            String b = text.substring(sp + 1).trim();
            if(!b.isEmpty() && fm.stringWidth(a) <= maxW && fm.stringWidth(b) <= maxW)
            {
                out.add(a);
                out.add(b);
                return out;
            }
        }
        int cut = longestFittingPrefixLen(text, fm, maxW);
        if(cut <= 0)
        {
            cut = 1;
        }
        String line1 = text.substring(0, cut);
        String line2 = text.substring(cut).trim();
        if(line2.isEmpty())
        {
            out.add(truncateToPixelWidth(line1, fm, maxW));
            return out;
        }
        out.add(line1);
        out.add(line2);
        return out;
    }

    private static String truncateToPixelWidth(String s, FontMetrics fm, int maxW)
    {
        if(s == null || s.isEmpty())
        {
            return "";
        }
        if(fm.stringWidth(s) <= maxW)
        {
            return s;
        }
        String ell = "…";
        int ellW = fm.stringWidth(ell);
        if(ellW > maxW)
        {
            return "";
        }
        int lo = 0, hi = s.length();
        while(lo < hi)
        {
            int mid = (lo + hi + 1) / 2;
            String t = s.substring(0, mid) + ell;
            if(fm.stringWidth(t) <= maxW)
            {
                lo = mid;
            }
            else
            {
                hi = mid - 1;
            }
        }
        return lo <= 0 ? ell : s.substring(0, lo) + ell;
    }

    private static void drawCardTooltip(Graphics2D g, Card c, int anchorX, int anchorY, int imgW, int imgH)
    {
        String title = c.getName();
        String type = c.getCardType().name();
        String body = c.getEffectDescription();

        int maxWidth = Math.min(420, Math.max(240, imgW / 3));
        Font titleFont = new Font(Font.SANS_SERIF, Font.BOLD, 14);
        Font bodyFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

        g.setFont(bodyFont);
        FontMetrics fm = g.getFontMetrics();

        List<String> lines = wrapText(body, fm, maxWidth - 26);
        int lineH = fm.getAscent() + fm.getDescent();

        g.setFont(titleFont);
        FontMetrics tfm = g.getFontMetrics();
        int headH = tfm.getAscent() + tfm.getDescent() + 10;

        int tooltipH = 12 + headH + lines.size() * lineH;
        int tooltipW = 26 + Math.max(tfm.stringWidth(title), fm.stringWidth(type));
        for(String line : lines)
        {
            tooltipW = Math.max(tooltipW, fm.stringWidth(line) + 26);
        }
        tooltipW = Math.min(maxWidth, tooltipW);

        int x = Math.max(12, Math.min(anchorX - tooltipW / 2, imgW - tooltipW - 12));
        int y = Math.max(10, anchorY - tooltipH);
        if(y < 10)
        {
            y = Math.min(imgH - tooltipH - 10, anchorY + 10);
        }

        // Shadow
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(x + 3, y + 5, tooltipW, tooltipH, 14, 14);

        // Panel
        g.setColor(new Color(15, 17, 24, 235));
        g.fillRoundRect(x, y, tooltipW, tooltipH, 14, 14);
        g.setColor(new Color(255, 255, 255, 34));
        g.drawRoundRect(x, y, tooltipW, tooltipH, 14, 14);

        // Header strip
        g.setPaint(new GradientPaint(x, y, new Color(40, 44, 60, 180), x, y + headH + 6, new Color(18, 20, 28, 0)));
        g.fillRoundRect(x + 2, y + 2, tooltipW - 4, headH + 8, 12, 12);

        g.setFont(titleFont);
        g.setColor(new Color(250, 250, 250));
        int ty = y + 10 + tfm.getAscent();
        g.drawString(ellipsize(title, 32), x + 12, ty);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g.setColor(new Color(200, 210, 220));
        g.drawString(type, x + 12, ty + 16);

        g.setFont(bodyFont);
        g.setColor(new Color(235, 235, 235));
        ty = y + 12 + headH + fm.getAscent();
        for(String line : lines)
        {
            g.drawString(line, x + 11, ty);
            ty += lineH;
        }
    }

    private static List<String> wrapText(String text, FontMetrics fm, int maxWidth)
    {
        String[] words = text.trim().split("\\s+");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for(String w : words)
        {
            if(current.isEmpty())
            {
                current.append(w);
                continue;
            }
            String candidate = current + " " + w;
            if(fm.stringWidth(candidate) <= maxWidth)
            {
                current.append(' ').append(w);
            }
            else
            {
                lines.add(current.toString());
                current.setLength(0);
                current.append(w);
            }
        }
        if(!current.isEmpty())
        {
            lines.add(current.toString());
        }
        return lines;
    }

    private static String ellipsize(String s, int maxChars)
    {
        if(s == null)
        {
            return "";
        }
        if(s.length() <= maxChars)
        {
            return s;
        }
        return s.substring(0, Math.max(1, maxChars - 1)) + "…";
    }

    private static Color blend(Color a, Color b, float t)
    {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (a.getRed() * (1 - t) + b.getRed() * t);
        int g = (int) (a.getGreen() * (1 - t) + b.getGreen() * t);
        int bl = (int) (a.getBlue() * (1 - t) + b.getBlue() * t);
        return new Color(r, g, bl, 255);
    }

    private static void drawHexPattern(Graphics2D g, int w, int h)
    {
        g.setColor(new Color(255, 255, 255, 10));
        int step = 24;
        for(int y = 0; y < h; y += step)
        {
            int offset = (y / step) % 2 == 0 ? 0 : step / 2;
            for(int x = -offset; x < w; x += step)
            {
                g.drawOval(x + offset, y, 2, 2);
            }
        }
    }

    private static void drawTypePattern(Graphics2D g, CardType type, int x, int y, int w, int h, CardPalette pal)
    {
        g.setColor(pal.patternInk);
        switch (type)
        {
            case CITY -> {
                // light grid lines
                int step = 18;
                for(int yy = y; yy < y + h; yy += step)
                {
                    g.drawLine(x, yy, x + w, yy);
                }
                for(int xx = x; xx < x + w; xx += step)
                {
                    g.drawLine(xx, y, xx, y + h);
                }
            }
            case EVENT -> {
                // sparkles / diagonal accents
                for(int i = 0; i < 10; i++)
                {
                    int xx = x + (i * w) / 10;
                    g.drawLine(xx, y + h, xx + 22, y + h - 22);
                }
            }
            case INFECTION -> {
                // hazard stripes
                int stripe = 16;
                for(int i = -h; i < w + h; i += stripe)
                {
                    g.drawLine(x + i, y + h, x + i + h, y);
                }
            }
        }
    }

    private static void drawTypeBadge(Graphics2D g, CardType type, int x, int y, int size, CardPalette pal)
    {
        int r = 10;
        g.setColor(pal.badgeFill);
        g.fillRoundRect(x, y, size, size, r, r);
        g.setColor(new Color(255, 255, 255, 40));
        g.drawRoundRect(x, y, size, size, r, r);

        // Simple icon shapes (no assets)
        g.setColor(pal.badgeIcon);
        int cx = x + size / 2;
        int cy = y + size / 2;
        switch (type)
        {
            case CITY -> {
                // location pin
                g.fillOval(cx - 5, cy - 6, 10, 10);
                Polygon p = new Polygon();
                p.addPoint(cx, cy + 10);
                p.addPoint(cx - 7, cy + 2);
                p.addPoint(cx + 7, cy + 2);
                g.fillPolygon(p);
                g.setColor(pal.badgeFill);
                g.fillOval(cx - 2, cy - 3, 4, 4);
            }
            case EVENT -> {
                // star
                Polygon star = new Polygon();
                star.addPoint(cx, cy - 10);
                star.addPoint(cx + 3, cy - 2);
                star.addPoint(cx + 11, cy - 2);
                star.addPoint(cx + 5, cy + 3);
                star.addPoint(cx + 7, cy + 11);
                star.addPoint(cx, cy + 6);
                star.addPoint(cx - 7, cy + 11);
                star.addPoint(cx - 5, cy + 3);
                star.addPoint(cx - 11, cy - 2);
                star.addPoint(cx - 3, cy - 2);
                g.fillPolygon(star);
            }
            case INFECTION -> {
                // warning triangle
                Polygon tri = new Polygon();
                tri.addPoint(cx, cy - 11);
                tri.addPoint(cx - 12, cy + 10);
                tri.addPoint(cx + 12, cy + 10);
                g.fillPolygon(tri);
                g.setColor(pal.badgeFill);
                g.fillRect(cx - 1, cy - 3, 2, 7);
                g.fillRect(cx - 1, cy + 6, 2, 2);
            }
        }
    }

    private static CardPalette paletteFor(Card c)
    {
        return switch (c.getCardType())
        {
            case CITY -> CardPalette.city(c.getCityColor());
            case EVENT -> CardPalette.event();
            case INFECTION -> CardPalette.infection();
        };
    }

    private record CardPalette(
        Color frameTop,
        Color frameBottom,
        Color paperTop,
        Color paperBottom,
        Color ribbonTop,
        Color ribbonBottom,
        Color patternInk,
        Color titleColor,
        Color badgeFill,
        Color badgeIcon
    )
    {
        static CardPalette city(Color accent)
        {
            return new CardPalette(
                blend(accent, Color.WHITE, 0.18f),
                blend(accent, Color.BLACK, 0.45f),
                new Color(232, 235, 238),
                new Color(200, 205, 212),
                blend(accent, Color.BLACK, 0.25f),
                blend(accent, Color.BLACK, 0.55f),
                new Color(0, 0, 0, 12),
                new Color(250, 250, 250),
                blend(accent, Color.BLACK, 0.35f),
                new Color(245, 245, 245)
            );
        }

        static CardPalette event()
        {
            Color purple = new Color(135, 82, 190);
            Color gold = new Color(225, 190, 92);
            return new CardPalette(
                blend(purple, gold, 0.20f),
                blend(purple, Color.BLACK, 0.55f),
                new Color(230, 228, 238),
                new Color(198, 194, 210),
                blend(purple, Color.BLACK, 0.20f),
                blend(purple, Color.BLACK, 0.55f),
                new Color(70, 40, 90, 20),
                new Color(255, 245, 220),
                blend(gold, Color.BLACK, 0.30f),
                new Color(20, 12, 24)
            );
        }

        static CardPalette infection()
        {
            Color red = new Color(205, 72, 72);
            return new CardPalette(
                new Color(70, 70, 74),
                new Color(22, 22, 24),
                new Color(210, 210, 210),
                new Color(170, 170, 172),
                blend(red, Color.BLACK, 0.30f),
                blend(red, Color.BLACK, 0.65f),
                new Color(0, 0, 0, 18),
                new Color(255, 245, 235),
                blend(red, Color.BLACK, 0.35f),
                new Color(255, 235, 210)
            );
        }
    }
}
