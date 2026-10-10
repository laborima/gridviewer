package io.github.laborima.gridviewer.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;

/**
 * An open NetCDF file (or OPeNDAP URL). Nothing but metadata is read when opening: values are
 * only read on request, one strided window at a time, on a single I/O thread because
 * netCDF-Java files are not thread safe.
 */
public final class GridDataset implements AutoCloseable {

    private final String location;
    private final NetcdfDataset dataset;
    private final List<GridVariable> variables;
    private final ExecutorService io;
    private final SliceCache cache;

    private GridDataset(String location, NetcdfDataset dataset) {
        this.location = location;
        this.dataset = dataset;
        this.io = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "netcdf-io");
            t.setDaemon(true);
            return t;
        });
        this.cache = new SliceCache(Math.max(64L << 20, Runtime.getRuntime().maxMemory() / 4));
        List<GridVariable> list = new ArrayList<>();
        for (Variable v : dataset.getVariables()) {
            if (GridVariable.isGridded(v)) {
                list.add(new GridVariable(dataset, v));
            }
        }
        // Most interesting first: 4D before 3D before 2D, coordinates last.
        list.sort(Comparator.comparingInt((GridVariable g) -> isCoordinateLike(g) ? 1 : 0)
                .thenComparing(g -> -g.shape().length));
        this.variables = List.copyOf(list);
    }

    /** Opens a local path or a remote URL. Only the header is read. */
    public static GridDataset open(String location) throws IOException {
        return new GridDataset(location, NetcdfDatasets.openDataset(location));
    }

    public String location() {
        return location;
    }

    /** Short display name: file name for local paths, last URL segment otherwise. */
    public String displayName() {
        String s = location.replaceAll("[/\\\\]+$", "");
        return s.substring(Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\')) + 1);
    }

    /** Size on disk for local files, -1 for remote datasets. */
    public long fileSize() {
        try {
            Path p = Path.of(location);
            return Files.isRegularFile(p) ? Files.size(p) : -1;
        } catch (RuntimeException | IOException e) {
            return -1;
        }
    }

    public String fileType() {
        String t = dataset.getFileTypeDescription();
        return t == null ? "" : t;
    }

    public List<GridVariable> variables() {
        return variables;
    }

    public GridVariable variable(String name) {
        for (GridVariable v : variables) {
            if (v.name().equals(name)) {
                return v;
            }
        }
        return null;
    }

    /** Reads a slice through the cache. Must run on the I/O thread (see {@link #submit}). */
    public Slice readSlice(GridVariable variable, SliceRequest request) throws IOException {
        Slice cached = cache.get(request);
        if (cached != null) {
            return cached;
        }
        Slice slice = variable.readSlice(request);
        cache.put(slice);
        return slice;
    }

    /** Runs blocking netCDF work on the dataset I/O thread. */
    public <T> CompletableFuture<T> submit(Callable<T> work) {
        CompletableFuture<T> f = new CompletableFuture<>();
        io.execute(() -> {
            try {
                f.complete(work.call());
            } catch (Throwable e) {
                f.completeExceptionally(e);
            }
        });
        return f;
    }

    ExecutorService executor() {
        return io;
    }

    @Override
    public void close() {
        io.execute(() -> {
            try {
                dataset.close();
            } catch (IOException ignored) {
                // nothing useful to do when a read-only file fails to close
            }
        });
        io.shutdown();
        cache.clear();
    }

    private static boolean isCoordinateLike(GridVariable g) {
        String n = g.name().toLowerCase(java.util.Locale.ROOT);
        return Coordinates.isLongitude(n, g.units()) || Coordinates.isLatitude(n, g.units())
                || n.endsWith("_bnds") || n.endsWith("_bounds") || n.contains("mask");
    }
}
