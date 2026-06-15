package com.otectus.runic_races.common.state;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.network.NetworkHandler;
import com.otectus.runic_races.network.S2CRaceStatePacket;
import com.otectus.runic_races.notification.RaceNotificationService;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side transient store of {@link RaceStateFlags} per player. Not persisted —
 * power ticks and event handlers re-compute the flags on each relevant frame.
 *
 * Any time a flag changes, an {@link S2CRaceStatePacket} is pushed to the owning
 * player so the HUD state-rune overlay updates without polling.
 *
 * Clears on logout so stale UUIDs don't accumulate across long-running servers.
 */
@Mod.EventBusSubscriber(modid = RunicRacesMod.MOD_ID)
public final class RaceStateTracker {

    // Accessed only on the server thread (player ticks, events) — a plain HashMap avoids
    // ConcurrentHashMap's lock-striping overhead.
    private static final Map<UUID, Integer> FLAGS = new HashMap<>();

    private RaceStateTracker() {}

    /** Returns the current flag bitfield for the player (0 if untracked). */
    public static int get(ServerPlayer player) {
        return FLAGS.getOrDefault(player.getUUID(), 0);
    }

    /** Sets a single flag on or off; syncs to the client only if the value changed. */
    public static void setFlag(ServerPlayer player, RaceStateFlags flag, boolean on) {
        UUID id = player.getUUID();
        Integer prev = FLAGS.get(id);
        int current = prev == null ? 0 : prev;
        int updated = flag.set(current, on);
        if (updated != current) {
            FLAGS.put(id, updated);
            NetworkHandler.sendToPlayer(player, new S2CRaceStatePacket(updated));
            // A single-bit set only changes the field on a real edge, so 'on' is the new value.
            RaceNotificationService.onFlagTransition(player, flag, on);
        }
    }

    /** Bulk-replace flags (diffs internally; only syncs on change). */
    public static void setAll(ServerPlayer player, int newFlags) {
        UUID id = player.getUUID();
        Integer prev = FLAGS.get(id);
        int current = prev == null ? 0 : prev;
        if (newFlags != current) {
            FLAGS.put(id, newFlags);
            NetworkHandler.sendToPlayer(player, new S2CRaceStatePacket(newFlags));
        }
    }

    /** Force a full resync to the client regardless of diff (login, dimension change). */
    public static void resync(ServerPlayer player) {
        NetworkHandler.sendToPlayer(player, new S2CRaceStatePacket(get(player)));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        FLAGS.remove(id);
        RaceHelper.invalidate(id);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            resync(player);
        }
    }
}
