package io.github.laborima.gridviewer.data;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.time.CalendarDateUnit;

/**
 * A variable with at least two horizontal dimensions, seen as a stack of 2D grids.
 *
 * <p>The last two dimensions are the horizontal grid (y, x). Among the leading ones, a time-like
 * dimension becomes the time axis and the dimension closest to the grid becomes the level axis;
 * any other leading dimension is held at index 0.
 *
 * <p>Read methods do blocking I/O and must be called from the dataset I/O thread.
 */
public final class GridVariable {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter MINUTE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    private final Variable variable;
    private final int rank;
    private final int xDim;
    private final int yDim;
    private final int zDim;
    private final int tDim;
    private final Axis x;
    private final Axis y;
    private final Axis z;
    private final Axis t;
    private final Variable lon2d;
    private final Variable lat2d;
    private final double validMin;
    private final double validMax;

    GridVariable(NetcdfFile file, Variable variable) {
        this.variable = variable;
        this.rank = variable.getRank();
        List<Dimension> dims = variable.getDimensions();
        xDim = rank - 1;
        yDim = rank - 2;
        int time = -1;
        int level = -1;
        for (int d = rank - 3; d >= 0; d--) {
            Variable coord = coordinateOf(file, dims.get(d));
            String units = coord == null ? "" : unitsOf(coord);
            if (time < 0 && Coordinates.isTime(dims.get(d).getShortName(), units)) {
                time = d;
            } else if (level < 0) {
                level = d;
            }
        }
        tDim = time;
        zDim = level;
        x = axis(file, dims.get(xDim));
        y = axis(file, dims.get(yDim));
        z = zDim < 0 ? null : axis(file, dims.get(zDim));
        t = tDim < 0 ? null : axis(file, dims.get(tDim));

        Variable[] lonLat = findCurvilinearCoordinates(file, variable, dims.get(yDim), dims.get(xDim));
        lon2d = x.isLongitude() && x.hasValues() ? null : lonLat[0];
        lat2d = y.isLatitude() && y.hasValues() ? null : lonLat[1];

        double[] range = validRange(variable);
        validMin = range[0];
        validMax = range[1];
    }

    /** True when {@code v} can be shown as a map: numeric, rank >= 2 and a real 2D grid. */
    static boolean isGridded(Variable v) {
        if (v.getRank() < 2 || !v.getDataType().isNumeric()) {
            return false;
        }
        int[] shape = v.getShape();
        return shape[shape.length - 1] >= 3 && shape[shape.length - 2] >= 3;
    }

    public String name() {
        return variable.getFullName();
    }

    public String longName() {
        String s = variable.findAttributeString("long_name", null);
        if (s == null) {
            s = variable.findAttributeString("standard_name", null);
        }
        return s == null ? name() : s;
    }

    /** Units, or an empty string when missing or dimensionless ("1"). */
    public String units() {
        String u = unitsOf(variable);
        return u.equals("1") ? "" : u;
    }

    public int[] shape() {
        return variable.getShape();
    }

    public String dimensionsString() {
        StringBuilder sb = new StringBuilder();
        for (Dimension d : variable.getDimensions()) {
            sb.append(sb.isEmpty() ? "" : " × ").append(d.getShortName()).append('=').append(d.getLength());
        }
        return sb.toString();
    }

    /** Bytes the whole variable would take in memory as floats. */
    public long fullSizeBytes() {
        return variable.getSize() * Float.BYTES;
    }

    public int nx() {
        return x.length();
    }

    public int ny() {
        return y.length();
    }

    public int levels() {
        return z == null ? 1 : z.length();
    }

    public int times() {
        return t == null ? 1 : t.length();
    }

    public Axis xAxis() {
        return x;
    }

    public Axis yAxis() {
        return y;
    }

    public Axis levelAxis() {
        return z;
    }

    public Axis timeAxis() {
        return t;
    }

    public boolean hasCurvilinearCoordinates() {
        return lon2d != null && lat2d != null;
    }

    /** True when the grid has a 1D longitude and a 1D latitude coordinate. */
    public boolean isRegularLonLat() {
        return x.isLongitude() && x.hasValues() && y.isLatitude() && y.hasValues();
    }

