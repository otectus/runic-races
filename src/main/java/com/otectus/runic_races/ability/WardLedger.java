package com.otectus.runic_races.ability;

import java.util.*;

/** Pure finite-budget policy, independent of the engine's damage event order. */
public final class WardLedger {
    public enum Kind { DAWN, PRISM, SHELL, DOMINION }
    public record Ward(Kind kind, UUID owner, long expires, float remaining, float fraction) {}
    public record Prevention(Ward ward, float amount) {}
    private final EnumMap<Kind, Ward> wards = new EnumMap<>(Kind.class);
    public void offer(Ward incoming, long now) {
        expire(now);
        Ward existing = wards.get(incoming.kind());
        // Same family replaces only for a stronger remaining budget; ties retain the later expiry.
        if (existing == null || incoming.remaining() > existing.remaining()
                || (incoming.remaining() == existing.remaining() && incoming.expires() > existing.expires()))
            wards.put(incoming.kind(), incoming);
    }
    public Optional<Prevention> select(float damage, long now, java.util.function.Predicate<Kind> eligible) {
        expire(now);
        if (!Float.isFinite(damage) || damage <= 0) return Optional.empty();
        Prevention best = null;
        for (Ward w : wards.values()) {
            if (!eligible.test(w.kind())) continue;
            float prevent = Math.min(w.remaining(), damage * w.fraction());
            if (prevent > 0 && (best == null || prevent > best.amount())) best = new Prevention(w, prevent);
        }
        return Optional.ofNullable(best);
    }
    public void consume(Prevention prevention) {
        Ward w = prevention.ward();
        if (wards.get(w.kind()) != w) return;
        float remaining = Math.max(0, w.remaining() - prevention.amount());
        if (remaining <= 0 || w.kind() == Kind.PRISM) wards.remove(w.kind());
        else wards.put(w.kind(), new Ward(w.kind(), w.owner(), w.expires(), remaining, w.fraction()));
    }
    public void expire(long now) { wards.values().removeIf(w -> w.expires() <= now); }
    public void remove(Kind kind) { wards.remove(kind); }
    public void removeOwner(UUID owner) { wards.values().removeIf(w -> w.owner().equals(owner)); }
    public float remaining(Kind kind) { Ward w = wards.get(kind); return w == null ? 0 : w.remaining(); }
    public boolean isEmpty() { return wards.isEmpty(); }
    public void clear() { wards.clear(); }
}
