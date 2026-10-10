package io.github.laborima.gridviewer.data;

/**
 * A horizontal slice of a variable at one time step and one level, restricted to a window and
 * subsampled by a stride.
 */
public record SliceRequest(String variable, int time, int level, Window window, int stride) {

    public SliceRequest {
        if (stride < 1) {
            throw new IllegalArgumentException("stride must be >= 1");
        }
    }

    public int columns() {
        return Stride.samples(window.width(), stride);
    }

    public int rows() {
        return Stride.samples(window.height(), stride);
    }

    public long samples() {
        return (long) columns() * rows();
    }
}
