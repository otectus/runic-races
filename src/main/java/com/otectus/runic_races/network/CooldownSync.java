package com.otectus.runic_races.network;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.config.RRServerConfig;
import com.otectus.runic_races.diagnostics.RRMetrics;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredEntityAction;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.common.power.ResourcePower;
import io.github.edwinmindcraft.apoli.common.power.configuration.ResourceConfiguration;
import io.github.edwinmindcraft.apoli.common.registry.action.ApoliEntityActions;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Synchronization boundary for Runic Races cooldown decay ({@code runic_races:cooldown_decay}).
 * <p>
 * Apoli's {@code change_resource} re-sends the owner's entire power container — every
 * power, source and serialized datum — to the owner and every player tracking them after
 * each step. Here a step only marks the resource changed; at the end of the server tick
 * each entity with changes gets one {@link S2CPowerDataPacket} carrying the changed
 * resources' current data, sent to the same audience (tracking players and self).
 * <p>
 * The values are read at flush time, so an activation or command that changes the same
 * resource later in the tick is never overwritten on the client by an older value. A
 * resource whose change could run a min/max action (and so change other powers), or that
 * is not a plain Apoli resource, keeps the full-container sync, as does everything when
 * {@code network.cooldownDeltaSync} is off. Login, respawn, dimension change, activation
 * and new trackers are untouched: Apoli still hydrates those with the full container.
 */
@Mod.EventBusSubscriber(modid = RunicRacesMod.MOD_ID)
public final class CooldownSync {

    private static final class Pending {
        final LivingEntity entity;
        final List<ResourceKey<ConfiguredPower<?, ?>>> resources = new ArrayList<>(2);

        Pending(LivingEntity entity) {
            this.entity = entity;
        }
    }

    // Server thread only; emptied every tick, so it never outlives one tick's entities.
    private static final Map<Integer, Pending> PENDING = new LinkedHashMap<>();

    private CooldownSync() {}

    /** Records that {@code resource} changed on {@code entity}; replaces Apoli's immediate full sync. */
    public static void changed(LivingEntity entity, Holder<ConfiguredPower<?, ?>> resource) {
        if (entity.level().isClientSide()) return;
        RRMetrics.add(RRMetrics.Counter.COOLDOWN_DECAY_STEPS);
        Optional<ResourceKey<ConfiguredPower<?, ?>>> key = resource.unwrapKey();
        if (key.isEmpty() || !RRServerConfig.COOLDOWN_DELTA_SYNC.get() || !isDeltaSafe(resource.value())) {
            ApoliAPI.synchronizePowerContainer(entity);
            RRMetrics.add(RRMetrics.Counter.COOLDOWN_FULL_SYNCS);
            return;
        }
        Pending pending = PENDING.get(entity.getId());
        if (pending == null || pending.entity != entity) {
            pending = new Pending(entity);
            PENDING.put(entity.getId(), pending);
        }
        if (!pending.resources.contains(key.get())) {
            pending.resources.add(key.get());
        }
    }

    /**
     * A change to a plain Apoli resource whose min and max actions do nothing touches no
     * other power, so its own serialized data is the whole difference a full sync would carry.
     */
    public static boolean isDeltaSafe(ConfiguredPower<?, ?> power) {
        if (!(power.getFactory() instanceof ResourcePower)
                || !(power.getConfiguration() instanceof ResourceConfiguration config)) {
            return false;
        }
        return isNoOp(config.minAction()) && isNoOp(config.maxAction());
    }

    private static boolean isNoOp(Holder<ConfiguredEntityAction<?, ?>> action) {
        return !action.isBound() || action.value().getFactory() == ApoliEntityActions.NOTHING.get();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;
        List<Pending> batch = new ArrayList<>(PENDING.values());
        PENDING.clear();
        for (Pending pending : batch) {
            flush(pending);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void flush(Pending pending) {
        LivingEntity entity = pending.entity;
        // A replaced entity (respawn, removal) is hydrated by Apoli's own full sync.
        if (entity.isRemoved()) return;
        IPowerContainer container = IPowerContainer.get(entity).orElse(null);
        if (container == null) return;
        List<S2CPowerDataPacket.Entry> entries = new ArrayList<>(pending.resources.size());
        for (ResourceKey<ConfiguredPower<?, ?>> key : pending.resources) {
            if (!container.hasPower(key)) continue;
            Holder holder = container.getPower(key);
            if (holder == null || !holder.isBound()) continue;
            entries.add(new S2CPowerDataPacket.Entry(key.location(),
                    ((ConfiguredPower) holder.value()).serialize(container)));
        }
        if (entries.isEmpty()) return;
        NetworkHandler.sendToTrackingAndSelf(entity, new S2CPowerDataPacket(entity.getId(), entries));
        RRMetrics.add(RRMetrics.Counter.COOLDOWN_DELTA_PACKETS);
        RRMetrics.add(RRMetrics.Counter.COOLDOWN_DELTA_ENTRIES, entries.size());
    }

    public static int pendingCount() {
        return PENDING.size();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        PENDING.clear();
    }
}
