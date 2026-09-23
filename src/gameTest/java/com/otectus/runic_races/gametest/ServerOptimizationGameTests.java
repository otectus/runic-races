package com.otectus.runic_races.gametest;

import com.mojang.authlib.GameProfile;
import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.action.CooldownDecayAction;
import com.otectus.runic_races.common.state.RaceStateFlags;
import com.otectus.runic_races.common.state.RaceStateTracker;
import com.otectus.runic_races.config.RRServerConfig;
import com.otectus.runic_races.diagnostics.RRMetrics;
import com.otectus.runic_races.event.RacialEventHandler;
import com.otectus.runic_races.integration.IntegrationManager;
import com.otectus.runic_races.network.CooldownSync;
import com.otectus.runic_races.network.S2CParticleBatchPacket;
import com.otectus.runic_races.network.S2CPowerDataPacket;
import com.otectus.runic_races.power.BiomeAffinityPower;
import com.otectus.runic_races.power.ScalingAttributePower;
import com.otectus.runic_races.presentation.PresentationScheduler;
import com.otectus.runic_races.presentation.RunicPresentation;
import com.otectus.runic_races.presentation.SignatureKey;
import com.otectus.runic_races.presentation.SignatureRegistry;
import com.otectus.runic_races.race.RaceRegistry;
import com.otectus.runic_races.registry.ModEntityActions;
import com.otectus.runic_races.util.OriginsPowerHelper;
import com.otectus.runic_races.util.RaceHelper;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.registry.OriginsDynamicRegistries;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Server optimization regressions (1.7.2): cooldown decay transport, batched particles,
 * race-state aggregation, integration activation and lifecycle cleanup, measured against
 * real Forge, Origins and Apoli. Tests that add recording players to the level run in
 * their own batch so no other test's mobs or recipients see them.
 */
@GameTestHolder("runic_races")
@PrefixGameTestTemplate(false)
public final class ServerOptimizationGameTests {

    private static final ResourceLocation RR_CHANNEL = new ResourceLocation(RunicRacesMod.MOD_ID, "main");
    // Registration indices pinned by NetworkAppendOnlyTest.
    private static final int POWER_DATA_INDEX = 9;
    private static final int PARTICLE_BATCH_INDEX = 10;

    /** A server player whose connection records every packet instead of sending it. */
    private static final class Recorder extends ServerPlayer {
        final List<Packet<?>> packets = new ArrayList<>();

