package io.github.laborima.gridviewer.data;

import java.io.IOException;
import java.nio.file.Path;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.write.NetcdfFormatWriter;

/** Small NetCDF files written on the fly for the tests. */
public final class TestFiles {

    public static final int NT = 3;
    public static final int NZ = 5;
    public static final int NY = 40;
    public static final int NX = 60;
    public static final float FILL = -999f;

    private TestFiles() {
    }

    /** Value stored at (t, z, j, i): every index is readable back from the number. */
    public static float value(int t, int z, int j, int i) {
        return t * 1_000_000f + z * 100_000f + j * 100f + i;
    }

    /** Columns i < 3 are land, marked with the fill value. */
    public static boolean land(int i) {
        return i < 3;
    }

    /** CF file: temp(time, depth, lat, lon) on a regular grid, with a fill value. */
    public static Path regular(Path dir) throws IOException {
        Path file = dir.resolve("regular.nc");
        NetcdfFormatWriter.Builder b = NetcdfFormatWriter.createNewNetcdf3(file.toString());
        b.addDimension("time", NT);
        b.addDimension("depth", NZ);
        b.addDimension("lat", NY);
        b.addDimension("lon", NX);
        b.addVariable("time", DataType.DOUBLE, "time").addAttribute(new Attribute("units", "days since 2020-01-01"));
        b.addVariable("depth", DataType.DOUBLE, "depth").addAttribute(new Attribute("units", "m"));
        b.addVariable("lat", DataType.DOUBLE, "lat").addAttribute(new Attribute("units", "degrees_north"));
        b.addVariable("lon", DataType.DOUBLE, "lon").addAttribute(new Attribute("units", "degrees_east"));
        b.addVariable("temp", DataType.FLOAT, "time depth lat lon")
                .addAttribute(new Attribute("units", "degrees_Celsius"))
                .addAttribute(new Attribute("long_name", "sea water temperature"))
                .addAttribute(new Attribute("_FillValue", FILL));
        float[] data = new float[NT * NZ * NY * NX];
        int k = 0;
        for (int t = 0; t < NT; t++) {
            for (int z = 0; z < NZ; z++) {
                for (int j = 0; j < NY; j++) {
                    for (int i = 0; i < NX; i++) {
                        data[k++] = land(i) ? FILL : value(t, z, j, i);
                    }
                }
            }
        }
        try (NetcdfFormatWriter w = b.build()) {
            w.write("time", Array.factory(DataType.DOUBLE, new int[] {NT}, new double[] {0, 1, 2}));
            w.write("depth", Array.factory(DataType.DOUBLE, new int[] {NZ}, new double[] {0, 10, 50, 100, 500}));
            double[] lat = new double[NY];
            for (int j = 0; j < NY; j++) {
                lat[j] = -39 + 2 * j;
            }
            double[] lon = new double[NX];
            for (int i = 0; i < NX; i++) {
                lon[i] = 6 * i;
            }
            w.write("lat", Array.factory(DataType.DOUBLE, new int[] {NY}, lat));
            w.write("lon", Array.factory(DataType.DOUBLE, new int[] {NX}, lon));
            w.write("temp", Array.factory(DataType.FLOAT, new int[] {NT, NZ, NY, NX}, data));
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
        return file;
    }

    /**
     * NEMO-like file: votemper(time_counter, deptht, y, x) with 2D nav_lon/nav_lat, no fill
     * value, land stored as 0.
     */
    public static Path nemo(Path dir) throws IOException {
        Path file = dir.resolve("nemo.nc");
        int nt = 2;
        int nz = 3;
        int ny = 20;
        int nx = 30;
        NetcdfFormatWriter.Builder b = NetcdfFormatWriter.createNewNetcdf3(file.toString());
        b.addDimension("time_counter", nt);
        b.addDimension("deptht", nz);
        b.addDimension("y", ny);
        b.addDimension("x", nx);
        b.addVariable("nav_lon", DataType.FLOAT, "y x").addAttribute(new Attribute("units", "degrees_east"));
        b.addVariable("nav_lat", DataType.FLOAT, "y x").addAttribute(new Attribute("units", "degrees_north"));
        b.addVariable("deptht", DataType.FLOAT, "deptht").addAttribute(new Attribute("units", "m"));
        b.addVariable("time_counter", DataType.DOUBLE, "time_counter")
                .addAttribute(new Attribute("units", "seconds since 2006-01-01 00:00:00"));
        b.addVariable("votemper", DataType.FLOAT, "time_counter deptht y x")
                .addAttribute(new Attribute("units", "C"));
        float[] lon = new float[ny * nx];
        float[] lat = new float[ny * nx];
        for (int j = 0; j < ny; j++) {
            for (int i = 0; i < nx; i++) {
                lon[j * nx + i] = -60 + i + 0.1f * j;
                lat[j * nx + i] = 10 + j - 0.05f * i;
            }
        }
        float[] t = new float[nt * nz * ny * nx];
        for (int k = 0; k < t.length; k++) {
            int i = k % nx;
            t[k] = i < 2 ? 0f : 10 + k % 7;
        }
        try (NetcdfFormatWriter w = b.build()) {
            w.write("nav_lon", Array.factory(DataType.FLOAT, new int[] {ny, nx}, lon));
            w.write("nav_lat", Array.factory(DataType.FLOAT, new int[] {ny, nx}, lat));
            w.write("deptht", Array.factory(DataType.FLOAT, new int[] {nz}, new float[] {0.5f, 5f, 50f}));
            w.write("time_counter", Array.factory(DataType.DOUBLE, new int[] {nt}, new double[] {43200, 129600}));
            w.write("votemper", Array.factory(DataType.FLOAT, new int[] {nt, nz, ny, nx}, t));
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
        return file;
    }
}
