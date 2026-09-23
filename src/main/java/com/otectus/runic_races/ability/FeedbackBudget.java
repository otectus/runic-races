package com.otectus.runic_races.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Limits execution feedback even when many targets or damage callbacks share a tick. */
final class FeedbackBudget {
    private final int limit;
    private final Map<UUID, Integer> spent = new HashMap<>();
    private long tick = Long.MIN_VALUE;

    FeedbackBudget(int limit) { this.limit = Math.max(0, limit); }

    int reserve(UUID owner, long now, int requested) {
        if (tick != now) { spent.clear(); tick = now; }
        int used = spent.getOrDefault(owner, 0);
        int count = Math.max(0, Math.min(requested, limit - used));
        if (count > 0) spent.put(owner, used + count);
        return count;
    }

    void clear() { spent.clear(); tick = Long.MIN_VALUE; }
}
