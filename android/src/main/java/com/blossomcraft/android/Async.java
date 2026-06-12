package com.blossomcraft.android;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs blocking core/API calls off the main thread and delivers the result back
 * on it, so Activities/Fragments can call services without freezing the UI.
 */
public final class Async {

    public interface Work<T> {
        T run() throws Exception;
    }

    public interface OnSuccess<T> {
        void accept(T value);
    }

    public interface OnError {
        void accept(Throwable error);
    }

    private static final ExecutorService POOL = Executors.newCachedThreadPool();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Async() {
    }

    public static <T> void run(Work<T> work, OnSuccess<T> onSuccess, OnError onError) {
        POOL.execute(() -> {
            try {
                T result = work.run();
                MAIN.post(() -> onSuccess.accept(result));
            } catch (Throwable t) {
                MAIN.post(() -> onError.accept(t));
            }
        });
    }

    public static String message(Throwable t) {
        return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    }
}
