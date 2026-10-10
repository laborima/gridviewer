package io.github.laborima.gridviewer.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.laborima.gridviewer.data.Axis;
import io.github.laborima.gridviewer.data.Window;
import org.junit.jupiter.api.Test;

class RenderTest {

    @Test
    void everyBundledColourMapLoads() {
        assertTrue(ColorMaps.all().size() >= 29);
        for (ColorMap m : ColorMaps.all()) {
            assertEquals(ColorMap.SIZE, m.argb().length, m.name());
            assertFalse(m.source().isBlank(), m.name());
            for (int c : m.argb()) {
                assertEquals(0xFF, c >>> 24, m.name() + " must be opaque");
            }
        }
        assertEquals("thermal", ColorMaps.get("does-not-exist").name());
    }

    @Test
    void suggestsOceanographicColourMaps() {
        assertEquals("thermal", ColorMaps.suggest("votemper", "Temperature", "C").name());
        assertEquals("haline", ColorMaps.suggest("so", "Sea Water Salinity", "1e-3").name());
        assertEquals("balance", ColorMaps.suggest("anom", "Daily sea surface temperature anomalies", "Celsius").name());
        assertEquals("balance", ColorMaps.suggest("sossheig", "Sea Surface Height", "m").name());
        assertEquals("ice", ColorMaps.suggest("siconc", "Sea ice concentration", "%").name());
        assertEquals("viridis", ColorMaps.suggest("foo", "bar", "").name());
        assertTrue(ColorMaps.isDiverging(ColorMaps.get("balance")));
        assertFalse(ColorMaps.isDiverging(ColorMaps.get("thermal")));
    }

    @Test
    void keepsThe2006DefaultRanges() {
        assertArrayEquals(new double[] {-4, 35}, ColorMaps.defaultRange("votemper"));
        assertEquals(null, ColorMaps.defaultRange("unknown"));
    }

    @Test
    void autoRangeIgnoresMissingValuesAndOutliers() {
        float[] v = new float[1000];
        for (int k = 0; k < v.length; k++) {
            v[k] = k % 10 == 0 ? Float.NaN : k / 100f;
        }
        v[1] = 1e6f;
        ValueRange r = ValueRange.auto(v);
        assertTrue(r.min() >= 0 && r.min() < 0.2, "min " + r.min());
        assertTrue(r.max() > 9.7 && r.max() < 10.1, "max " + r.max());
        assertEquals(new ValueRange(-3, 3), new ValueRange(-1, 3).symmetric());
        assertEquals(new ValueRange(0, 1), ValueRange.auto(new float[] {Float.NaN}));
    }

    @Test
    void degenerateRangesAreWidened() {
        ValueRange r = new ValueRange(5, 5);
        assertTrue(r.max() > r.min());
    }

    @Test
    void rasterizerFlipsRowsAndMasksLand() {
        ColorMap gray = ColorMap.fromStops("gray", "test", new int[][] {{0, 0, 0}, {255, 255, 255}});
        float[] values = {0, 1, Float.NaN, 0.5f};
        int[] img = Rasterizer.colorize(values, 2, gray, new ValueRange(0, 1), true, false);
        // Row 1 (NaN, 0.5) is drawn on top.
        assertEquals(Rasterizer.LAND, img[0]);
        assertEquals(gray.color(0.5), img[1]);
        assertEquals(0xFF000000, img[2]);
        assertEquals(0xFFFFFFFF, img[3]);
        int[] masked = Rasterizer.colorize(values, 2, gray, new ValueRange(0, 1), false, true);
        assertEquals(Rasterizer.LAND, masked[0]);
    }

    @Test
    void globeTextureFindsTheRightSample() {
        // 1-degree global grid, longitudes 0..359, latitudes -89.5..89.5.
        double[] lon = new double[360];
        for (int i = 0; i < 360; i++) {
            lon[i] = i;
        }
        double[] lat = new double[180];
        for (int j = 0; j < 180; j++) {
            lat[j] = -89.5 + j;
        }
        int[] colors = new int[360 * 180];
        colors[(90 + 10) * 360 + 350] = 0xFFFF0000; // lat 10.5N, lon 350E = 10W
        int[] tex = GlobeTexture.regular(colors, new Window(0, 0, 359, 179), 1,
                new Axis("lon", 360, "degrees_east", lon, null), new Axis("lat", 180, "degrees_north", lat, null),
                360, 180);
        // Texture: column 0 is 180W, so 10W is column 170; row 0 is 90N, so 10.5N is row 79.
        assertEquals(0xFFFF0000, tex[79 * 360 + 170]);
        assertNotNull(tex);
    }
}
