package io.github.laborima.gridviewer.render;

/**
 * A 256-entry colour lookup table, stored as opaque ARGB integers.
 */
public record ColorMap(String name, String source, int[] argb) {

    public static final int SIZE = 256;

    public ColorMap {
        if (argb.length != SIZE) {
            throw new IllegalArgumentException(name + ": expected " + SIZE + " colours, got " + argb.length);
        }
    }

    /** Builds a table from any number of RGB stops (0-255), linearly resampled to 256 entries. */
    public static ColorMap fromStops(String name, String source, int[][] rgb) {
        int[] lut = new int[SIZE];
        int n = rgb.length;
        for (int k = 0; k < SIZE; k++) {
            double pos = n == 1 ? 0 : k * (n - 1) / (double) (SIZE - 1);
            int a = (int) Math.floor(pos);
            int b = Math.min(n - 1, a + 1);
            double f = pos - a;
            int r = (int) Math.round(rgb[a][0] + f * (rgb[b][0] - rgb[a][0]));
            int g = (int) Math.round(rgb[a][1] + f * (rgb[b][1] - rgb[a][1]));
            int bl = (int) Math.round(rgb[a][2] + f * (rgb[b][2] - rgb[a][2]));
            lut[k] = 0xFF000000 | r << 16 | g << 8 | bl;
        }
        return new ColorMap(name, source, lut);
    }

    /** Colour for a value already normalised to [0, 1]; values outside are clamped. */
    public int color(double t) {
        int k = (int) (t * (SIZE - 1) + 0.5);
        return argb[Math.clamp(k, 0, SIZE - 1)];
    }

    public ColorMap reversed() {
        int[] r = new int[SIZE];
        for (int k = 0; k < SIZE; k++) {
            r[k] = argb[SIZE - 1 - k];
        }
        return new ColorMap(name.endsWith(" (reversed)") ? name : name + " (reversed)", source, r);
    }

    @Override
    public String toString() {
        return name;
    }
}