        Recorder(ServerLevel level, String name) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), name));
            connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), this) {
                @Override public void send(Packet<?> packet) { packets.add(packet); }
                @Override public void send(Packet<?> packet, PacketSendListener listener) { packets.add(packet); }
            };
        }
    }

    private static Recorder player(GameTestHelper h, String race, Vec3 relative) {
        Recorder p = new Recorder(h.getLevel(), "RR_" + race.substring(0, Math.min(12, race.length())));
        Vec3 at = h.absoluteVec(relative);
        p.setPos(at.x, at.y, at.z);
        IOriginContainer origins = IOriginContainer.get(p).orElseThrow(() -> new AssertionError("Origins capability absent"));
        origins.setOrigin(RaceHelper.FAMILY_LAYER, ResourceKey.create(OriginsDynamicRegistries.ORIGINS_REGISTRY,
                new ResourceLocation(RunicRacesMod.MOD_ID, "family_" + RaceRegistry.getFamily(race))));
        origins.setOrigin(RaceHelper.RACE_LAYER, ResourceKey.create(OriginsDynamicRegistries.ORIGINS_REGISTRY,
                new ResourceLocation(RunicRacesMod.MOD_ID, race)));
        origins.tick();
        RaceHelper.invalidate(p.getUUID());
        return p;
    }

    private static ConfiguredPower<?, ?> configuredPower(ServerPlayer player, String path) {
        Holder<ConfiguredPower<?, ?>> holder = holder(player, path);
        return holder.value();
    }

    @SuppressWarnings("unchecked")
    private static Holder<ConfiguredPower<?, ?>> holder(ServerPlayer player, String path) {
        IPowerContainer container = IPowerContainer.get(player).orElseThrow(() -> new AssertionError("Apoli capability absent"));
        var holder = container.getPower(new ResourceLocation(RunicRacesMod.MOD_ID, path));
        if (holder == null || !holder.isBound()) throw new AssertionError("Missing configured power: " + path);
        return (Holder<ConfiguredPower<?, ?>>) (Holder<?>) holder;
    }

    private static void join(GameTestHelper h, Recorder... players) {
        for (Recorder p : players) h.getLevel().addNewPlayer(p);
    }

    private static void leave(GameTestHelper h, Recorder... players) {
        for (Recorder p : players) {
            PresentationScheduler.cancel(p.getUUID());
            RaceStateTracker.clear(p);
            if (!p.isRemoved()) h.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void clearRecorded(Recorder... players) {
        for (Recorder p : players) p.packets.clear();
    }

    /** Packet id, body and uncompressed length prefix: what the connection frames. */
    private static int wireBytes(Packet<?> packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeVarInt(ConnectionProtocol.PLAY.getPacketId(PacketFlow.CLIENTBOUND, packet));
            packet.write(buffer);
            int body = buffer.readableBytes();
            return body + FriendlyByteBuf.getVarIntSize(body);
        } finally {
            buffer.release();
        }
    }

    /** The RR message index of a recorded payload, or -1. */
    private static int rrIndex(Packet<?> packet) {
        if (!(packet instanceof ClientboundCustomPayloadPacket payload) || !RR_CHANNEL.equals(payload.getIdentifier())) return -1;
        FriendlyByteBuf data = payload.getData();
        return data.getUnsignedByte(data.readerIndex());
    }

    private static FriendlyByteBuf rrBody(Packet<?> packet) {
        FriendlyByteBuf data = new FriendlyByteBuf(((ClientboundCustomPayloadPacket) packet).getData().slice());
        data.readUnsignedByte();
        return data;
    }

    private static boolean isApoli(Packet<?> packet) {
        return packet instanceof ClientboundCustomPayloadPacket payload
                && "apoli".equals(payload.getIdentifier().getNamespace());
    }

    // ==================== COOLDOWN DECAY (R3) ====================

    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 200)
    public static void everyRacialCooldownTimerTakesTheDeltaPath(GameTestHelper h) {
        int timers = 0;
        int decaying = 0;
        for (String race : RaceRegistry.allRaceNames()) {
            Recorder p = player(h, race, new Vec3(2.5, 2, 2.5));
            IPowerContainer container = IPowerContainer.get(p).orElseThrow(() -> new AssertionError("Apoli capability absent"));
            for (var held : container.getPowers()) {
                ResourceLocation id = held.unwrapKey().map(ResourceKey::location).orElse(null);
                if (id == null || !held.isBound() || !RunicRacesMod.MOD_ID.equals(id.getNamespace())) continue;
                if (id.getPath().endsWith("_cooldown_timer")) {
                    timers++;
                    h.assertTrue(CooldownSync.isDeltaSafe(held.value()),
                            "Cooldown timer would fall back to a full container sync: " + id);
                } else if (id.getPath().endsWith("_cooldown_decay")) {
                    decaying++;
                }
            }
            RaceStateTracker.clear(p);
        }
        h.assertTrue(timers == 60 && decaying == 43,
                "Expected 60 cooldown timers (43 decaying), found " + timers + " (" + decaying + ")");
        h.succeed();
    }

    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 120, batch = "rr_transport")
    public static void cooldownDecayTrafficAndValues(GameTestHelper h) {
        Recorder caster = player(h, "fire_drake", new Vec3(2.5, 2, 2.5));
        Recorder[] observers = {
                player(h, "primian", new Vec3(5.5, 2, 2.5)),
                player(h, "primian", new Vec3(2.5, 2, 5.5)),
                player(h, "primian", new Vec3(5.5, 2, 5.5))};
        Recorder[] all = {caster, observers[0], observers[1], observers[2]};
        join(h, all);
        String timerPath = "fire_drake/dragonfire_breath_cooldown_timer";
        ResourceLocation timerId = new ResourceLocation(RunicRacesMod.MOD_ID, timerPath);
        ConfiguredPower<?, ?> timer = configuredPower(caster, timerPath);
        CooldownDecayAction action = ModEntityActions.COOLDOWN_DECAY.get();
        CooldownDecayAction.Configuration step = new CooldownDecayAction.Configuration(holder(caster, timerPath), -10);
        // Server config is never toggled here: ConfigValue.set autosaves the TOML and races
        // Forge's file watcher. The legacy step is emulated exactly instead.
        h.assertTrue(RRServerConfig.COOLDOWN_DELTA_SYNC.get(), "Test world must use the default network.cooldownDeltaSync");
        long[] legacy = new long[2];
        long[] delta = new long[2];
        long[] fullSyncsBefore = new long[1];

        // Phase A: what origins:change_resource did — the same change, then Apoli's full sync.
        h.runAfterDelay(1, () -> {
            OriginsPowerHelper.setResourceValue(caster, timerId, 800);
            clearRecorded(all);
        });
        for (int i = 0; i < 10; i++) h.runAfterDelay(2 + i, () -> {
            timer.change(caster, -10);
            io.github.edwinmindcraft.apoli.api.ApoliAPI.synchronizePowerContainer(caster);
        });
        h.runAfterDelay(13, () -> {
            for (Recorder r : all) for (Packet<?> packet : r.packets) if (isApoli(packet)) {
                legacy[0]++;
                legacy[1] += wireBytes(packet);
            }
            h.assertTrue(timer.getValue(caster).orElse(-1) == 700, "Legacy decay did not step 800 -> 700");
            // Phase B: runic_races:cooldown_decay with resource deltas.
            OriginsPowerHelper.setResourceValue(caster, timerId, 800);
            clearRecorded(all);
            fullSyncsBefore[0] = RRMetrics.get(RRMetrics.Counter.COOLDOWN_FULL_SYNCS);
        });
        for (int i = 0; i < 10; i++) h.runAfterDelay(14 + i, () -> action.execute(step, caster));
        h.runAfterDelay(26, () -> {
            int lastValue = -1;
            for (Recorder r : all) {
                for (Packet<?> packet : r.packets) {
                    if (rrIndex(packet) != POWER_DATA_INDEX) continue;
                    delta[0]++;
                    delta[1] += wireBytes(packet);
                    S2CPowerDataPacket decoded = S2CPowerDataPacket.decode(rrBody(packet));
                    h.assertTrue(decoded.entityId() == caster.getId(), "Delta addressed the wrong entity");
                    lastValue = decoded.entries().get(0).data().getInt("Value");
                }
            }
            h.assertTrue(timer.getValue(caster).orElse(-1) == 700, "Delta decay did not step 800 -> 700");
            h.assertTrue(lastValue == 700, "Clients were last told " + lastValue + ", not the authoritative 700");
            h.assertTrue(RRMetrics.get(RRMetrics.Counter.COOLDOWN_FULL_SYNCS) == fullSyncsBefore[0],
                    "A delta-safe decay step still forced a full container sync");
            h.assertTrue(legacy[0] == 10L * all.length && delta[0] == 10L * all.length,
                    "Expected one update per step per recipient in both modes: legacy=" + legacy[0] + " delta=" + delta[0]);
            h.assertTrue(delta[1] * 5 <= legacy[1],
                    "Cooldown bytes fell less than 80%: legacy=" + legacy[1] + " delta=" + delta[1]);
            RunicRacesMod.LOGGER.info("[RunicRaces][measure] cooldown decay, 10 steps x {} recipients: legacy {} packets / {} bytes, delta {} packets / {} bytes ({}% fewer bytes)",
                    all.length, legacy[0], legacy[1], delta[0], delta[1],
                    String.format(Locale.ROOT, "%.1f", 100.0 * (legacy[1] - delta[1]) / legacy[1]));
            // Same-tick ordering: a decay followed by an activation must leave clients on the activation.
            clearRecorded(all);
            action.execute(step, caster);
            OriginsPowerHelper.setResourceValue(caster, timerId, 800);
        });
        h.runAfterDelay(28, () -> {
            try {
                int told = -1;
                for (Packet<?> packet : caster.packets) {
                    if (rrIndex(packet) == POWER_DATA_INDEX) {
                        told = S2CPowerDataPacket.decode(rrBody(packet)).entries().get(0).data().getInt("Value");
                    }
                }
                h.assertTrue(told == 800, "A same-tick activation was overwritten by an older decay value: " + told);
            } finally {
                leave(h, all);
            }
            h.succeed();
        });
    }

    // ==================== BATCHED PARTICLES (R4) ====================

    private record Emitted(String type, boolean force, double x, double y, double z, String motion) {}

    private static List<Emitted> particles(Recorder recorder, long[] totals) {
        List<Emitted> out = new ArrayList<>();
        for (Packet<?> packet : recorder.packets) {
            if (packet instanceof ClientboundLevelParticlesPacket vanilla) {
                totals[0]++;
                totals[1] += wireBytes(packet);
                String motion = vanilla.getCount() == 0
                        ? bits(vanilla.getMaxSpeed() * vanilla.getXDist(), vanilla.getMaxSpeed() * vanilla.getYDist(),
                        vanilla.getMaxSpeed() * vanilla.getZDist())
                        : "n" + vanilla.getCount() + ":" + bits(vanilla.getXDist(), vanilla.getYDist(), vanilla.getZDist()) + ":" + bits(vanilla.getMaxSpeed());
                out.add(new Emitted(type(vanilla.getParticle()), vanilla.isOverrideLimiter(),
                        vanilla.getX(), vanilla.getY(), vanilla.getZ(), motion));
            } else if (rrIndex(packet) == PARTICLE_BATCH_INDEX) {
                totals[0]++;
                totals[1] += wireBytes(packet);
                FriendlyByteBuf body = rrBody(packet);
                // Re-read the particle header the way the decoder does, then the batch.
                S2CParticleBatchPacket batch = S2CParticleBatchPacket.decode(new FriendlyByteBuf(body.copy()));
                ParticleOptions options = readOptions(body);
                boolean force = body.readBoolean();
                for (int i = 0; i < batch.count(); i++) {
                    String motion = batch.mode() == S2CParticleBatchPacket.MODE_JITTER
                            ? "n1:" + bits(batch.motion(i, 0), batch.motion(i, 1), batch.motion(i, 2)) + ":" + bits(batch.motion(i, 3))
                            : bits(batch.motion(i, 0), batch.motion(i, 1), batch.motion(i, 2));
                    out.add(new Emitted(type(options), force, batch.x(i), batch.y(i), batch.z(i), motion));
                }
            }
        }
        return out;
    }

    private static ParticleOptions readOptions(FriendlyByteBuf buffer) {
        var type = buffer.readById(BuiltInRegistries.PARTICLE_TYPE);
        return read(buffer, type);
    }

    private static <T extends ParticleOptions> T read(FriendlyByteBuf buffer, net.minecraft.core.particles.ParticleType<T> type) {
        return type.getDeserializer().fromNetwork(type, buffer);
    }

    private static String type(ParticleOptions options) {
        return BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType()) + "|" + options.writeToString();
    }

    private static String bits(float... values) {
        StringBuilder sb = new StringBuilder();
        for (float v : values) sb.append(Integer.toHexString(Float.floatToIntBits(v))).append(',');
        return sb.toString();
    }

    /** Same particles, recipients and motion; positions within float transport precision. */
    private static String mismatch(List<Emitted> legacy, List<Emitted> batched) {
        if (legacy.size() != batched.size()) return "count " + legacy.size() + " vs " + batched.size();
        boolean[] used = new boolean[batched.size()];
        outer:
        for (Emitted expected : legacy) {
            for (int j = 0; j < batched.size(); j++) {
                Emitted actual = batched.get(j);
                if (!used[j] && actual.type().equals(expected.type()) && actual.force() == expected.force()
                        && actual.motion().equals(expected.motion())
                        && Math.abs(actual.x() - expected.x()) < 1.0e-4
                        && Math.abs(actual.y() - expected.y()) < 1.0e-4
                        && Math.abs(actual.z() - expected.z()) < 1.0e-4) {
                    used[j] = true;
                    continue outer;
                }
            }
            return "no batched match for " + expected;
        }
        return null;
    }

    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 100, batch = "rr_transport")
    public static void batchedSignatureParticlesMatchLegacyDelivery(GameTestHelper h) {
        Recorder caster = player(h, "arachnid", new Vec3(2.5, 2, 2.5));
        Recorder near = player(h, "primian", new Vec3(6.5, 2, 3.5));
        // On the 32-block fringe: some points of every wide shape land on each side of it.
        Recorder fringe = player(h, "primian", new Vec3(2.5 + 31.6, 2, 2.5));
        Recorder[] all = {caster, near, fringe};
        join(h, all);
        h.runAfterDelay(2, () -> {
            long[] legacyTotals = new long[2];
            long[] batchedTotals = new long[2];
            int compared = 0;
            try {
                for (SignatureKey key : SignatureKey.values()) {
                    if (SignatureRegistry.get(key) == null) continue;
                    Vec3 lineTarget = caster.position().add(3, 1, 1);
                    List<List<Emitted>> runs = new ArrayList<>();
                    for (boolean batched : new boolean[]{false, true}) {
                        com.otectus.runic_races.presentation.ParticleBatch.overrideTransport(batched);
                        clearRecorded(all);
                        // CONE samples server randomness: replay the same draws in both runs.
                        h.getLevel().random.setSeed(0x5EEDL + key.ordinal());
                        RunicPresentation.fire(caster, key, lineTarget);
                        PresentationScheduler.cancel(caster.getUUID());
                        for (Recorder r : all) runs.add(particles(r, batched ? batchedTotals : legacyTotals));
                    }
                    for (int i = 0; i < all.length; i++) {
                        String problem = mismatch(runs.get(i), runs.get(i + all.length));
                        h.assertTrue(problem == null, key + " delivered different particles to recipient " + i + ": " + problem);
                    }
                    compared++;
                }
            } finally {
                com.otectus.runic_races.presentation.ParticleBatch.overrideTransport(null);
                leave(h, all);
            }
            h.assertTrue(compared > 50, "Only " + compared + " signature recipes were compared");
            h.assertTrue(batchedTotals[0] * 5 <= legacyTotals[0],
                    "Particle messages fell less than 80%: legacy=" + legacyTotals[0] + " batched=" + batchedTotals[0]);
            h.assertTrue(batchedTotals[1] * 2 <= legacyTotals[1],
                    "Particle bytes fell less than 50%: legacy=" + legacyTotals[1] + " batched=" + batchedTotals[1]);
            RunicRacesMod.LOGGER.info("[RunicRaces][measure] {} signature recipes x 3 recipients (one on the 32-block fringe): legacy {} packets / {} bytes, batched {} packets / {} bytes",
                    compared, legacyTotals[0], legacyTotals[1], batchedTotals[0], batchedTotals[1]);
            h.succeed();
        });
    }

    // ==================== STATE AGGREGATION + CONFIG DERIVATION (R1, R5) ====================

    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void canineAffinitiesAreSeparateSourcesWithStableModifierIds(GameTestHelper h) {
        Recorder p = player(h, "canine", new Vec3(2.5, 2, 2.5));
        ConfiguredPower<?, ?> forest = configuredPower(p, "canine/pack_hunter_forest_home");
        ConfiguredPower<?, ?> taiga = configuredPower(p, "canine/pack_hunter_taiga_home");
        h.assertTrue(!forest.getRegistryName().equals(taiga.getRegistryName()), "Both affinities share one flag source");
        ConfiguredPower<?, ?>[] affinities = new ConfiguredPower<?, ?>[]{forest, taiga};
        String[] tags = {"minecraft:is_forest", "minecraft:is_taiga"};
        for (int a = 0; a < affinities.length; a++) {
            var config = (BiomeAffinityPower.Configuration) affinities[a].getConfiguration();
            for (String role : List.of("home_speed", "home_damage", "hostile_speed", "hostile_damage")) {
                String legacyKey = "runic_races:biome_affinity:" + role + ":" + tags[a] + ":" + ""
                        + ":" + 0.06 + ":" + 0.05 + ":" + 0.0 + ":" + 0.0;
                UUID legacy = UUID.nameUUIDFromBytes(legacyKey.getBytes(StandardCharsets.UTF_8));
                h.assertTrue(config.modifierUuid(role).equals(legacy), "Modifier identity changed for " + role);
            }
            var roles = config.derived().roles();
            h.assertTrue(roles.size() == 2 && roles.get(0).uuid().equals(config.modifierUuid("home_speed"))
                            && roles.get(1).uuid().equals(config.modifierUuid("home_damage")),
                    "Derived roles do not match the configured non-zero home roles: " + roles);
        }
        // Standing in a forest that is not a taiga: the two powers disagree every interval.
        int affinity = RaceStateFlags.of(RaceStateFlags.BIOME_HOME, RaceStateFlags.BIOME_HOSTILE);
        for (int i = 0; i < 20; i++) {
            RaceStateTracker.setContribution(p, i % 2 == 0 ? forest.getRegistryName() : taiga.getRegistryName(), affinity,
                    i % 2 == 0 ? RaceStateFlags.BIOME_HOME.mask() : 0);
            h.assertTrue(RaceStateFlags.BIOME_HOME.isSet(RaceStateTracker.get(p)), "A disagreeing affinity erased BIOME_HOME");
        }
        RaceStateTracker.clearContribution(p, forest.getRegistryName());
        h.assertTrue(!RaceStateFlags.BIOME_HOME.isSet(RaceStateTracker.get(p)), "Removed source still holds BIOME_HOME");

        Recorder elf = player(h, "dark_elf", new Vec3(4.5, 2, 2.5));
        var scaling = (ScalingAttributePower.Configuration) configuredPower(elf, "dark_elf/children_of_darkness_night_power").getConfiguration();
        String scalingKey = "runic_races:scaling:generic.attack_damage:multiply_total:" + (-0.05) + ":" + 0.1;
        h.assertTrue(scaling.modifierUuid().equals(UUID.nameUUIDFromBytes(scalingKey.getBytes(StandardCharsets.UTF_8))),
                "Scaling modifier identity changed");
        RaceStateTracker.clear(p);
        RaceStateTracker.clear(elf);
        h.succeed();
    }

    // ==================== LIFECYCLE (R2) ====================

    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void clearingTheRaceRemovesTheAdaptationModifier(GameTestHelper h) {
        UUID adaptation = UUID.fromString("a7b3c8d5-3456-6789-abcd-ef0123456789");
        Recorder p = player(h, "primian", new Vec3(2.5, 2, 2.5));
        p.getPersistentData().putInt("runic_races:human_adapt_stacks", 3);
        p.getPersistentData().putLong("runic_races:human_adapt_last_tick", h.getLevel().getGameTime());
        RacialEventHandler handler = new RacialEventHandler();
        p.tickCount = 10;
        handler.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, p));
        var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        h.assertTrue(speed.getModifier(adaptation) != null, "Primian adaptation modifier was not applied");

        IOriginContainer origins = IOriginContainer.get(p).orElseThrow(() -> new AssertionError("Origins capability absent"));
        origins.setOrigin(RaceHelper.RACE_LAYER, RaceHelper.EMPTY_ORIGIN);
        origins.tick();
        RaceHelper.invalidate(p.getUUID());
        h.assertTrue(RaceHelper.getRaceName(p).isEmpty(), "Race layer was not cleared");
        p.tickCount = 20;
        handler.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, p));
        h.assertTrue(speed.getModifier(adaptation) == null, "Adaptation speed survived clearing the race");
        RaceStateTracker.clear(p);
        h.succeed();
    }

    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 40, batch = "rr_transport")
    public static void integrationActivationFollowsTheLiveConfig(GameTestHelper h) {
        // Read-only: writing a ConfigValue autosaves the TOML and races Forge's file watcher.
        if (net.minecraftforge.fml.ModList.get().isLoaded("curios")) {
            h.assertTrue(IntegrationManager.getLoadedIntegrations().stream().anyMatch(i -> i.getName().equals("Curios")),
                    "Curios adapter missing although the mod is present");
            h.assertTrue(IntegrationManager.isIntegrationActive("Curios") == RRServerConfig.CURIOS_INTEGRATION.get(),
                    "Curios activation does not follow its live config toggle");
        } else {
            h.assertTrue(!IntegrationManager.isIntegrationActive("Curios"), "Absent Curios reported active");
        }
        h.succeed();
    }
}
