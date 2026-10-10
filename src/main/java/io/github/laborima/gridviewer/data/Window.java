package io.github.laborima.gridviewer.data;

/**
 * Inclusive rectangle of grid indices: columns {@code i0..i1}, rows {@code j0..j1}.
 */
public record Window(int i0, int j0, int i1, int j1) {

    public Window {
        if (i1 < i0 || j1 < j0) {
            throw new IllegalArgumentException("Empty window " + i0 + ".." + i1 + " x " + j0 + ".." + j1);
        }
    }

    public static Window full(int nx, int ny) {
        return new Window(0, 0, nx - 1, ny - 1);
    }

    public int width() {
        return i1 - i0 + 1;
    }

    public int height() {
        return j1 - j0 + 1;
    }

    public long cells() {
        return (long) width() * height();
    }

    public boolean contains(int i, int j) {
        return i >= i0 && i <= i1 && j >= j0 && j <= j1;
    }

    /** Clamps this window to a grid of {@code nx} by {@code ny} cells. */
    public Window clamp(int nx, int ny) {
        int a = Math.clamp(i0, 0, nx - 1);
        int b = Math.clamp(j0, 0, ny - 1);
        return new Window(a, b, Math.clamp(i1, a, nx - 1), Math.clamp(j1, b, ny - 1));
    }

    /**
     * Snaps the window origin down to a multiple of {@code stride}, so that panning reuses
     * the same sample positions and the picture does not shimmer.
     */
    public Window alignTo(int stride) {
        return new Window(i0 - Math.floorMod(i0, stride), j0 - Math.floorMod(j0, stride), i1, j1);
    }

    @Override
    public String toString() {
        return "[" + i0 + ".." + i1 + "] x [" + j0 + ".." + j1 + "]";
    }
}
