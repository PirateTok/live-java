package com.piratetok.live;

import com.piratetok.live.ReconnectBudget.Exit;
import com.piratetok.live.ReconnectLoop.Outcome;
import com.piratetok.live.ReconnectLoop.Session;
import com.piratetok.live.events.EventType;
import com.piratetok.live.events.TikTokEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** F4 + F5 at client level: the reconnect loop against fake ttwid / session / clock. */
class ReconnectLoopTest {

    private final AtomicInteger freshCalls = new AtomicInteger();
    private final AtomicInteger runCalls = new AtomicInteger();
    private final List<Duration> delays = new CopyOnWriteArrayList<>();
    private final List<TikTokEvent> events = new CopyOnWriteArrayList<>();
    private final AtomicBoolean stopped = new AtomicBoolean();

    private void run(int maxRetries, int freshFails, Exit exit) throws Exception {
        var deps = new ReconnectLoop.Deps(
            () -> freshCalls.incrementAndGet() <= freshFails ? null : new Session("t", "u"),
            s -> {
                runCalls.incrementAndGet();
                return CompletableFuture.completedFuture(new Outcome(exit, Duration.ofSeconds(1), s));
            },
            d -> { delays.add(d); return Runnable::run; },
            events::add,
            stopped::get,
            Runnable::run);
        ReconnectLoop.run("7", maxRetries, deps).get(5, TimeUnit.SECONDS);
    }

    private List<String> types() {
        return events.stream().map(TikTokEvent::type).toList();
    }

    @Test
    void reconnectingPerRetryThenDisconnectedOnce() throws Exception {
        run(3, 2, Exit.ERRORED);
        assertEquals(List.of(EventType.RECONNECTING, EventType.RECONNECTING, EventType.RECONNECTING, EventType.DISCONNECTED), types());
        assertEquals(List.of(1, 2, 3), events.subList(0, 3).stream().map(e -> e.data().get("attempt")).toList());
        // 2 ttwid failures + 2 young-error sessions, each rotating → fresh every attempt
        assertEquals(4, freshCalls.get());
        assertEquals(2, runCalls.get());
        assertEquals(List.of(Duration.ofSeconds(2), Duration.ofSeconds(4), Duration.ofSeconds(8)), delays);
    }

    @Test
    void cleanCloseReusesTtwidAndUa() throws Exception {
        run(4, 0, Exit.CLOSED);
        assertEquals(1, freshCalls.get());
        assertEquals(5, runCalls.get());
        assertEquals(4, types().stream().filter(EventType.RECONNECTING::equals).count());
        assertEquals(1, types().stream().filter(EventType.DISCONNECTED::equals).count());
    }

    @Test
    void deviceBlockedRotatesWithShortDelay() throws Exception {
        run(2, 0, Exit.DEVICE_BLOCKED);
        assertEquals(3, freshCalls.get());
        assertEquals(ReconnectBudget.DEVICE_BLOCKED_DELAY, delays.getFirst());
    }

    @Test
    void userStopDisconnectsOnce() throws Exception {
        var deps = new ReconnectLoop.Deps(
            () -> new Session("t", "u"),
            s -> { stopped.set(true); return CompletableFuture.completedFuture(new Outcome(Exit.CLOSED, Duration.ZERO, s)); },
            d -> Runnable::run,
            events::add,
            stopped::get,
            Runnable::run);
        ReconnectLoop.run("7", 5, deps).get(5, TimeUnit.SECONDS);
        assertEquals(List.of(EventType.DISCONNECTED), types());
    }
}
