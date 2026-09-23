package com.otectus.runic_races.common.state;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.diagnostics.RRMetrics;
import com.otectus.runic_races.flight.FlightServerHandler;
import com.otectus.runic_races.network.NetworkHandler;
import com.otectus.runic_races.network.S2CAdaptationStacksPacket;
import com.otectus.runic_races.network.S2CRaceStatePacket;
import com.otectus.runic_races.notification.RaceNotificationService;
import com.otectus.runic_races.presentation.ProcDebounce;
import com.otectus.runic_races.presentation.RunicPresentation;
import com.otectus.runic_races.presentation.SignatureKey;
import com.otectus.runic_races.presentation.WeaknessCueRegistry;
import com.otectus.runic_races.util.RaceHelper;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side transient store of {@link RaceStateFlags} per player. Not persisted —
 * power ticks and event handlers re-compute the flags on each relevant frame.
 *
 * Two kinds of writer feed a player's state:
 * <ul>
 *   <li>{@link #setFlag} — single-writer flags owned by the per-player state watcher.</li>
 *   <li>{@link #setContribution} — flags several powers can write at once (two biome
 *       affinities on one race both report {@link RaceStateFlags#BIOME_HOME}). Each source
 *       keeps its own bits and the flag is the OR of its live sources, so one source
 *       clearing the bit no longer erases another's.</li>
 * </ul>
 * Writes only update logical state. At the end of the server tick every changed player
 * gets at most one {@link S2CRaceStatePacket}, and notifications/onset cues fire from the
 * difference between that final state and the last one delivered — intermediate
 * setter calls within a tick never reach the client.
 *
 * Clears on logout so stale UUIDs don't accumulate across long-running servers.
 */
@Mod.EventBusSubscriber(modid = RunicRacesMod.MOD_ID)
public final class RaceStateTracker {

    private static final class State {
        /** Direct flags plus per-power contributions; tracks what was last delivered. */
        final FlagAggregate<ResourceLocation> flags = new FlagAggregate<>();
        /** Entity to deliver the next flush to; released after each flush. */
        @Nullable ServerPlayer target;
    }

    // Accessed only on the server thread (player ticks, events) — plain maps avoid
    // ConcurrentHashMap's lock-striping overhead.
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final Map<UUID, State> DIRTY = new LinkedHashMap<>();
    private static boolean validateSourcesAfterReload = false;

    private RaceStateTracker() {}

    /** Returns the current logical flag bitfield for the player (0 if untracked). */
    public static int get(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state == null ? 0 : state.flags.effective();
    }

    /** Sets a single-writer flag on or off; the client hears about it at the end of the tick. */
    public static void setFlag(ServerPlayer player, RaceStateFlags flag, boolean on) {
        State state = on ? state(player) : STATES.get(player.getUUID());
        if (state != null && state.flags.setDirect(flag.mask(), on)) {
            markDirty(player, state);
        }
    }

    /**
     * Records {@code source}'s view of the flags in {@code mask}: bits of {@code mask} set in
     * {@code bits} are on for this source, the rest are off. Other sources are untouched.
     */
    public static void setContribution(ServerPlayer player, ResourceLocation source, int mask, int bits) {
        State state = (bits & mask) != 0 ? state(player) : STATES.get(player.getUUID());
        if (state != null && state.flags.setContribution(source, mask, bits)) {
            markDirty(player, state);
        }
    }

    /** Drops everything {@code source} contributed (the power was removed). */
    public static void clearContribution(ServerPlayer player, ResourceLocation source) {
        State state = STATES.get(player.getUUID());
        if (state != null && state.flags.clearContribution(source)) {
            markDirty(player, state);
        }
    }

    /** Drops all flags (race change); resyncs so the client mirror clears too. No notifications. */
    public static void clear(ServerPlayer player) {
        UUID id = player.getUUID();
        DIRTY.remove(id);
        if (STATES.remove(id) != null) {
            NetworkHandler.sendToPlayer(player, new S2CRaceStatePacket(0));
            RRMetrics.add(RRMetrics.Counter.RACE_STATE_PACKETS);
        }
    }

    /**
     * Re-delivers the last flushed state (login, respawn, dimension change). Pending
     * transitions are left for the end-of-tick flush so their notifications still fire.
     */
    public static void resync(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        NetworkHandler.sendToPlayer(player, new S2CRaceStatePacket(state == null ? 0 : state.flags.delivered()));
        RRMetrics.add(RRMetrics.Counter.RACE_STATE_PACKETS);
    }

    /** Players with tracked state (diagnostics). */
    public static int trackedCount() {
        return STATES.size();
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    private static void markDirty(ServerPlayer player, State state) {
        state.target = player;
        DIRTY.put(player.getUUID(), state);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (validateSourcesAfterReload) {
            validateSourcesAfterReload = false;
            dropRemovedSources(event.getServer());
        }
        if (DIRTY.isEmpty()) return;
        List<State> pending = new ArrayList<>(DIRTY.values());
        DIRTY.clear();
        for (State state : pending) {
            ServerPlayer player = state.target;
            state.target = null;
            if (player == null || STATES.get(player.getUUID()) != state) continue;
            flush(player, state);
        }
    }

    private static void flush(ServerPlayer player, State state) {
        int changed = state.flags.takeTransitions();
        if (changed == 0) return;
        int current = state.flags.delivered();
        NetworkHandler.sendToPlayer(player, new S2CRaceStatePacket(current));
        RRMetrics.add(RRMetrics.Counter.RACE_STATE_PACKETS);
        String race = null;
        for (RaceStateFlags flag : RaceStateFlags.values()) {
            if ((changed & flag.mask()) == 0) continue;
            boolean on = flag.isSet(current);
            RRMetrics.add(RRMetrics.Counter.RACE_STATE_TRANSITIONS);
            RaceNotificationService.onFlagTransition(player, flag, on);
            if (on) {
                // Sensory cue for the weakness starting to bite (words stay with the
                // notification above). Debounced so flicker at a boundary (water
                // surface, cave mouth) reads as one moment.
                if (race == null) race = RaceHelper.getRaceName(player).orElse("");
                SignatureKey cue = WeaknessCueRegistry.onsetCue(race, flag);
                if (cue != null) {
                    RunicPresentation.fireProc(player, cue, 100);
                }
            }
        }
    }

    /** After a datapack reload, forget contributions from powers the player no longer holds. */
    private static void dropRemovedSources(MinecraftServer server) {
        for (Map.Entry<UUID, State> entry : STATES.entrySet()) {
            State state = entry.getValue();
            if (!state.flags.hasContributions()) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            IPowerContainer container = player == null ? null : IPowerContainer.get(player).orElse(null);
            if (container == null) continue;
            if (state.flags.retainSources(container::hasPower)) {
                markDirty(player, state);
            }
        }
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        // A null player means a /reload, not a joining player.
        if (event.getPlayer() == null) validateSourcesAfterReload = true;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        STATES.remove(id);
        DIRTY.remove(id);
        RaceHelper.invalidate(id);
        FlightServerHandler.onLogout(id);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RaceHelper.invalidate(player.getUUID());
            resync(player);
            // The adaptation stack mirror is client-side; drop the "already synced"
            // marker and push the live count so a relog starts from the truth.
            var data = player.getPersistentData();
            data.remove("runic_races:human_adapt_synced_stacks");
            NetworkHandler.sendToPlayer(player,
                    new S2CAdaptationStacksPacket(data.getInt("runic_races:human_adapt_stacks")));
        }
    }

    /** The player entity was replaced (respawn, End return): the new entity starts from the last delivered state. */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RaceHelper.invalidate(player.getUUID());
            resync(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RaceHelper.invalidate(player.getUUID());
            resync(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        STATES.clear();
        DIRTY.clear();
        validateSourcesAfterReload = false;
        ProcDebounce.clearAll();
        RaceHelper.clearAll();
        FlightServerHandler.clearAll();
    }
}
