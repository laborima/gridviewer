package io.github.laborima.gridviewer.render;

/**
 * Turns grid values into ARGB pixels.
 */
public final class Rasterizer {

    /** Colour of land and missing values. */
    public static final int LAND = 0xFF2B2F36;

    private Rasterizer() {
    }

    /**
     * Colours a row-major grid of {@code columns} values per row.
     *
     * @param flipRows put the last grid row at the top of the image (north up for south-to-north grids)
     * @param maskZeros treat exact zeros as land, as NEMO outputs without fill values do
     */
    public static int[] colorize(float[] values, int columns, ColorMap map, ValueRange range,
                                 boolean flipRows, boolean maskZeros) {
        int rows = values.length / columns;
        int[] argb = new int[values.length];
        int[] lut = map.argb();
        double scale = (ColorMap.SIZE - 1) / (range.max() - range.min());
        double min = range.min();
        for (int r = 0; r < rows; r++) {
            int src = r * columns;
            int dst = (flipRows ? rows - 1 - r : r) * columns;
            for (int c = 0; c < columns; c++) {
                float v = values[src + c];
                if (Float.isNaN(v) || maskZeros && v == 0f) {
                    argb[dst + c] = LAND;
                } else {
                    int k = (int) ((v - min) * scale + 0.5);
                    argb[dst + c] = lut[k < 0 ? 0 : k > 255 ? 255 : k];
                }
            }
        }
        return argb;
    }
}
