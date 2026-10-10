package io.github.laborima.gridviewer.render;

import java.util.Locale;

/**
 * Number, size and coordinate formatting shared by the viewer and the command line.
 */
public final class Formats {

    private Formats() {
    }

    /** Short tick label: 2 decimals below 10, 1 below 100, none above; scientific when tiny or huge. */
    public static String tick(double v) {
        double a = Math.abs(v);
        if (a != 0 && (a < 0.01 || a >= 1e5)) {
            return String.format(Locale.ROOT, "%.2e", v);
        }
        String s = String.format(Locale.ROOT, a >= 100 ? "%.0f" : a >= 10 ? "%.1f" : "%.2f", v);
        return s.matches("-0(\\.0+)?") ? s.substring(1) : s;
    }

    /** Value with up to 4 significant digits, for read-outs. */
    public static String value(double v) {
        if (Double.isNaN(v)) {
            return "—";
        }
        double a = Math.abs(v);
        if (a != 0 && (a < 1e-3 || a >= 1e6)) {
            return String.format(Locale.ROOT, "%.3e", v);
        }
        return String.format(Locale.ROOT, "%.4g", v).replaceAll("(\\.\\d*?)0+$", "$1").replaceAll("\\.$", "");
    }

    public static String bytes(long b) {
        if (b < 1 << 20) {
            return String.format(Locale.ROOT, "%.1f kB", b / 1024.0);
        }
        if (b < 1L << 30) {
            return String.format(Locale.ROOT, "%.1f MB", b / (1024.0 * 1024));
        }
        return String.format(Locale.ROOT, "%.2f GB", b / (1024.0 * 1024 * 1024));
    }

    public static String lon(double lon) {
        double l = ((lon + 180) % 360 + 360) % 360 - 180;
        if (Math.abs(Math.abs(l) - 180) < 1e-9 || Math.abs(l) < 1e-9) {
            return degrees(Math.abs(l)) + "°";
        }
        return degrees(Math.abs(l)) + "°" + (l < 0 ? "W" : "E");
    }

    public static String lat(double lat) {
        if (Math.abs(lat) < 1e-9) {
            return "0°";
        }
        return degrees(Math.abs(lat)) + "°" + (lat < 0 ? "S" : "N");
    }

    /** Up to two decimals, without trailing zeros. */
    private static String degrees(double v) {
        return String.format(Locale.ROOT, "%.2f", v).replaceAll("\\.?0+$", "");
    }
}
