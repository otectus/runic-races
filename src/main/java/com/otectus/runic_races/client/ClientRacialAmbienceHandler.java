package com.otectus.runic_races.client;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.config.RRClientConfig;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Client-only ambience handler that spawns cosmetic particles / sounds tied to
 * race identity without involving the server (no packet cost, no shared state).
 *
 * <ul>
 *   <li><b>Wolfkin Scent Trail</b>: faint crit trails from the player toward any
 *       mob within 24 blocks that is below 50% HP, every ~15 ticks.</li>
 *   <li><b>Goblin Gleam Sense</b>: soft chime + end-rod sparkle when a chest or
 *       barrel block is within 18 blocks, rate-limited to once per 8 seconds per nearby chest.</li>
 *   <li><b>Wing environment reactions</b>: smoke wisps when Elder Drake is on
 *       fire, drip particles when Wyvern is wet/raining.</li>
 * </ul>
 *
 * All gated behind {@code hud.ambient.stateParticles} so users can silence it.
 */
@Mod.EventBusSubscriber(modid = RunicRacesMod.MOD_ID, value = Dist.CLIENT)
public final class ClientRacialAmbienceHandler {

    private static final int SCENT_TRAIL_INTERVAL = 15;
    private static final int GLEAM_SENSE_COOLDOWN = 160; // 8 seconds
    private static final int WING_ENV_INTERVAL = 10;

    private static long lastScentTrailTick = 0;
    private static long lastGleamTick = 0;
    private static long lastWingEnvTick = 0;

    // Scent-trail search cache: the 24-block entity scan is the heaviest ambience cost, so reuse
    // the result while the player is roughly stationary and re-query only on movement / staleness.
    private static List<LivingEntity> cachedWounded = java.util.Collections.emptyList();
    private static long lastScentQueryTick = Long.MIN_VALUE;
    private static double lastScentQueryX, lastScentQueryY, lastScentQueryZ;
    private static final int SCENT_QUERY_MAX_AGE = 20;
    private static final double SCENT_REQUERY_DIST_SQ = 4.0; // 2 blocks

    private ClientRacialAmbienceHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!RRClientConfig.AMBIENT_STATE_PARTICLES.get()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return;

        String race = RaceHelper.getRaceName(player).orElse(null);
        if (race == null) return;

        long gameTime = level.getGameTime();

