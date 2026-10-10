package io.github.laborima.gridviewer.data;

/**
 * Values read for a {@link SliceRequest}, row-major with row 0 at {@code window.j0()}.
 * Missing values (land, fill values) are {@code NaN}.
 */
public record Slice(SliceRequest request, float[] values, long readNanos) {

    public int columns() {
        return request.columns();
    }

    public int rows() {
        return request.rows();
    }

    /** Value of the sample covering grid cell (i, j), or NaN when outside this slice. */
    public float valueAt(int i, int j) {
        Window w = request.window();
        if (!w.contains(i, j)) {
            return Float.NaN;
        }
        int c = (i - w.i0()) / request.stride();
        int r = (j - w.j0()) / request.stride();
        return values[r * columns() + c];
    }

    public long bytes() {
        return (long) values.length * Float.BYTES;
    }
}
