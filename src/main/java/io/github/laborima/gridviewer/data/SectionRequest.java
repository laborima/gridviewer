package io.github.laborima.gridviewer.data;

/**
 * A vertical section through all levels, along one grid row (fixed {@code j}) or one grid
 * column (fixed {@code i}), between {@code from} and {@code to} and subsampled by a stride.
 */
public record SectionRequest(String variable, int time, Orientation orientation, int fixed,
                             int from, int to, int stride) {

    public enum Orientation {
        /** Fixed row j: the section runs west-east along i. */
        ALONG_ROW,
        /** Fixed column i: the section runs south-north along j. */
        ALONG_COLUMN
    }

    public SectionRequest {
        if (stride < 1 || to < from) {
            throw new IllegalArgumentException("bad section " + from + ".." + to + " / " + stride);
        }
    }

    public int columns() {
        return Stride.samples(to - from + 1, stride);
    }
}