        switch (race) {
            case "canine" -> maybeRunScentTrail(player, level, gameTime);
            case "deep_one" -> maybeRunGleamSense(player, level, gameTime);
            case "sprite", "faerie", "avian", "wind_wyrm",
                 "fire_drake", "ice_drake", "terra_drake", "volt_drake" ->
                    maybeRunWingEnv(player, level, race, gameTime);
            default -> { /* no ambience for other races yet */ }
        }
    }

    // ============================================================
    // Wolfkin: Scent Trail
    // ============================================================
    private static void maybeRunScentTrail(LocalPlayer player, ClientLevel level, long gameTime) {
        if (gameTime - lastScentTrailTick < SCENT_TRAIL_INTERVAL) return;
        lastScentTrailTick = gameTime;

        double dxq = player.getX() - lastScentQueryX;
        double dyq = player.getY() - lastScentQueryY;
        double dzq = player.getZ() - lastScentQueryZ;
        boolean stale = gameTime - lastScentQueryTick >= SCENT_QUERY_MAX_AGE;
        boolean moved = dxq * dxq + dyq * dyq + dzq * dzq > SCENT_REQUERY_DIST_SQ;
        if (stale || moved) {
            AABB box = player.getBoundingBox().inflate(24.0);
            cachedWounded = level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != player && e.isAlive() && (e instanceof Enemy || (e instanceof Mob m && m.getTarget() != null))
                            && e.getHealth() / e.getMaxHealth() < 0.5f);
            lastScentQueryTick = gameTime;
            lastScentQueryX = player.getX();
            lastScentQueryY = player.getY();
            lastScentQueryZ = player.getZ();
        }

        List<LivingEntity> wounded = cachedWounded;
        if (wounded.isEmpty()) return;

        Vec3 origin = player.position().add(0, player.getEyeHeight() * 0.5, 0);
        for (LivingEntity target : wounded) {
            if (!target.isAlive()) continue; // cached list may be up to SCENT_QUERY_MAX_AGE ticks old
            Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(origin);
            double dist = dir.length();
            if (dist < 2.0 || dist > 24.0) continue;
            Vec3 step = dir.normalize();
            int segments = Math.min(6, (int) (dist / 3.0));
            for (int i = 1; i <= segments; i++) {
                double t = i / (double) (segments + 1);
                double px = origin.x + step.x * dist * t;
                double py = origin.y + step.y * dist * t;
                double pz = origin.z + step.z * dist * t;
                level.addParticle(ParticleTypes.SCRAPE, px, py, pz, 0, 0, 0);
            }
        }
    }

    // ============================================================
    // Goblin: Gleam Sense
    // ============================================================
    private static void maybeRunGleamSense(LocalPlayer player, ClientLevel level, long gameTime) {
        if (gameTime - lastGleamTick < GLEAM_SENSE_COOLDOWN) return;

        BlockPos playerPos = player.blockPosition();
        int radius = 18;
        BlockPos found = null;

        // Scan chunk-local block entities first — cheap and catches the 99% case.
        for (BlockEntity be : nearbyBlockEntities(level, playerPos, radius)) {
            if (be instanceof ChestBlockEntity && be.getBlockState().getBlock() instanceof ChestBlock) {
                found = be.getBlockPos();
                break;
            }
        }

        if (found == null) return;
        lastGleamTick = gameTime;

        double cx = found.getX() + 0.5;
        double cy = found.getY() + 0.7;
        double cz = found.getZ() + 0.5;

        level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.25f, 1.6f, false);
        for (int i = 0; i < 8; i++) {
            level.addParticle(ParticleTypes.END_ROD,
                    cx + (Math.random() - 0.5) * 0.4,
                    cy + Math.random() * 0.6,
                    cz + (Math.random() - 0.5) * 0.4,
                    0, 0.02, 0);
        }
    }

    private static Iterable<BlockEntity> nearbyBlockEntities(ClientLevel level, BlockPos center, int radius) {
        // Iterate only the block-entity maps of the loaded chunks in range — O(actual block
        // entities) instead of the old O(volume) ~46k-position scan. Filtered to a cylinder of
        // the requested radius so behaviour matches the previous box scan closely enough.
        java.util.List<BlockEntity> result = new java.util.ArrayList<>();
        int minCX = (center.getX() - radius) >> 4;
        int maxCX = (center.getX() + radius) >> 4;
        int minCZ = (center.getZ() - radius) >> 4;
        int maxCZ = (center.getZ() + radius) >> 4;
        int r2 = radius * radius;
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos p = be.getBlockPos();
                    int dx = p.getX() - center.getX();
                    int dy = p.getY() - center.getY();
                    int dz = p.getZ() - center.getZ();
                    if (dx * dx + dz * dz <= r2 && Math.abs(dy) <= radius) {
                        result.add(be);
                    }
                }
            }
        }
        return result;
    }

    // ============================================================
    // Wings: environment reactions
    // ============================================================
    private static void maybeRunWingEnv(LocalPlayer player, ClientLevel level, String race, long gameTime) {
        if (gameTime - lastWingEnvTick < WING_ENV_INTERVAL) return;
        lastWingEnvTick = gameTime;

        double bx = player.getX();
        double by = player.getY() + 1.2;
        double bz = player.getZ();

        boolean isDrake = race.endsWith("_drake") || "wind_wyrm".equals(race);
        boolean isPixie = "sprite".equals(race) || "faerie".equals(race);

        if (isDrake && player.isOnFire()) {
            // Smoke wisps trail from the wings while burning.
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                        bx + (Math.random() - 0.5) * 0.8,
                        by + 0.2,
                        bz + (Math.random() - 0.5) * 0.8,
                        0, 0.03, 0);
            }
        } else if (("wind_wyrm".equals(race) || isDrake) && player.isInWaterOrRain()) {
            // Drip particles when wet.
            for (int i = 0; i < 2; i++) {
                level.addParticle(ParticleTypes.FALLING_WATER,
                        bx + (Math.random() - 0.5) * 0.7,
                        by + 0.1,
                        bz + (Math.random() - 0.5) * 0.7,
                        0, 0, 0);
            }
        } else if (isPixie && !player.onGround() && !player.isFallFlying()) {
            // Idle flutter sparkle while hovering.
            level.addParticle(ParticleTypes.END_ROD,
                    bx + (Math.random() - 0.5) * 0.5,
                    by,
                    bz + (Math.random() - 0.5) * 0.5,
                    0, -0.01, 0);
        } else if ("avian".equals(race) && !player.onGround() && !player.isFallFlying()) {
            // Soft feather-cloud while airborne.
            level.addParticle(ParticleTypes.CLOUD,
                    bx + (Math.random() - 0.5) * 0.5,
                    by,
                    bz + (Math.random() - 0.5) * 0.5,
                    0, -0.01, 0);
        }
    }
}