    /** Valid range declared by the file ({@code valid_min/max}, {@code valid_range}), or NaN. */
    public double validMin() {
        return validMin;
    }

    public double validMax() {
        return validMax;
    }

    /**
     * True when the file explicitly marks missing values; otherwise land is often stored as 0
     * (NEMO). The implicit NetCDF-3 default fill value does not count.
     */
    public boolean declaresMissingValues() {
        return variable.findAttribute("_FillValue") != null || variable.findAttribute("missing_value") != null
                || variable.findAttribute("valid_range") != null || variable.findAttribute("valid_min") != null
                || variable.findAttribute("valid_max") != null;
    }

    public String attributesString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name()).append("  (").append(dimensionsString()).append(")\n");
        sb.append("type: ").append(variable.getDataType()).append('\n');
        for (Attribute a : variable.attributes()) {
            sb.append(a.getShortName()).append(" = ");
            if (a.isString()) {
                sb.append(a.getStringValue());
            } else {
                for (int k = 0; k < a.getLength(); k++) {
                    sb.append(k == 0 ? "" : ", ").append(a.getNumericValue(k));
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /** Reads a strided horizontal slice. Blocking I/O. */
    public Slice readSlice(SliceRequest request) throws IOException {
        Window w = request.window();
        int s = request.stride();
        List<Range> ranges = baseRanges(request.time());
        try {
            if (zDim >= 0) {
                ranges.set(zDim, new Range(request.level(), request.level()));
            }
            ranges.set(yDim, new Range(w.j0(), w.j1(), s));
            ranges.set(xDim, new Range(w.i0(), w.i1(), s));
            long start = System.nanoTime();
            float[] values = toFloats(variable.read(ranges));
            return new Slice(request, values, System.nanoTime() - start);
        } catch (InvalidRangeException e) {
            throw new IOException("Invalid slice " + request + ": " + e.getMessage(), e);
        }
    }

    /** Reads a strided vertical section through every level. Blocking I/O. */
    public Section readSection(SectionRequest request) throws IOException {
        if (zDim < 0) {
            throw new IOException(name() + " has no vertical dimension");
        }
        List<Range> ranges = baseRanges(request.time());
        try {
            ranges.set(zDim, new Range(0, levels() - 1));
            Range along = new Range(request.from(), request.to(), request.stride());
            Range fixed = new Range(request.fixed(), request.fixed());
            boolean alongRow = request.orientation() == SectionRequest.Orientation.ALONG_ROW;
            ranges.set(yDim, alongRow ? fixed : along);
            ranges.set(xDim, alongRow ? along : fixed);
            long start = System.nanoTime();
            float[] values = toFloats(variable.read(ranges));
            return new Section(request, levels(), values, System.nanoTime() - start);
        } catch (InvalidRangeException e) {
            throw new IOException("Invalid section " + request + ": " + e.getMessage(), e);
        }
    }

    /**
     * Longitudes and latitudes of the points of a strided slice, as two arrays laid out like
     * {@link Slice#values()}. Returns {@code null} when the grid has no geographic coordinates.
     * Blocking I/O for curvilinear grids.
     */
    public float[][] readLonLat(Window w, int stride) throws IOException {
        int cols = Stride.samples(w.width(), stride);
        int rows = Stride.samples(w.height(), stride);
        if (isRegularLonLat()) {
            float[] lon = new float[cols * rows];
            float[] lat = new float[cols * rows];
            for (int r = 0; r < rows; r++) {
                float la = (float) y.value(w.j0() + r * stride);
                for (int c = 0; c < cols; c++) {
                    lon[r * cols + c] = (float) x.value(w.i0() + c * stride);
                    lat[r * cols + c] = la;
                }
            }
            return new float[][] {lon, lat};
        }
        if (!hasCurvilinearCoordinates()) {
            return null;
        }
        return new float[][] {readCoordinate(lon2d, w, stride), readCoordinate(lat2d, w, stride)};
    }

    /** Longitude and latitude of one grid point, or {@code null}. Blocking I/O when curvilinear. */
    public double[] lonLatAt(int i, int j) throws IOException {
        float[][] ll = readLonLat(new Window(i, j, i, j), 1);
        return ll == null ? null : new double[] {ll[0][0], ll[1][0]};
    }

    private float[] readCoordinate(Variable coord, Window w, int stride) throws IOException {
        List<Range> ranges = new ArrayList<>();
        int r = coord.getRank();
        try {
            for (int d = 0; d < r - 2; d++) {
                ranges.add(new Range(0, 0));
            }
            ranges.add(new Range(w.j0(), w.j1(), stride));
            ranges.add(new Range(w.i0(), w.i1(), stride));
            Array a = coord.read(ranges);
            return (float[]) a.get1DJavaArray(DataType.FLOAT);
        } catch (InvalidRangeException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private List<Range> baseRanges(int time) {
        List<Range> ranges = new ArrayList<>(rank);
        for (int d = 0; d < rank; d++) {
            ranges.add(d == tDim ? Range.make(time, time) : Range.make(0, 0));
        }
        return ranges;
    }

    private float[] toFloats(Array array) {
        float[] values = (float[]) array.get1DJavaArray(DataType.FLOAT);
        if (variable instanceof VariableDS vds && vds.hasMissing()) {
            for (int k = 0; k < values.length; k++) {
                if (!Float.isNaN(values[k]) && vds.isMissing(values[k])) {
                    values[k] = Float.NaN;
                }
            }
        }
        if (!Double.isNaN(validMin) || !Double.isNaN(validMax)) {
            double lo = Double.isNaN(validMin) ? Double.NEGATIVE_INFINITY : validMin;
            double hi = Double.isNaN(validMax) ? Double.POSITIVE_INFINITY : validMax;
            for (int k = 0; k < values.length; k++) {
                if (values[k] < lo || values[k] > hi) {
                    values[k] = Float.NaN;
                }
            }
        }
        // Huge fill values that escaped the conventions (1e20, 9.97e36...) are missing too.
        for (int k = 0; k < values.length; k++) {
            if (Math.abs(values[k]) >= 9.0e19f) {
                values[k] = Float.NaN;
            }
        }
        return values;
    }

    private static double[] validRange(Variable v) {
        double lo = Double.NaN;
        double hi = Double.NaN;
        Attribute range = v.findAttribute("valid_range");
        if (range != null && range.getLength() == 2 && !range.isString()) {
            lo = range.getNumericValue(0).doubleValue();
            hi = range.getNumericValue(1).doubleValue();
        }
        Attribute min = v.findAttribute("valid_min");
        if (min != null && !min.isString()) {
            lo = min.getNumericValue().doubleValue();
        }
        Attribute max = v.findAttribute("valid_max");
        if (max != null && !max.isString()) {
            hi = max.getNumericValue().doubleValue();
        }
        // Packed data: valid_* are in packed units, while reads return unpacked values.
        if (v instanceof VariableDS vds && (vds.getScaleFactor() != 1.0 || vds.getOffset() != 0.0)) {
            lo = Double.isNaN(lo) ? lo : lo * vds.getScaleFactor() + vds.getOffset();
            hi = Double.isNaN(hi) ? hi : hi * vds.getScaleFactor() + vds.getOffset();
        }
        return new double[] {lo, hi};
    }

    private static Variable coordinateOf(NetcdfFile file, Dimension dim) {
        Variable v = file.findVariable(NetcdfFile.makeValidPathName(dim.getShortName()));
        return v != null && v.getRank() == 1 && v.getDimension(0).equals(dim) ? v : null;
    }

    private static String unitsOf(Variable v) {
        String u = v.getUnitsString();
        return u == null ? "" : u.trim();
    }

    private static Axis axis(NetcdfFile file, Dimension dim) {
        Variable coord = coordinateOf(file, dim);
        if (coord == null || !coord.getDataType().isNumeric()) {
            return Axis.indexOnly(dim.getShortName(), dim.getLength());
        }
        try {
            double[] values = (double[]) coord.read().get1DJavaArray(DataType.DOUBLE);
            String units = unitsOf(coord);
            String[] labels = Coordinates.isTime(dim.getShortName(), units) ? timeLabels(file, coord, units, values) : null;
            return new Axis(dim.getShortName(), dim.getLength(), units, values, labels);
        } catch (IOException | RuntimeException e) {
            return Axis.indexOnly(dim.getShortName(), dim.getLength());
        }
    }

    private static String[] timeLabels(NetcdfFile file, Variable coord, String units, double[] values) {
        String[] climatology = climatologyLabels(file, coord, units, values.length);
        if (climatology != null) {
            return climatology;
        }
        try {
            long[] millis = decode(coord, units, values);
            boolean midnight = Arrays.stream(millis).allMatch(m -> Math.floorMod(m, 86_400_000L) == 0);
            DateTimeFormatter f = midnight ? DAY : MINUTE;
            return Arrays.stream(millis).mapToObj(m -> f.format(Instant.ofEpochMilli(m))).toArray(String[]::new);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static long[] decode(Variable coord, String units, double[] values) {
        CalendarDateUnit unit = CalendarDateUnit.of(coord.findAttributeString("calendar", null), units);
        long[] millis = new long[values.length];
        for (int k = 0; k < values.length; k++) {
            millis[k] = unit.makeCalendarDate(values[k]).getMillis();
        }
        return millis;
    }

    /**
     * CF climatologies (a {@code climatology} attribute naming the period bounds) and
     * "since year 0" units are labelled with their period instead of a meaningless date.
     */
    private static String[] climatologyLabels(NetcdfFile file, Variable coord, String units, int n) {
        String boundsName = coord.findAttributeString("climatology", null);
        boolean yearZero = units.matches("(?i).*since\\s+0{1,4}-.*");
        if (boundsName == null && !yearZero) {
            return null;
        }
        String[] labels = new String[n];
        Arrays.fill(labels, "climatology");
        Variable bounds = boundsName == null ? null : file.findVariable(NetcdfFile.makeValidPathName(boundsName));
        if (bounds == null || yearZero) {
            return labels;
        }
        try {
            double[] b = (double[]) bounds.read().get1DJavaArray(DataType.DOUBLE);
            long[] millis = decode(coord, units, b);
            long future = Instant.now().plus(java.time.Duration.ofDays(365L * 100)).toEpochMilli();
            if (Arrays.stream(millis).anyMatch(m -> m > future)) {
                return labels; // bounds that decode to centuries ahead are not real dates
            }
            DateTimeFormatter year = DateTimeFormatter.ofPattern("yyyy").withZone(ZoneOffset.UTC);
            for (int k = 0; k < n && 2 * k + 1 < millis.length; k++) {
                // Bounds end at the start of the next period, hence the minus one millisecond.
                labels[k] = "climatology " + year.format(Instant.ofEpochMilli(millis[2 * k]))
                        + "–" + year.format(Instant.ofEpochMilli(millis[2 * k + 1] - 1));
            }
        } catch (IOException | RuntimeException e) {
            // keep the plain label
        }
        return labels;
    }

    /**
     * Finds 2D longitude/latitude variables on the same (y, x) grid, from the CF
     * {@code coordinates} attribute first, then from the usual names (nav_lon, glamt...).
     */
    private static Variable[] findCurvilinearCoordinates(NetcdfFile file, Variable v, Dimension yd, Dimension xd) {
        List<Variable> candidates = new ArrayList<>();
        String coords = v.findAttributeString("coordinates", "");
        for (String name : coords.trim().split("\\s+")) {
            Variable c = name.isEmpty() ? null : file.findVariable(NetcdfFile.makeValidPathName(name));
            if (c != null) {
                candidates.add(c);
            }
        }
        candidates.addAll(file.getVariables());
        Variable lon = null;
        Variable lat = null;
        for (Variable c : candidates) {
            if (c.getRank() < 2 || !c.getDataType().isNumeric() || c == v) {
                continue;
            }
            List<Dimension> d = c.getDimensions();
            if (!d.get(d.size() - 1).equals(xd) || !d.get(d.size() - 2).equals(yd)) {
                continue;
            }
            String name = c.getShortName().toLowerCase(Locale.ROOT);
            String units = unitsOf(c);
            if (lon == null && Coordinates.isLongitude(name, units)) {
                lon = c;
            } else if (lat == null && Coordinates.isLatitude(name, units)) {
                lat = c;
            }
        }
        return new Variable[] {lon, lat};
    }
}
