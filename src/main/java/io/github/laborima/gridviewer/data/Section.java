package io.github.laborima.gridviewer.data;

/**
 * Values of a {@link SectionRequest}: {@code levels} rows (row 0 is the first level, usually
 * the surface) by {@code request.columns()} columns. Missing values are {@code NaN}.
 */
public record Section(SectionRequest request, int levels, float[] values, long readNanos) {

    public int columns() {
        return request.columns();
    }

    public float valueAt(int level, int column) {
        return values[level * columns() + column];
    }
}
