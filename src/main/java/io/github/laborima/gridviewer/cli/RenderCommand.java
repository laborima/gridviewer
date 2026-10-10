package io.github.laborima.gridviewer.cli;

import java.awt.image.BufferedImage;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

import io.github.laborima.gridviewer.data.GridDataset;
import io.github.laborima.gridviewer.data.GridVariable;
import io.github.laborima.gridviewer.data.Slice;
import io.github.laborima.gridviewer.data.SliceRequest;
import io.github.laborima.gridviewer.data.Stride;
import io.github.laborima.gridviewer.data.Window;
import io.github.laborima.gridviewer.render.ColorMap;
import io.github.laborima.gridviewer.render.ColorMaps;
import io.github.laborima.gridviewer.render.Formats;
import io.github.laborima.gridviewer.render.StaticMap;
import io.github.laborima.gridviewer.render.ValueRange;

/**
 * {@code gridviewer render}: draws one level of a variable to a PNG without opening a window,
 * reading only the points the image needs.
 */
public final class RenderCommand {

    static final String USAGE = """
            Usage: gridviewer render FILE [options] -o OUT.png

              -v, --var NAME        variable (default: the first gridded variable)
              -t, --time N          time index (default 0; -1 is the latest)
              -z, --level N         level index (default 0, usually the surface; -1 is the deepest)
              -W, --width PX        image width in pixels (default 1600)
                  --window I0,J0,I1,J1  grid index window to draw (default: whole grid)
              -p, --palette NAME    colour map (see 'gridviewer palettes')
              -r, --range MIN:MAX   colour range (default: automatic)
                  --mask-zeros      draw exact zeros as land (NEMO files without fill values)
                  --full            read every point, ignoring the screen resolution (for comparison)
                  --no-legend       map only, no title and colour bar
              -o, --output FILE     PNG file to write
            """;

    private RenderCommand() {
    }

    public static int run(String[] argv, PrintStream out) throws Exception {
        Args a = new Args(argv, Set.of("mask-zeros", "full", "no-legend", "help"),
                Map.of("v", "var", "t", "time", "z", "level", "W", "width", "p", "palette", "r", "range",
                        "o", "output", "h", "help"));
        if (a.has("help") || a.positionals().isEmpty() || !a.has("output")) {
            out.print(USAGE);
            return a.has("help") ? 0 : 2;
        }
        try (GridDataset ds = GridDataset.open(a.positionals().getFirst())) {
            GridVariable v = a.has("var") ? ds.variable(a.get("var", null)) : ds.variables().stream().findFirst().orElse(null);
            if (v == null) {
                out.println("No such gridded variable. Try: gridviewer info " + a.positionals().getFirst());
                return 1;
            }
            // Negative indices count from the end: -t -1 is the latest time step.
            int t = Math.floorMod(a.getInt("time", 0), v.times());
            int z = Math.floorMod(a.getInt("level", 0), v.levels());
            Window window = a.has("window") ? parseWindow(a.get("window", "")) : Window.full(v.nx(), v.ny());
            window = window.clamp(v.nx(), v.ny());
            int width = a.getInt("width", 1600);
            int mapHeight = (int) Math.round(width * (double) window.height() / window.width());
            int stride = a.has("full") ? 1 : Stride.forWindow(window, width, mapHeight);

            SliceRequest request = new SliceRequest(v.name(), t, z, window.alignTo(stride), stride);
            Slice slice = ds.submit(() -> ds.readSlice(v, request)).get();

            ColorMap map = a.has("palette") ? ColorMaps.get(a.get("palette", null))
                    : ColorMaps.suggest(v.name(), v.longName(), v.units());
            ValueRange range = parseRange(a.get("range", null), v, slice, map);
            boolean flip = !v.yAxis().isDescending();
            String subtitle = describe(v, t, z) + "  ·  " + ds.displayName();
            BufferedImage img = StaticMap.render(slice, v, subtitle, map, range, flip, a.has("mask-zeros"),
                    width, mapHeight, !a.has("no-legend"));
            Path output = Path.of(a.get("output", "out.png"));
            ImageIO.write(img, "png", output.toFile());

            long full = window.cells() * Float.BYTES;
            out.printf(Locale.ROOT, "%s: read %,d of %,d points (stride %d), %s instead of %s, in %d ms -> %s%n",
                    v.name(), slice.values().length, window.cells(), stride, Formats.bytes(slice.bytes()), Formats.bytes(full),
                    slice.readNanos() / 1_000_000, output);
            return 0;
        }
    }

    static String describe(GridVariable v, int t, int z) {
        StringBuilder sb = new StringBuilder();
        if (v.timeAxis() != null) {
            sb.append(v.timeAxis().label(t));
        }
        if (v.levelAxis() != null) {
            sb.append(sb.isEmpty() ? "" : "  ·  ").append(v.levelAxis().label(z));
        }
        return sb.isEmpty() ? v.name() : sb.toString();
    }

    private static Window parseWindow(String s) {
        String[] p = s.split(",");
        if (p.length != 4) {
            throw new IllegalArgumentException("--window expects I0,J0,I1,J1");
        }
        return new Window(Integer.parseInt(p[0].strip()), Integer.parseInt(p[1].strip()),
                Integer.parseInt(p[2].strip()), Integer.parseInt(p[3].strip()));
    }

    private static ValueRange parseRange(String s, GridVariable v, Slice slice, ColorMap map) {
        if (s != null) {
            String[] p = s.split(":");
            return new ValueRange(Double.parseDouble(p[0]), Double.parseDouble(p[1]));
        }
        double[] d = ColorMaps.defaultRange(v.name());
        if (d != null) {
            return new ValueRange(d[0], d[1]);
        }
        ValueRange auto = ValueRange.auto(slice.values());
        return ColorMaps.isDiverging(map) ? auto.symmetric() : auto;
    }
}
