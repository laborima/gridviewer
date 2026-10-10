package io.github.laborima.gridviewer.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Overlay files inherited from the 2006 Grid Viewer: lists of grid points and NEMO MPI
 * domain decompositions.
 */
public final class Overlays {

    /** A grid point to mark on the map, with an optional label. */
    public record Point(int i, int j, String label) {
    }

    /** The sub-domain of one MPI process, as an inclusive window of global grid indices. */
    public record Domain(int rank, Window window) {
    }

    private Overlays() {
    }

    /**
     * Reads a point list: one {@code i j [label]} per line, 0-based indices.
     * Blank lines and lines starting with {@code #} are skipped.
     */
    public static List<Point> readPoints(Path file) throws IOException {
        List<Point> points = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            String s = line.strip();
            if (s.isEmpty() || s.startsWith("#")) {
                continue;
            }
            String[] t = s.split("[\\s,;]+", 3);
            if (t.length < 2 || !isInteger(t[0]) || !isInteger(t[1])) {
                continue;
            }
            points.add(new Point(Integer.parseInt(t[0]), Integer.parseInt(t[1]), t.length > 2 ? t[2] : ""));
        }
        return points;
    }

    /**
     * Reads a NEMO {@code layout.dat}. Each data line holds
     * {@code jn nlci nlcj nldi nldj nlei nlej nimpp njmpp ...} with 1-based Fortran offsets;
     * header lines are skipped.
     */
    public static List<Domain> readLayout(Path file) throws IOException {
        List<Domain> domains = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            String[] t = line.strip().split("\\s+");
            if (t.length < 9 || !allIntegers(t, 9)) {
                continue;
            }
            int rank = Integer.parseInt(t[0]);
            int nlci = Integer.parseInt(t[1]);
            int nlcj = Integer.parseInt(t[2]);
            int nimpp = Integer.parseInt(t[7]);
            int njmpp = Integer.parseInt(t[8]);
            if (nlci < 1 || nlcj < 1) {
                continue;
            }
            domains.add(new Domain(rank, new Window(nimpp - 1, njmpp - 1, nimpp + nlci - 2, njmpp + nlcj - 2)));
        }
        return domains;
    }

    private static boolean allIntegers(String[] t, int n) {
        for (int k = 0; k < n; k++) {
            if (!isInteger(t[k])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isInteger(String s) {
        return s.matches("-?\\d+");
    }
}
