package io.github.laborima.gridviewer.data;

/**
 * The trick that made Grid Viewer usable on files bigger than memory: never read more grid
 * points than there are pixels to show them. When a window of {@code cells} grid points is
 * displayed on {@code pixels} screen pixels, only every {@code stride}-th point is read.
 * Zooming in shrinks the window, the stride drops, and the detail is read on demand.
 */
public final class Stride {

    private Stride() {
    }

    /**
     * Largest stride that still gives at least one sample per screen pixel along an axis.
     *
     * @param cells  number of grid cells visible along the axis
     * @param pixels number of device pixels available along the axis
     */
    public static int forAxis(int cells, double pixels) {
        if (pixels <= 0) {
            return Math.max(1, cells);
        }
        return Math.max(1, (int) Math.floor(cells / pixels));
    }

    /**
     * One stride for both axes, so grid cells stay square on screen.
     */
    public static int forWindow(Window window, double widthPx, double heightPx) {
        return Math.min(forAxis(window.width(), widthPx), forAxis(window.height(), heightPx));
    }

    /** Number of samples a strided read of {@code length} cells returns. */
    public static int samples(int length, int stride) {
        return (length + stride - 1) / stride;
    }
}
