package com.piratetok.live;

import com.piratetok.live.events.EventType;
import com.piratetok.live.events.TikTokEvent;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * The reconnect loop with its side effects injected. ttwid + UA are fetched once and reused; they rotate only
 * on DEVICE_BLOCKED, a ttwid failure, or a connection that died young. A ttwid failure is a failed attempt.
 * Emits {@code reconnecting} per retry and {@code disconnected} exactly once, last.
 */
final class ReconnectLoop {

    private static final Logger log = Logger.getLogger(ReconnectLoop.class.getName());

    /** ttwid + UA pair, reused across reconnects until rotated. */
    record Session(String ttwid, String ua) {}

    record Outcome(ReconnectBudget.Exit exit, Duration lived, Session session) {}

    /**
     * @param fresh   fetches ttwid + UA, {@code null} on failure (runs on {@code executor})
     * @param run     runs one WSS session for a held session
     * @param delayer executor that runs a task after the given delay
     */
    record Deps(Supplier<Session> fresh,
                Function<Session, CompletableFuture<Outcome>> run,
                Function<Duration, Executor> delayer,
                Consumer<TikTokEvent> emit,
                BooleanSupplier stopped,
                Executor executor) {}

    static CompletableFuture<Void> run(String roomId, int maxRetries, Deps deps) {
        return step(roomId, maxRetries, new ReconnectBudget(maxRetries), null, deps)
                .whenComplete((v, ex) -> deps.emit().accept(new TikTokEvent(EventType.DISCONNECTED, null, roomId)));
    }

    private static CompletableFuture<Void> step(
            String roomId, int maxRetries, ReconnectBudget budget, Session held, Deps deps) {
        if (deps.stopped().getAsBoolean()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Session> session = held != null
                ? CompletableFuture.completedFuture(held)
                : CompletableFuture.supplyAsync(deps.fresh(), deps.executor());

        return session.thenCompose(s -> s == null
                ? CompletableFuture.completedFuture(new Outcome(ReconnectBudget.Exit.NO_TTWID, Duration.ZERO, null))
                : deps.run().apply(s)
        ).thenCompose(outcome -> {
            if (deps.stopped().getAsBoolean()) {
                return CompletableFuture.completedFuture(null);
            }
            var judgement = ReconnectBudget.judge(outcome.exit(), outcome.lived());
            Session next = judgement.rotate() ? null : outcome.session();
            var verdict = budget.record(judgement.end());
            if (verdict.giveUp()) {
                log.info("max retries (" + maxRetries + ") exceeded at attempt " + verdict.attempt());
                return CompletableFuture.completedFuture(null);
            }
            long delaySecs = verdict.delay().toSeconds();
            deps.emit().accept(new TikTokEvent(EventType.RECONNECTING,
                Map.of("attempt", verdict.attempt(), "maxRetries", maxRetries, "delaySecs", delaySecs), roomId));
            log.info("reconnecting in " + delaySecs + "s (attempt " + verdict.attempt() + "/" + maxRetries + ")");
            return CompletableFuture.runAsync(() -> {}, deps.delayer().apply(verdict.delay()))
                    .thenCompose(ignored -> step(roomId, maxRetries, budget, next, deps));
        });
    }

    private ReconnectLoop() {}
}
