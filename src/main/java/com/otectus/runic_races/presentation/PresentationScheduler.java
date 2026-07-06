package com.otectus.runic_races.presentation;

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

/**
 * Runs the delayed beats of multi-beat signature recipes ({@code .delayed(n)} specs).
 *
 * Beats track the caster live: at fire time the player is re-resolved by UUID and the
 * beat plays at their *current* position and look vector — so a rise-then-implode stays
 * attached to a strafing caster, and a blink ability's settle beat lands at the
 * destination. If the player is gone (logged out, died, dimension-hopped away from the
 * resolvable list) the beat is silently dropped. LINE targets are frozen at fire time.
 *
 * Registered on the Forge event bus in {@code RunicRacesMod}.
 */
public final class PresentationScheduler {

    private record PendingBeat(@Nullable SfxSpec sfx, @Nullable VfxSpec vfx, @Nullable Vec3 lineTarget) {}

    private record OwnedBeat(UUID owner, PendingBeat beat) {}

    private static final BeatQueue<OwnedBeat> QUEUE = new BeatQueue<>();

    public static void scheduleSfx(ServerPlayer player, SfxSpec spec) {
        QUEUE.schedule(player.getUUID(), spec.delayTicks(),
                new OwnedBeat(player.getUUID(), new PendingBeat(spec, null, null)));
    }

    public static void scheduleVfx(ServerPlayer player, VfxSpec spec, @Nullable Vec3 lineTarget) {
        QUEUE.schedule(player.getUUID(), spec.delayTicks(),
                new OwnedBeat(player.getUUID(), new PendingBeat(null, spec, lineTarget)));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || QUEUE.size() == 0) return;
        MinecraftServer server = event.getServer();
        for (OwnedBeat owned : QUEUE.tick()) {
            ServerPlayer player = server.getPlayerList().getPlayer(owned.owner());
            if (player == null || !player.isAlive() || !(player.level() instanceof ServerLevel level)) continue;
            PendingBeat beat = owned.beat();
            if (beat.sfx() != null) {
                RunicPresentation.playOneSfx(level, player.position(), beat.sfx());
            }
            if (beat.vfx() != null) {
                RunicPresentation.spawnOneVfx(level, player.position().add(0, 0.3, 0),
                        player.getLookAngle(), beat.vfx(), beat.lineTarget());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
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
