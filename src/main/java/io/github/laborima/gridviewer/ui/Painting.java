package io.github.laborima.gridviewer.ui;

import java.util.Locale;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import io.github.laborima.gridviewer.render.ColorMap;
import io.github.laborima.gridviewer.render.ValueRange;

/**
 * Drawing helpers shared by the canvas views.
 */
final class Painting {

    static final Color BACKGROUND = Color.web("#111418");
    static final Color CARD = Color.web("#171b21", 0.86);
    static final Color TEXT = Color.web("#e6e9ef");
    static final Color MUTED = Color.web("#9aa3b2");
    static final Color ACCENT = Color.web("#4cc9f0");
    static final Color HIGHLIGHT = Color.web("#ffd166");
    static final Font SMALL = Font.font("System", 11);
    static final Font LABEL = Font.font("System", FontWeight.SEMI_BOLD, 12);

    private Painting() {
    }

    /** Image of {@code columns} by {@code argb.length / columns} opaque pixels. */
    static WritableImage image(int[] argb, int columns) {
        int rows = argb.length / columns;
        WritableImage img = new WritableImage(columns, rows);
        img.getPixelWriter().setPixels(0, 0, columns, rows, PixelFormat.getIntArgbPreInstance(), argb, 0, columns);
        return img;
    }

    /** Strip of the colour map, to be stretched with smoothing on. */
    static WritableImage strip(ColorMap map) {
        // A few identical rows: a single row blends with the transparent border when stretched.
        int rows = 4;
        int[] argb = new int[ColorMap.SIZE * rows];
        for (int r = 0; r < rows; r++) {
            System.arraycopy(map.argb(), 0, argb, r * ColorMap.SIZE, ColorMap.SIZE);
        }
        return image(argb, ColorMap.SIZE);
    }

    /**
     * Floating colour bar card anchored at its bottom-right corner.
     */
    static void colorBar(GraphicsContext gc, double right, double bottom, ColorMap map, ValueRange range,
                         String title) {
        double barW = 260;
        double w = barW + 28;
        double h = 62;
        double x = right - w;
        double y = bottom - h;
        gc.setFill(CARD);
        gc.fillRoundRect(x, y, w, h, 12, 12);
        gc.setFont(LABEL);
        gc.setFill(TEXT);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        gc.fillText(ellipsize(title, LABEL, barW), x + 14, y + 8);
        gc.setImageSmoothing(true);
        gc.drawImage(strip(map), x + 14, y + 27, barW, 10);
        gc.setStroke(Color.web("#ffffff", 0.25));
        gc.setLineWidth(1);
        gc.strokeRect(x + 14.5, y + 27.5, barW - 1, 9);
        gc.setFont(SMALL);
        gc.setFill(MUTED);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(format(range.min()), x + 14, y + 41);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(format((range.min() + range.max()) / 2), x + 14 + barW / 2, y + 41);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText(format(range.max()), x + 14 + barW, y + 41);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    /** Text with a dark halo so it stays readable over any colour. */
    static void haloText(GraphicsContext gc, String s, double x, double y) {
        gc.setFill(Color.web("#000000", 0.55));
        gc.fillText(s, x + 1, y + 1);
        gc.setFill(TEXT);
        gc.fillText(s, x, y);
    }

    static String ellipsize(String s, Font font, double maxWidth) {
        Text t = new Text(s);
        t.setFont(font);
        if (t.getLayoutBounds().getWidth() <= maxWidth) {
            return s;
        }
        String cut = s;
        while (cut.length() > 4) {
            cut = cut.substring(0, cut.length() - 2);
            t.setText(cut + "…");
            if (t.getLayoutBounds().getWidth() <= maxWidth) {
                return cut + "…";
            }
        }
        return cut;
    }

    static String format(double v) {
        double a = Math.abs(v);
        if (a != 0 && (a < 0.01 || a >= 1e5)) {
            return String.format(Locale.ROOT, "%.2e", v);
        }
        String s = String.format(Locale.ROOT, a >= 100 ? "%.0f" : a >= 10 ? "%.1f" : "%.2f", v);
        return s.matches("-0(\\.0+)?") ? s.substring(1) : s;
    }

    /** Value with up to 4 significant digits, for read-outs. */
    static String value(double v) {
        if (Double.isNaN(v)) {
            return "—";
        }
        double a = Math.abs(v);
        if (a != 0 && (a < 1e-3 || a >= 1e6)) {
            return String.format(Locale.ROOT, "%.3e", v);
        }
        return String.format(Locale.ROOT, "%.4g", v).replaceAll("(\\.\\d*?)0+$", "$1").replaceAll("\\.$", "");
    }

    static String lon(double lon) {
        double l = ((lon + 180) % 360 + 360) % 360 - 180;
        return String.format(Locale.ROOT, "%.2f°%s", Math.abs(l), l < 0 ? "W" : "E");
    }

    static String lat(double lat) {
        return String.format(Locale.ROOT, "%.2f°%s", Math.abs(lat), lat < 0 ? "S" : "N");
    }

    static String bytes(long b) {
        if (b < 1 << 20) {
            return String.format(Locale.ROOT, "%.0f kB", Math.ceil(b / 1024.0));
        }
        if (b < 1L << 30) {
            return String.format(Locale.ROOT, "%.1f MB", b / (1024.0 * 1024));
        }
        return String.format(Locale.ROOT, "%.2f GB", b / (1024.0 * 1024 * 1024));
    }

    /** Fractional index of {@code v} in a monotonic array, or NaN when outside. */
    static double fractionalIndex(double[] values, double v) {
        int n = values.length;
        if (n < 2) {
            return Double.NaN;
        }
        boolean asc = values[n - 1] > values[0];
        double lo = asc ? values[0] : values[n - 1];
        double hi = asc ? values[n - 1] : values[0];
        if (v < lo || v > hi) {
            return Double.NaN;
        }
        int a = 0;
        int b = n - 1;
        while (b - a > 1) {
            int m = (a + b) >>> 1;
            if (asc == values[m] <= v) {
                a = m;
            } else {
                b = m;
            }
        }
        double d = values[b] - values[a];
        return d == 0 ? a : a + (v - values[a]) / d;
    }
}
