package io.github.laborima.gridviewer.cli;

import java.io.PrintStream;

import io.github.laborima.gridviewer.data.Axis;
import io.github.laborima.gridviewer.data.GridDataset;
import io.github.laborima.gridviewer.data.GridVariable;
import io.github.laborima.gridviewer.render.ColorMap;
import io.github.laborima.gridviewer.render.ColorMaps;
import io.github.laborima.gridviewer.render.Formats;

/**
 * {@code gridviewer info} and {@code gridviewer palettes}.
 */
public final class InfoCommand {

    private InfoCommand() {
    }

    public static int info(String[] argv, PrintStream out) throws Exception {
        if (argv.length == 0) {
            out.println("Usage: gridviewer info FILE|URL");
            return 2;
        }
        try (GridDataset ds = GridDataset.open(argv[0])) {
            out.println(ds.location());
            long size = ds.fileSize();
            out.println("  " + ds.fileType() + (size >= 0 ? ", " + Formats.bytes(size) + " on disk" : ""));
            out.println();
            if (ds.variables().isEmpty()) {
                out.println("  No gridded variable (rank >= 2) in this file.");
            }
            for (GridVariable v : ds.variables()) {
                out.printf("  %-14s %s%n", v.name(), v.longName() + (v.units().isEmpty() ? "" : " [" + v.units() + "]"));
                out.printf("  %-14s %s, %s as floats%n", "", v.dimensionsString(), Formats.bytes(v.fullSizeBytes()));
                describeAxis(out, "levels", v.levelAxis());
                describeAxis(out, "times", v.timeAxis());
                out.printf("  %-14s %s%n", "", v.isRegularLonLat() ? "regular lon/lat grid"
                        : v.hasCurvilinearCoordinates() ? "curvilinear grid (2D lon/lat)" : "index grid");
            }
        }
        return 0;
    }

    public static int palettes(PrintStream out) {
        for (ColorMap m : ColorMaps.all()) {
            out.printf("  %-22s %s%n", m.name(), m.source());
        }
        return 0;
    }

    private static void describeAxis(PrintStream out, String what, Axis axis) {
        if (axis == null || axis.length() < 2) {
            return;
        }
        out.printf("  %-14s %d %s: %s .. %s%n", "", axis.length(), what, axis.label(0), axis.label(axis.length() - 1));
    }
}
