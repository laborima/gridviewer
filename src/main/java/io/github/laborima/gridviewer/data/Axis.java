package io.github.laborima.gridviewer.data;

import java.util.Locale;

/**
 * One dimension of a variable, with its coordinate values when the file provides them.
 *
 * @param name   dimension name
 * @param length number of points
 * @param units  coordinate units, empty when unknown
 * @param values coordinate values, or {@code null} when the dimension has no 1D coordinate
 * @param labels preformatted labels (decoded dates for time axes), or {@code null}
 */
public record Axis(String name, int length, String units, double[] values, String[] labels) {

    public static Axis indexOnly(String name, int length) {
        return new Axis(name, length, "", null, null);
    }

    public boolean hasValues() {
        return values != null;
    }

    public double value(int k) {
        return values == null ? k : values[k];
    }

    /** Human-readable coordinate of point {@code k}, falling back to the index. */
    public String label(int k) {
        if (labels != null) {
            return labels[k];
        }
        if (values == null) {
            return name + " " + k;
        }
        return format(values[k]) + (units.isEmpty() ? "" : " " + shortUnits());
    }

    public boolean isLongitude() {
        return Coordinates.isLongitude(name, units);
    }

    public boolean isLatitude() {
        return Coordinates.isLatitude(name, units);
    }

    /** True when coordinate values decrease along the axis (north-up files, for instance). */
    public boolean isDescending() {
        return values != null && values.length > 1 && values[values.length - 1] < values[0];
    }

    private String shortUnits() {
        return switch (units.toLowerCase(Locale.ROOT)) {
            case "degrees_east", "degree_east", "degreese" -> "°E";
            case "degrees_north", "degree_north", "degreesn" -> "°N";
            case "meters", "meter", "metres", "metre" -> "m";
            default -> units;
        };
    }

    static String format(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e7) {
            return String.valueOf((long) v);
        }
        double a = Math.abs(v);
        if (a >= 0.01 && a < 1e5) {
            return String.format(Locale.ROOT, "%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return String.format(Locale.ROOT, "%.3g", v);
    }
}
