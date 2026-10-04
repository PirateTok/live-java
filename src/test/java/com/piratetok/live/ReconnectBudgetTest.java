package com.piratetok.live;

import com.piratetok.live.ReconnectBudget.End;
import com.piratetok.live.ReconnectBudget.Exit;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconnectBudgetTest {

    private static final Duration SHORT = Duration.ofSeconds(3);
    private static final Duration LONG = Duration.ofSeconds(45);

    @Test
    void consecutiveFailuresAccumulateUntilGiveUp() {
        var budget = new ReconnectBudget(3);
        assertEquals(new ReconnectBudget.Verdict(false, 1, Duration.ofSeconds(2)), budget.record(End.FAILED));
        assertEquals(new ReconnectBudget.Verdict(false, 2, Duration.ofSeconds(4)), budget.record(End.FAILED));
        assertEquals(new ReconnectBudget.Verdict(false, 3, Duration.ofSeconds(8)), budget.record(End.FAILED));
        var last = budget.record(End.FAILED);
        assertTrue(last.giveUp());
        assertEquals(4, last.attempt());
    }

    @Test
    void healthySessionResetsTheCount() {
        var budget = new ReconnectBudget(3);
        budget.record(End.FAILED);
        budget.record(End.FAILED);
        budget.record(End.FAILED);
        var healthy = budget.record(End.HEALTHY);
        assertFalse(healthy.giveUp());
        assertEquals(1, healthy.attempt());
        for (int i = 0; i < 20; i++) {
            assertFalse(budget.record(End.FAILED).giveUp());
            assertFalse(budget.record(End.HEALTHY).giveUp());
        }
    }

    @Test
    void blockedUsesShortDelayAndStillCounts() {
        var budget = new ReconnectBudget(2);
        var v = budget.record(End.BLOCKED);
        assertEquals(ReconnectBudget.DEVICE_BLOCKED_DELAY, v.delay());
        budget.record(End.BLOCKED);
        assertTrue(budget.record(End.BLOCKED).giveUp());
    }

    @Test
    void backoffCapsAtThirtySeconds() {
        assertEquals(Duration.ofSeconds(16), ReconnectBudget.backoff(4));
        assertEquals(Duration.ofSeconds(30), ReconnectBudget.backoff(5));
        assertEquals(Duration.ofSeconds(30), ReconnectBudget.backoff(Integer.MAX_VALUE));
    }

    @Test
    void rotationRules() {
        assertEquals(new ReconnectBudget.Judgement(End.BLOCKED, true), ReconnectBudget.judge(Exit.DEVICE_BLOCKED, LONG));
        assertEquals(new ReconnectBudget.Judgement(End.FAILED, true), ReconnectBudget.judge(Exit.NO_TTWID, Duration.ZERO));
        assertEquals(new ReconnectBudget.Judgement(End.FAILED, true), ReconnectBudget.judge(Exit.ERRORED, SHORT));
        assertEquals(new ReconnectBudget.Judgement(End.HEALTHY, false), ReconnectBudget.judge(Exit.ERRORED, LONG));
        assertEquals(new ReconnectBudget.Judgement(End.FAILED, false), ReconnectBudget.judge(Exit.CLOSED, SHORT));
        assertEquals(new ReconnectBudget.Judgement(End.HEALTHY, false), ReconnectBudget.judge(Exit.CLOSED, LONG));
    }
}
