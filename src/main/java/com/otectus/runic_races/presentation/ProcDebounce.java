package com.otectus.runic_races.presentation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player, per-channel cooldown for cosmetic proc cues, so an event that
 * fires in bursts (mob combos, rapid ticks) reads as one moment instead of
 * spam. Transient by design — procs are cosmetic, so losing stamps on relog
 * is fine and eviction on logout is free.
 *
 * Plain HashMap: touched only from the server thread, like the rest of the
 * presentation layer. Registered on the Forge event bus in {@code RunicRacesMod}.
 */
public final class ProcDebounce {

    private static final Map<UUID, Map<String, Long>> LAST_FIRED = new HashMap<>();

    /**
     * True if {@code channel} is off cooldown for this player, stamping it when
     * acquired. Callers check their gating conditions first and only acquire
     * when they intend to fire.
     */
    public static boolean tryAcquire(ServerPlayer player, String channel, int cooldownTicks) {
        long now = player.serverLevel().getGameTime();
        Map<String, Long> channels = LAST_FIRED.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        Long last = channels.get(channel);
        if (last != null && now - last < cooldownTicks) {
            return false;
        }
        channels.put(channel, now);
        return true;
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_FIRED.remove(event.getEntity().getUUID());
    }
}
