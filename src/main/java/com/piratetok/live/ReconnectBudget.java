package com.piratetok.live;

import java.time.Duration;

/**
 * Counts consecutive failed attempts. A session that stayed up for {@link #HEALTHY_SESSION} resets the
 * count, so long-lived streams don't die after max_retries lifetime blips.
 */
final class ReconnectBudget {

    static final Duration HEALTHY_SESSION = Duration.ofSeconds(30);
    static final Duration DEVICE_BLOCKED_DELAY = Duration.ofSeconds(2);
    static final Duration MAX_BACKOFF = Duration.ofSeconds(30);

    enum Exit { CLOSED, DEVICE_BLOCKED, ERRORED, NO_TTWID }

    enum End { HEALTHY, FAILED, BLOCKED }

    /** How an attempt ended and whether to drop ttwid + UA. */
    record Judgement(End end, boolean rotate) {}

    /** {@code giveUp} means stop reconnecting; otherwise wait {@code delay} before attempt {@code attempt}. */
    record Verdict(boolean giveUp, int attempt, Duration delay) {}

    private final int maxRetries;
    private int attempt;

    ReconnectBudget(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    static Judgement judge(Exit exit, Duration lived) {
        boolean healthy = lived.compareTo(HEALTHY_SESSION) >= 0;
        End end = healthy ? End.HEALTHY : End.FAILED;
        return switch (exit) {
            case CLOSED -> new Judgement(end, false);
            case DEVICE_BLOCKED -> new Judgement(End.BLOCKED, true);
            case ERRORED -> new Judgement(end, !healthy);
            case NO_TTWID -> new Judgement(End.FAILED, true);
        };
    }

    Verdict record(End end) {
        attempt = end == End.HEALTHY ? 1 : attempt + 1;
        if (attempt > maxRetries) {
            return new Verdict(true, attempt, Duration.ZERO);
        }
        Duration delay = end == End.BLOCKED ? DEVICE_BLOCKED_DELAY : backoff(attempt);
        return new Verdict(false, attempt, delay);
    }

    static Duration backoff(int attempt) {
        return attempt >= 5 ? MAX_BACKOFF : Duration.ofSeconds(1L << attempt);
    }
}
