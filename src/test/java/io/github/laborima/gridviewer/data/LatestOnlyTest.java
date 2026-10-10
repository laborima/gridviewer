package io.github.laborima.gridviewer.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LatestOnlyTest {

    @TempDir
    Path dir;

    @Test
    void dropsRequestsOvertakenByNewerOnes() throws Exception {
        try (GridDataset ds = GridDataset.open(TestFiles.regular(dir).toString())) {
            CountDownLatch gate = new CountDownLatch(1);
            List<Integer> ran = new CopyOnWriteArrayList<>();
            List<Integer> delivered = new CopyOnWriteArrayList<>();
            CountDownLatch done = new CountDownLatch(1);
            LatestOnly<Integer, Integer> loader = new LatestOnly<>(ds, q -> {
                ran.add(q);
                return q * 10;
            }, Runnable::run);
            // Block the I/O thread so the requests queue up.
            ds.submit(() -> gate.await(5, TimeUnit.SECONDS));
            for (int k = 1; k <= 20; k++) {
                loader.request(k, r -> {
                    delivered.add(r);
                    done.countDown();
                }, e -> { });
            }
            gate.countDown();
            assertTrue(done.await(5, TimeUnit.SECONDS));
            ds.submit(() -> null).get();
            assertEquals(List.of(20), ran);
            assertEquals(List.of(200), delivered);
        }
    }
}
