package com.otectus.runic_races.presentation;

import com.otectus.runic_races.diagnostics.RRMetrics;
import com.otectus.runic_races.presentation.SignatureEntry.SfxSpec;
import com.otectus.runic_races.presentation.SignatureEntry.VfxSpec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import com.otectus.runic_races.util.RaceHelper;

/**
 * Runs the delayed beats of multi-beat signature recipes ({@code .delayed(n)} specs).
 *
 * Beats track the caster live: at fire time the player is re-resolved by UUID and the
 * beat plays at their *current* position and look vector — so a rise-then-implode stays
 * attached to a strafing caster, and a blink ability's settle beat lands at the
 * destination. Breath particles instead retain their cast origin/aim to match the
 * instantaneous hit cone. Missing/dead players, world changes and race changes
 * discard the beat. LINE targets are always frozen at fire time.
 *
 * Registered on the Forge event bus in {@code RunicRacesMod}.
 */
public final class PresentationScheduler {

    private record Pose(Vec3 origin, Vec3 look, Vec3 eyes) {}

    private record PendingBeat(@Nullable SfxSpec sfx, @Nullable VfxSpec vfx,
                               @Nullable Vec3 lineTarget, @Nullable Pose pose) {}

    private record OwnedBeat(UUID owner, ResourceLocation dimension, @Nullable ResourceLocation race, PendingBeat beat) {}

    private static final BeatQueue<OwnedBeat> QUEUE = new BeatQueue<>();

    public static void scheduleSfx(ServerPlayer player, SfxSpec spec) {
        QUEUE.schedule(player.getUUID(), spec.delayTicks(),
                owned(player, new PendingBeat(spec, null, null, null)));
        RRMetrics.add(RRMetrics.Counter.BEATS_SCHEDULED);
    }

    public static void scheduleVfx(ServerPlayer player, VfxSpec spec, @Nullable Vec3 lineTarget) {
        scheduleVfx(player, spec, lineTarget, false);
    }

    public static void scheduleVfx(ServerPlayer player, VfxSpec spec, @Nullable Vec3 lineTarget, boolean snapshot) {
        Pose pose = snapshot ? new Pose(player.position().add(0, 0.3, 0),
                player.getLookAngle(), player.getEyePosition()) : null;
        QUEUE.schedule(player.getUUID(), spec.delayTicks(),
                owned(player, new PendingBeat(null, spec, lineTarget, pose)));
        RRMetrics.add(RRMetrics.Counter.BEATS_SCHEDULED);
    }

    public static int queuedBeats() {
        return QUEUE.size();
    }

    /** Drops every pending beat of one player. */
    public static void cancel(UUID owner) {
        QUEUE.cancel(owner);
    }

    private static OwnedBeat owned(ServerPlayer player, PendingBeat beat) {
        return new OwnedBeat(player.getUUID(), player.level().dimension().location(),
                RaceHelper.getRaceId(player).orElse(null), beat);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || QUEUE.size() == 0) return;
        MinecraftServer server = event.getServer();
        for (OwnedBeat owned : QUEUE.tick()) {
            ServerPlayer player = server.getPlayerList().getPlayer(owned.owner());
            if (player == null || !player.isAlive() || !(player.level() instanceof ServerLevel level)) continue;
            if (!level.dimension().location().equals(owned.dimension())
                    || !Objects.equals(RaceHelper.getRaceId(player).orElse(null), owned.race())) continue;
            PendingBeat beat = owned.beat();
            RRMetrics.add(RRMetrics.Counter.BEATS_DELIVERED);
            if (beat.sfx() != null) {
                RunicPresentation.playOneSfx(level, player.position(), beat.sfx());
            }
            if (beat.vfx() != null) {
                Pose pose = beat.pose() == null ? new Pose(player.position().add(0, 0.3, 0),
                        player.getLookAngle(), player.getEyePosition()) : beat.pose();
                RunicPresentation.spawnOneVfx(level, pose.origin(), pose.look(), beat.vfx(), beat.lineTarget(), pose.eyes());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        QUEUE.cancel(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        QUEUE.cancel(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QUEUE.cancel(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        QUEUE.clear();
    }
}
