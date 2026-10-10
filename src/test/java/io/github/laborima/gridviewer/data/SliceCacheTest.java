package io.github.laborima.gridviewer.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SliceCacheTest {

    private static Slice slice(int level, int n) {
        SliceRequest r = new SliceRequest("v", 0, level, new Window(0, 0, n - 1, 0), 1);
        return new Slice(r, new float[n], 0);
    }

    @Test
    void evictsLeastRecentlyUsedBeyondBudget() {
        SliceCache cache = new SliceCache(1000);
        Slice a = slice(0, 100);
        Slice b = slice(1, 100);
        Slice c = slice(2, 100);
        cache.put(a);
        cache.put(b);
        assertNotNull(cache.get(a.request()));
        cache.put(c);
        assertNull(cache.get(b.request()));
        assertNotNull(cache.get(a.request()));
        assertNotNull(cache.get(c.request()));
        assertEquals(800, cache.bytes());
    }

    @Test
    void ignoresSlicesLargerThanTheBudget() {
        SliceCache cache = new SliceCache(100);
        cache.put(slice(0, 1000));
        assertEquals(0, cache.bytes());
    }
}
