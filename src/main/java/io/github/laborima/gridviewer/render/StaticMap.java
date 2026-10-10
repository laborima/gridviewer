package io.github.laborima.gridviewer.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Locale;

import io.github.laborima.gridviewer.data.GridVariable;
import io.github.laborima.gridviewer.data.Slice;

/**
 * Renders a slice to a {@link BufferedImage} with Java2D, for the command line and exports.
 */
public final class StaticMap {

    private static final Color BACKGROUND = new Color(0x15181D);
    private static final Color TEXT = new Color(0xE6E9EF);
    private static final Color MUTED = new Color(0x9AA3B2);

    private StaticMap() {
    }

    /**
     * Draws the slice stretched to {@code width} by {@code mapHeight} pixels with nearest
     * neighbour sampling, plus a title and colour bar strip below when {@code legend} is set.
     */
    public static BufferedImage render(Slice slice, GridVariable variable, String subtitle, ColorMap map,
                                       ValueRange range, boolean flipRows, boolean maskZeros,
                                       int width, int mapHeight, boolean legend) {
        int[] argb = Rasterizer.colorize(slice.values(), slice.columns(), map, range, flipRows, maskZeros);
        BufferedImage raw = new BufferedImage(slice.columns(), slice.rows(), BufferedImage.TYPE_INT_ARGB);
        raw.setRGB(0, 0, slice.columns(), slice.rows(), argb, 0, slice.columns());

        int legendHeight = legend ? 92 : 0;
        BufferedImage out = new BufferedImage(width, mapHeight + legendHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(BACKGROUND);
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(raw, 0, 0, width, mapHeight, null);
        if (legend) {
            drawLegend(g, variable, subtitle, map, range, width, mapHeight);
        }
        g.dispose();
        return out;
    }

    private static void drawLegend(Graphics2D g, GridVariable variable, String subtitle, ColorMap map,
                                   ValueRange range, int width, int top) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int pad = 20;
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        g.setColor(TEXT);
        String units = variable.units().isEmpty() ? "" : "  [" + variable.units() + "]";
        g.drawString(variable.longName() + units, pad, top + 30);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        g.setColor(MUTED);
        g.drawString(subtitle, pad, top + 52);

        int barW = Math.min(420, width / 2);
        int barX = width - pad - barW;
        int barY = top + 18;
        for (int x = 0; x < barW; x++) {
            g.setColor(new Color(map.color(x / (double) (barW - 1))));
            g.fillRect(barX + x, barY, 1, 16);
        }
        g.setColor(MUTED);
        g.setStroke(new BasicStroke(1f));
        g.drawRect(barX, barY, barW - 1, 16);
        g.setColor(TEXT);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        for (int k = 0; k <= 4; k++) {
            double v = range.min() + k * (range.max() - range.min()) / 4;
            String s = format(v);
            int x = barX + k * (barW - 1) / 4 - g.getFontMetrics().stringWidth(s) / 2;
            x = Math.clamp(x, barX, barX + barW - g.getFontMetrics().stringWidth(s));
            g.drawString(s, x, barY + 34);
        }
    }

    static String format(double v) {
        double a = Math.abs(v);
        if (a != 0 && (a < 0.01 || a >= 1e5)) {
            return String.format(Locale.ROOT, "%.2e", v);
        }
        String s = String.format(Locale.ROOT, a >= 100 ? "%.0f" : a >= 10 ? "%.1f" : "%.2f", v);
        return s.equals("-0") || s.equals("-0.0") || s.equals("-0.00") ? s.substring(1) : s;
    }
}
