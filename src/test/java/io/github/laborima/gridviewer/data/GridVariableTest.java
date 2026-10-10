package io.github.laborima.gridviewer.data;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GridVariableTest {

    @TempDir
    Path dir;

    @Test
    void recognisesTheAxesOfACfFile() throws Exception {
        try (GridDataset ds = GridDataset.open(TestFiles.regular(dir).toString())) {
            GridVariable v = ds.variable("temp");
            assertNotNull(v);
            assertEquals(TestFiles.NX, v.nx());
            assertEquals(TestFiles.NY, v.ny());
            assertEquals(TestFiles.NZ, v.levels());
            assertEquals(TestFiles.NT, v.times());
            assertTrue(v.isRegularLonLat());
            assertEquals("50 m", v.levelAxis().label(2));
            assertEquals("2020-01-03", v.timeAxis().label(2));
            assertEquals("sea water temperature", v.longName());
            assertTrue(v.declaresMissingValues());
        }
    }

    @Test
    void stridedSliceReadsEveryNthPointOfTheWindow() throws Exception {
        try (GridDataset ds = GridDataset.open(TestFiles.regular(dir).toString())) {
            GridVariable v = ds.variable("temp");
            Window w = new Window(4, 5, 30, 20);
            SliceRequest r = new SliceRequest("temp", 2, 3, w, 3);
            Slice s = ds.submit(() -> ds.readSlice(v, r)).get();
            assertEquals(9, s.columns());
            assertEquals(6, s.rows());
            assertEquals(54, s.values().length);
            for (int row = 0; row < s.rows(); row++) {
                for (int col = 0; col < s.columns(); col++) {
                    int j = 5 + 3 * row;
                    int i = 4 + 3 * col;
                    assertEquals(TestFiles.value(2, 3, j, i), s.values()[row * s.columns() + col], 0f);
                    assertEquals(TestFiles.value(2, 3, j, i), s.valueAt(i, j), 0f);
                }
            }
            // A cell between samples reports the sample that covers it.
            assertEquals(TestFiles.value(2, 3, 5, 4), s.valueAt(6, 7), 0f);
            assertTrue(Float.isNaN(s.valueAt(0, 0)));
        }
    }

    @Test
    void fillValuesBecomeNaN() throws Exception {
        try (GridDataset ds = GridDataset.open(TestFiles.regular(dir).toString())) {
            GridVariable v = ds.variable("temp");
            Slice s = ds.submit(() -> ds.readSlice(v, new SliceRequest("temp", 0, 0, Window.full(v.nx(), v.ny()), 1))).get();
            for (int i = 0; i < v.nx(); i++) {
                assertEquals(TestFiles.land(i), Float.isNaN(s.valueAt(i, 7)), "column " + i);
            }
        }
    }

    @Test
    void sectionsGoThroughEveryLevel() throws Exception {
        try (GridDataset ds = GridDataset.open(TestFiles.regular(dir).toString())) {
            GridVariable v = ds.variable("temp");
            SectionRequest row = new SectionRequest("temp", 1, SectionRequest.Orientation.ALONG_ROW, 12, 10, 50, 4);
            Section s = ds.submit(() -> v.readSection(row)).get();
            assertEquals(TestFiles.NZ, s.levels());
            assertEquals(11, s.columns());
            assertEquals(TestFiles.value(1, 4, 12, 14), s.valueAt(4, 1), 0f);

            SectionRequest col = new SectionRequest("temp", 0, SectionRequest.Orientation.ALONG_COLUMN, 33, 0, 39, 1);
            Section c = ds.submit(() -> v.readSection(col)).get();
            assertEquals(40, c.columns());
            assertEquals(TestFiles.value(0, 2, 17, 33), c.valueAt(2, 17), 0f);
        }
    }

    @Test
    void regularGridLonLatFollowTheStride() throws Exception {
        try (GridDataset ds = GridDataset.open(TestFiles.regular(dir).toString())) {
            GridVariable v = ds.variable("temp");
            float[][] ll = v.readLonLat(new Window(2, 1, 11, 4), 3);
            assertArrayEquals(new float[] {12, 30, 48, 66, 12, 30, 48, 66}, ll[0], 0f);
            assertEquals(-37f, ll[1][0], 0f);
            assertEquals(-31f, ll[1][4], 0f);
        }
    }

    @Test
    void nemoOutputsAreRecognised() throws Exception {
        try (GridDataset ds = GridDataset.open(TestFiles.nemo(dir).toString())) {
            GridVariable v = ds.variable("votemper");
            assertNotNull(v);
            assertEquals(2, v.times());
            assertEquals(3, v.levels());
            assertFalse(v.isRegularLonLat());
            assertTrue(v.hasCurvilinearCoordinates());
            assertFalse(v.declaresMissingValues());
            assertEquals("2006-01-01 12:00", v.timeAxis().label(0));
            double[] ll = ds.submit(() -> v.lonLatAt(4, 10)).get();
            assertEquals(-60 + 4 + 1.0, ll[0], 1e-5);
            assertEquals(10 + 10 - 0.2, ll[1], 1e-5);
            // The data variable comes before the coordinates in the list.
            assertEquals("votemper", ds.variables().getFirst().name());
        }
    }

    @Test
    void indexGridsHaveNoGeographicCoordinates() throws Exception {
        try (GridDataset ds = GridDataset.open(TestFiles.nemo(dir).toString())) {
            GridVariable lon = ds.variable("nav_lon");
            assertNotNull(lon);
            assertEquals(1, lon.levels());
            assertNull(lon.timeAxis());
        }
    }
}
