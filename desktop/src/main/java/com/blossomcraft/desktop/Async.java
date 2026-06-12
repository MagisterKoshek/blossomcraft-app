package com.blossomcraft.desktop;

import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Runs blocking API calls off the JavaFX Application Thread and delivers the
 * result (or error) back on it. Keeps the UI responsive without each page
 * having to manage its own threads.
 */
public final class Async {

    private static final java.util.concurrent.ExecutorService POOL =
            Executors.newCachedThreadPool(new ThreadFactory() {
                private final AtomicInteger n = new AtomicInteger();

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "bc-async-" + n.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            });

    private Async() {
    }

    /** Run {@code work} in the background; deliver success or failure on the FX thread. */
    public static <T> void run(Supplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        POOL.submit(() -> {
            try {
                T result = work.get();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Throwable t) {
                Platform.runLater(() -> onError.accept(t));
            }
        });
    }

    /** Run a void task in the background. */
    public static void run(Runnable work, Runnable onSuccess, Consumer<Throwable> onError) {
        run(() -> {
            work.run();
            return null;
        }, ignored -> onSuccess.run(), onError);
    }

    public static void shutdown() {
        POOL.shutdownNow();
    }
}
