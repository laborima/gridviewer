package io.github.laborima.gridviewer.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StrideAndWindowTest {

    @Test
    void neverReadsMoreThanOnePointPerPixel() {
        assertEquals(1, Stride.forAxis(500, 1000));
        assertEquals(1, Stride.forAxis(1000, 1000));
        assertEquals(1, Stride.forAxis(1999, 1000));
        assertEquals(2, Stride.forAxis(2000, 1000));
        assertEquals(22, Stride.forAxis(36000, 1600));
    }

    @Test
    void oneStrideKeepsCellsSquare() {
        Window w = new Window(0, 0, 35999, 17998);
        // 1600 x 800 pixels: the axis that needs the finer stride wins.
        assertEquals(22, Stride.forWindow(w, 1600, 800));
        assertEquals(11, Stride.forWindow(w, 3200, 800));
    }

    @Test
    void sampleCountRoundsUp() {
        assertEquals(4, Stride.samples(10, 3));
        assertEquals(10, Stride.samples(10, 1));
        assertEquals(1, Stride.samples(1, 5));
    }

    @Test
    void windowClampsToTheGrid() {
        Window w = new Window(-10, -5, 100, 50).clamp(60, 40);
        assertEquals(new Window(0, 0, 59, 39), w);
        assertEquals(60, w.width());
        assertEquals(2400, w.cells());
    }

    @Test
    void alignmentKeepsSamplesOnTheSameLattice() {
        Window w = new Window(13, 7, 90, 80).alignTo(4);
        assertEquals(12, w.i0());
        assertEquals(4, w.j0());
        assertEquals(90, w.i1());
        assertTrue(w.contains(13, 7));
        assertFalse(w.contains(91, 7));
    }

    @Test
    void emptyWindowsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Window(5, 0, 4, 0));
    }
}
