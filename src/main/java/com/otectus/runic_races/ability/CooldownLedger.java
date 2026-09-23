package com.otectus.runic_races.ability;

import java.util.*;

/** Remaining simulation ticks: selecting another race advances time, disconnecting does not. */
public final class CooldownLedger extends EnumMap<AbilityKind, Integer> {
    public CooldownLedger() { super(AbilityKind.class); }
    public void tick() { replaceAll((k, v) -> Math.max(0, v - 1)); values().removeIf(v -> v <= 0); }
    public Map<String, Integer> save() {
        Map<String, Integer> saved = new LinkedHashMap<>();
        forEach((k, v) -> { if (v > 0) saved.put(k.cooldownId(), v); });
        return Collections.unmodifiableMap(saved);
    }
    public void restore(Map<String, Integer> saved) {
        clear();
        for (AbilityKind kind : AbilityKind.values()) {
            int ticks = Math.max(0, Math.min(72000, saved.getOrDefault(kind.cooldownId(), 0)));
            if (ticks > 0) put(kind, ticks);
        }
    }
}
