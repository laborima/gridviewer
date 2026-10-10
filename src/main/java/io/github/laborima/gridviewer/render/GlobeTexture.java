package io.github.laborima.gridviewer.render;

import io.github.laborima.gridviewer.data.Axis;
import io.github.laborima.gridviewer.data.Window;

/**
 * Builds equirectangular textures (longitude -180..180 left to right, latitude 90..-90 top to
 * bottom) from coloured grid samples.
 */
public final class GlobeTexture {

    /** Colour of the parts of the globe the grid does not cover. */
    public static final int EMPTY = 0xFF1A1E24;

    private GlobeTexture() {
    }

    /**
     * Exact inverse mapping for grids with 1D longitude and latitude.
     *
     * @param colors   one colour per sample, row-major, row 0 at {@code window.j0()}
     * @param window   grid window of the samples
     * @param stride   stride of the samples
     */
    public static int[] regular(int[] colors, Window window, int stride, Axis lon, Axis lat, int width, int height) {
        int cols = (window.width() + stride - 1) / stride;
        int rows = (window.height() + stride - 1) / stride;
        int[] colOf = new int[width];
        for (int u = 0; u < width; u++) {
            double l = -180 + (u + 0.5) * 360.0 / width;
            colOf[u] = sampleIndex(lon.values(), wrapInto(lon.values(), l), window.i0(), stride, cols);
        }
        int[] rowOf = new int[height];
        for (int v = 0; v < height; v++) {
            double l = 90 - (v + 0.5) * 180.0 / height;
            rowOf[v] = sampleIndex(lat.values(), l, window.j0(), stride, rows);
        }
        int[] tex = new int[width * height];
        for (int v = 0; v < height; v++) {
            int r = rowOf[v];
            for (int u = 0; u < width; u++) {
                int c = colOf[u];
                tex[v * width + u] = r < 0 || c < 0 ? EMPTY : colors[r * cols + c];
            }
        }
        return tex;
    }

    /**
     * Forward splatting for curvilinear grids (2D longitude and latitude), followed by a few
     * passes of hole filling.
     */
    public static int[] scattered(int[] colors, float[] lon, float[] lat, int columns, int width, int height) {
        int[] tex = new int[width * height];
        boolean[] set = new boolean[width * height];
        // Block size from the typical spacing between neighbours along a row.
        double spacing = typicalSpacing(lon, lat, columns);
        int block = Math.max(1, (int) Math.ceil(spacing * width / 360.0));
        int half = block / 2;
        for (int k = 0; k < colors.length; k++) {
            float lo = lon[k];
            float la = lat[k];
            if (!Float.isFinite(lo) || !Float.isFinite(la)) {
                continue;
            }
            int u = (int) Math.floor((((lo + 180) % 360 + 360) % 360) / 360.0 * width);
            int v = (int) Math.floor((90 - la) / 180.0 * height);
            for (int dv = -half; dv <= half; dv++) {
                int vv = v + dv;
                if (vv < 0 || vv >= height) {
                    continue;
                }
                for (int du = -half; du <= half; du++) {
                    int uu = Math.floorMod(u + du, width);
                    tex[vv * width + uu] = colors[k];
                    set[vv * width + uu] = true;
                }
            }
        }
        fillHoles(tex, set, width, height, 3);
        for (int p = 0; p < tex.length; p++) {
            if (!set[p]) {
                tex[p] = EMPTY;
            }
        }
        return tex;
    }

    private static double typicalSpacing(float[] lon, float[] lat, int columns) {
        double sum = 0;
        int n = 0;
        for (int k = 1; k < lon.length && n < 5000; k += Math.max(1, lon.length / 5000)) {
            if (k % columns == 0) {
                continue;
            }
            double dl = Math.abs(lon[k] - lon[k - 1]);
            if (dl > 180) {
                dl = 360 - dl;
            }
            double d = Math.hypot(dl * Math.cos(Math.toRadians(lat[k])), lat[k] - lat[k - 1]);
            if (Double.isFinite(d) && d > 0) {
                sum += d;
                n++;
            }
        }
        return n == 0 ? 1 : 1.5 * sum / n;
    }

    private static void fillHoles(int[] tex, boolean[] set, int w, int h, int passes) {
        for (int pass = 0; pass < passes; pass++) {
            boolean[] next = set.clone();
            for (int v = 1; v < h - 1; v++) {
                for (int u = 0; u < w; u++) {
                    int p = v * w + u;
                    if (set[p]) {
                        continue;
                    }
                    int[] nb = {p - w, p + w, v * w + Math.floorMod(u - 1, w), v * w + (u + 1) % w};
                    for (int q : nb) {
                        if (set[q]) {
                            tex[p] = tex[q];
                            next[p] = true;
                            break;
                        }
                    }
                }
            }
            System.arraycopy(next, 0, set, 0, set.length);
        }
    }

    private static double wrapInto(double[] values, double lon) {
        double lo = Math.min(values[0], values[values.length - 1]);
        double l = lon;
        while (l < lo) {
            l += 360;
        }
        while (l - 360 >= lo) {
            l -= 360;
        }
        return l;
    }

    /** Sample index for a coordinate, or -1 when the coordinate is outside the window. */
    private static int sampleIndex(double[] axis, double value, int start, int stride, int samples) {
        int n = axis.length;
        boolean asc = axis[n - 1] > axis[0];
        double half = Math.abs(axis[n - 1] - axis[0]) / (n - 1) / 2;
        double lo = (asc ? axis[0] : axis[n - 1]) - half;
        double hi = (asc ? axis[n - 1] : axis[0]) + half;
        if (value < lo || value > hi) {
            return -1;
        }
        int a = 0;
        int b = n - 1;
        while (b - a > 1) {
            int m = (a + b) >>> 1;
            if (asc == axis[m] <= value) {
                a = m;
            } else {
                b = m;
            }
        }
        int idx = Math.abs(axis[a] - value) <= Math.abs(axis[b] - value) ? a : b;
        int s = (idx - start) / stride;
        return idx < start || s >= samples ? -1 : s;
    }
}
