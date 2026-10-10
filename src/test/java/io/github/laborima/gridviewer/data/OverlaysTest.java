package io.github.laborima.gridviewer.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OverlaysTest {

    @TempDir
    Path dir;

    @Test
    void readsPointLists() throws Exception {
        Path f = Files.writeString(dir.resolve("points.txt"), """
                # moorings
                10 20 PIRATA 0N 23W
                 3   4

                bad line
                5,6
                """);
        List<Overlays.Point> points = Overlays.readPoints(f);
        assertEquals(3, points.size());
        assertEquals(new Overlays.Point(10, 20, "PIRATA 0N 23W"), points.get(0));
        assertEquals(new Overlays.Point(3, 4, ""), points.get(1));
        assertEquals(new Overlays.Point(5, 6, ""), points.get(2));
    }

    @Test
    void readsNemoLayoutFiles() throws Exception {
        Path f = Files.writeString(dir.resolve("layout.dat"), """
                    jpnij   jpi   jpj   jpk  jpiglo  jpjglo
                       4    92    76    46     182     149
                 nono jpi jpj nldi nldj nlei nlej nimp njmp nono nono nono nono
                    0   92   76    2    2   91   75    1    1   -1    1   -1   -1
                    1   92   76    2    2   91   75   91    1    0   -1   -1    3
                """);
        List<Overlays.Domain> d = Overlays.readLayout(f);
        assertEquals(2, d.size());
        assertEquals(new Overlays.Domain(0, new Window(0, 0, 91, 75)), d.get(0));
        assertEquals(new Overlays.Domain(1, new Window(90, 0, 181, 75)), d.get(1));
    }
}
