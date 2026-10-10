package io.github.laborima.gridviewer.render;

import java.util.Arrays;

/**
 * The data range mapped onto a colour map.
 */
public record ValueRange(double min, double max) {

    public ValueRange {
        if (!(max > min)) {
            double c = Double.isFinite(min) ? min : 0;
            min = c - 0.5;
            max = c + 0.5;
        }
    }

    /** Position of {@code v} in the range, 0 at {@code min} and 1 at {@code max}. */
    public double normalise(double v) {
        return (v - min) / (max - min);
    }

    /**
     * Robust automatic range: the 1st and 99th percentiles of the finite values, so a few
     * outliers do not wash out the picture. Large arrays are sampled.
     */
    public static ValueRange auto(float[] values) {
        int step = Math.max(1, values.length / 200_000);
        float[] sample = new float[(values.length + step - 1) / step];
        int n = 0;
        for (int k = 0; k < values.length; k += step) {
            if (Float.isFinite(values[k])) {
                sample[n++] = values[k];
            }
        }
        if (n == 0) {
            return new ValueRange(0, 1);
        }
        Arrays.sort(sample, 0, n);
        double lo = sample[(int) Math.floor(0.01 * (n - 1))];
        double hi = sample[(int) Math.ceil(0.99 * (n - 1))];
        if (!(hi > lo)) {
            lo = sample[0];
            hi = sample[n - 1];
        }
        return new ValueRange(lo, hi);
    }

    /** Same width, centred on zero: for anomalies and velocities. */
    public ValueRange symmetric() {
        double m = Math.max(Math.abs(min), Math.abs(max));
        return new ValueRange(-m, m);
    }
}
