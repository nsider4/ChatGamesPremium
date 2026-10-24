package org.nsider.chatgames.discord;

import org.nsider.inventorymanager.extras.Nonnull;
import org.nsider.inventorymanager.extras.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Sends messages on one background thread, in the order they were queued, so an edit always
 * comes after the post it edits and the server thread never waits on Discord.
 *
 * <p>A target that keeps failing is reported once, then at most every few minutes, so a
 * deleted webhook doesn't fill the console.</p>
 */
final class DiscordQueue {

    private static final long REPORT_EVERY_MILLIS = 5 * 60_000;
    /** More waiting than this means Discord is down or far behind; newer messages matter more. */
    private static final int MAX_QUEUED = 200;

    private final BiConsumer<String, Throwable> warn;
    private final ExecutorService executor;
    private final Map<String, Long> lastReport = new ConcurrentHashMap<>();

    DiscordQueue(@Nonnull BiConsumer<String, Throwable> warn) {
        this.warn = warn;
        this.executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(MAX_QUEUED), runnable -> {
            Thread thread = new Thread(runnable, "ChatGames Discord");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    /** Posts to every target; {@code live} (if given) keeps each message's id for later edits. */
    void send(@Nonnull List<DiscordTarget> targets, @Nonnull DiscordMessage message, @Nullable LiveMessage live) {
        if (message.isEmpty() || targets.isEmpty()) return;
        submit(() -> {
            for (DiscordTarget target : targets) {
                try {
                    String id = target.send(message);
                    if (live != null && id != null) live.ids.put(target, id);
                    lastReport.remove(target.describe());
                } catch (IOException | RuntimeException e) {
                    report(target, e);
                }
            }
        });
    }

    /** Edits the messages of a live post; targets where the post failed get the message posted instead. */
    void edit(@Nonnull LiveMessage live, @Nonnull DiscordMessage message) {
        if (message.isEmpty()) return;
        submit(() -> {
            for (DiscordTarget target : live.targets) {
                String id = live.ids.get(target);
                try {
                    if (id != null) {
                        target.edit(id, message);
                    } else {
                        String posted = target.send(message);
                        if (posted != null) live.ids.put(target, posted);
                    }
                } catch (IOException | RuntimeException e) {
                    report(target, e);
                }
            }
        });
    }

    /** Waits a little for queued messages (results posted as the server stops), then stops the thread. */
    void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void submit(Runnable task) {
        try {
            executor.execute(task);
        } catch (RuntimeException e) {
            // Shut down (plugin disabling): nothing more is sent
        }
    }

    private void report(DiscordTarget target, Exception e) {
        String key = target.describe();
        long now = System.currentTimeMillis();
        Long last = lastReport.get(key);
        if (last != null && now - last < REPORT_EVERY_MILLIS) return;
        lastReport.put(key, now);
        warn.accept("Could not post to " + key + ": " + e.getMessage(), e instanceof IOException ? null : e);
    }

    /** A message posted to some targets that can be edited later (a game's start, turned into its result). */
    static final class LiveMessage {
        final List<DiscordTarget> targets;
        final Map<DiscordTarget, String> ids = new ConcurrentHashMap<>();

        LiveMessage(@Nonnull List<DiscordTarget> targets) {
            this.targets = Collections.unmodifiableList(new ArrayList<>(targets));
        }
    }
}
