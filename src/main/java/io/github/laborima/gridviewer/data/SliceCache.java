package io.github.laborima.gridviewer.data;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Least-recently-used cache of slices, bounded in bytes, so going back to a previous level,
 * time step or zoom costs no I/O.
 */
final class SliceCache {

    private final long budget;
    private final LinkedHashMap<SliceRequest, Slice> map = new LinkedHashMap<>(64, 0.75f, true);
    private long bytes;

    SliceCache(long budgetBytes) {
        this.budget = budgetBytes;
    }

    synchronized Slice get(SliceRequest request) {
        return map.get(request);
    }

    synchronized void put(Slice slice) {
        if (slice.bytes() > budget) {
            return;
        }
        Slice previous = map.put(slice.request(), slice);
        if (previous != null) {
            bytes -= previous.bytes();
        }
        bytes += slice.bytes();
        Iterator<Map.Entry<SliceRequest, Slice>> it = map.entrySet().iterator();
        while (bytes > budget && it.hasNext()) {
            bytes -= it.next().getValue().bytes();
            it.remove();
        }
    }

    synchronized void clear() {
        map.clear();
        bytes = 0;
    }

    synchronized long bytes() {
        return bytes;
    }
}
