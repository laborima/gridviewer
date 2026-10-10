package io.github.laborima.gridviewer.data;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Runs requests on the dataset I/O thread, keeping only the most recent one. While the user
 * scrolls, zooms or drags a slider, intermediate requests that have not started yet are
 * dropped and results that are already stale are never delivered.
 *
 * @param <Q> request type
 * @param <R> result type
 */
public final class LatestOnly<Q, R> {

    /** Blocking work run on the I/O thread. */
    @FunctionalInterface
    public interface Work<Q, R> {
        R run(Q request) throws Exception;
    }

    private record Job<Q, R>(long id, Q request, Consumer<R> onResult, Consumer<Throwable> onError) {
    }

    private final GridDataset dataset;
    private final Work<Q, R> work;
    private final Executor deliver;
    private final AtomicReference<Job<Q, R>> pending = new AtomicReference<>();
    private final AtomicLong latest = new AtomicLong();

    /**
     * @param deliver where results are handed over, typically {@code Platform::runLater}
     */
    public LatestOnly(GridDataset dataset, Work<Q, R> work, Executor deliver) {
        this.dataset = dataset;
        this.work = work;
        this.deliver = deliver;
    }

    public void request(Q request, Consumer<R> onResult, Consumer<Throwable> onError) {
        pending.set(new Job<>(latest.incrementAndGet(), request, onResult, onError));
        dataset.executor().execute(this::drain);
    }

    /** Forgets any queued request and makes in-flight results stale. */
    public void cancel() {
        latest.incrementAndGet();
        pending.set(null);
    }

    private void drain() {
        Job<Q, R> job = pending.getAndSet(null);
        if (job == null || job.id() != latest.get()) {
            return;
        }
        try {
            R result = work.run(job.request());
            deliver.execute(() -> {
                if (job.id() == latest.get()) {
                    job.onResult().accept(result);
                }
            });
        } catch (Exception e) {
            deliver.execute(() -> {
                if (job.id() == latest.get()) {
                    job.onError().accept(e);
                }
            });
        }
    }
}
