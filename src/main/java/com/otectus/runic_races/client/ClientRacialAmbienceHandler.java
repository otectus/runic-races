package com.otectus.runic_races.client;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.client.render.WingType;
import com.otectus.runic_races.client.state.ClientRaceState;
import com.otectus.runic_races.common.state.RaceStateFlags;
import com.otectus.runic_races.config.RRClientConfig;
import com.otectus.runic_races.registry.ModParticles;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-only ambience: cosmetic particles/sounds tied to race identity with no
 * packet cost or shared state. One {@link AmbienceRoutine} per race, registered
 * in the {@code ROUTINES} map — adding a race's minute-to-minute "feel" is one
 * registry entry, mirroring the SignatureRegistry pattern on the server side.
 *
 * Everything here is Minor-tier (1-3 particles per pulse), gated behind
 * {@code ambient.stateParticles}, and scaled by {@code ambient.particleDensity}.
 * Winged races additionally get glide wingtip trails and a landing dust puff,
 * gated behind {@code wings.glideTrails}.
 */
@Mod.EventBusSubscriber(modid = RunicRacesMod.MOD_ID, value = Dist.CLIENT)
public final class ClientRacialAmbienceHandler {

    @FunctionalInterface
    private interface AmbienceRoutine {
        void tick(LocalPlayer player, ClientLevel level, long gameTime);
    }

    private static final Map<String, AmbienceRoutine> ROUTINES = new HashMap<>();

    /** Per-routine last-fired game time, keyed by routine id — replaces one static per effect. */
    private static final Map<String, Long> LAST_FIRED = new HashMap<>();

    private static final int GLEAM_SENSE_COOLDOWN = 160; // 8 seconds
    private static final int WING_ENV_INTERVAL = 10;

    // Scent-trail search cache: the 24-block entity scan is the heaviest ambience cost, so reuse
    // the result while the player is roughly stationary and re-query only on movement / staleness.
    private static List<LivingEntity> cachedWounded = java.util.Collections.emptyList();
    private static long lastScentQueryTick = Long.MIN_VALUE;
    private static double lastScentQueryX, lastScentQueryY, lastScentQueryZ;
    private static final int SCENT_QUERY_MAX_AGE = 20;
    private static final double SCENT_REQUERY_DIST_SQ = 4.0; // 2 blocks

    // Glide-trail / landing state (local player only).
    private static boolean wasGliding = false;
    private static double lastFallSpeed = 0.0;
    // Sea Serpen "freshly emerged" sheen.
    private static long lastWetTick = Long.MIN_VALUE;

    private ClientRacialAmbienceHandler() {}

    static {
        // ----- Human -----
        ROUTINES.put("primian", (p, l, t) -> {
            // Always learning: a single glint at the hands while adaptation stacks are live.
            if (ClientRaceState.has(RaceStateFlags.ADAPTATION_ACTIVE) && every("primian.glint", t, 200)) {
                puff(l, ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 1, 0.3, 0.02);
            }
        });
        ROUTINES.put("celeron", (p, l, t) -> {
            // A stray down feather flutters loose while sprinting.
            if (p.isSprinting() && every("celeron.feather", t, 40)) {
                puff(l, ModParticles.FEATHER_DOWN.get(), p.getX(), p.getY() + 1.2, p.getZ(), 1, 0.3, 0.0);
            }
        });
        ROUTINES.put("magi", (p, l, t) -> {
            // Magic leaks from them: glints drip off a held enchanted item.
            if (p.getMainHandItem().isEnchanted() && every("magi.leak", t, 60)) {
                Vec3 hand = handPos(p);
                puff(l, ModParticles.ARCANE_GLINT.get(), hand.x, hand.y, hand.z, scaled(2), 0.15, -0.01);
            }
        });
        // valen: intentionally quiet — his identity is procs (shoulder-check, fall shake), not ambience.

        // ----- Elven -----
        ROUTINES.put("high_elf", (p, l, t) -> {
            // Worn enchanted armor glimmers now and then.
            if (every("high_elf.glimmer", t, 160) && hasEnchantedArmor(p)) {
                puff(l, ModParticles.ARCANE_GLINT.get(), p.getX(), p.getY() + 0.6 + Math.random(), p.getZ(), 1, 0.35, 0.01);
            }
        });
        ROUTINES.put("dark_elf", (p, l, t) -> {
            // Darkness clings to them: shadow curls at the feet in deep gloom.
            if (every("dark_elf.shadow", t, 80) && l.getMaxLocalRawBrightness(p.blockPosition()) < 4) {
                puff(l, ModParticles.SHADOW_WISP.get(), p.getX(), p.getY() + 0.2, p.getZ(), scaled(2), 0.4, 0.01);
            }
        });
        ROUTINES.put("moon_elf", (p, l, t) -> {
            // Silver motes rise around them under the open night sky.
            if (every("moon_elf.motes", t, 60) && l.isNight() && l.canSeeSky(p.blockPosition())) {
                puff(l, ParticleTypes.END_ROD, p.getX(), p.getY() + 0.4, p.getZ(), scaled(2), 0.6, 0.03);
            }
        });
        ROUTINES.put("ice_elf", (p, l, t) -> {
            // Winter's child: gentle rime-fall in cold biomes.
            if (every("ice_elf.rime", t, 80) && isColdHere(l, p.blockPosition())) {
                puff(l, ModParticles.FROST_MOTE.get(), p.getX(), p.getY() + 1.8, p.getZ(), scaled(2), 0.6, -0.02);
            }
        });
        // blood_elf: identity lives in its combat procs (lifesteal siphon), not idle ambience.

        // ----- Dwarven -----
        ROUTINES.put("canine", ClientRacialAmbienceHandler::maybeRunScentTrail);
        ROUTINES.put("deep_one", ClientRacialAmbienceHandler::maybeRunGleamSense);
        ROUTINES.put("forge_one", (p, l, t) -> {
            // Contentment near a working forge: a single ember drifts off them.
            if (every("forge_one.ember", t, 100) && isNearLitForge(l, p.blockPosition())) {
                puff(l, ModParticles.EMBER_SCALE.get(), p.getX(), p.getY() + 1.3, p.getZ(), 1, 0.3, 0.02);
            }
        });
        ROUTINES.put("frost_one", (p, l, t) -> {
            // Visible breath-fog in the cold or at night.
            if (every("frost_one.breath", t, 120) && (isColdHere(l, p.blockPosition()) || l.isNight())) {
                Vec3 face = p.position().add(p.getLookAngle().scale(0.3)).add(0, p.getEyeHeight() - 0.1, 0);
                puff(l, ParticleTypes.CLOUD, face.x, face.y, face.z, 1, 0.05, 0.01);
            }
        });
        ROUTINES.put("iron_one", (p, l, t) -> {
            // Sparks glint off the raised shield while actively blocking.
            if (p.isBlocking() && every("iron_one.spark", t, 40)) {
                Vec3 front = p.position().add(p.getLookAngle().scale(0.6)).add(0, 1.1, 0);
                puff(l, ParticleTypes.ENCHANTED_HIT, front.x, front.y, front.z, scaled(2), 0.2, 0.02);
            }
        });
        ROUTINES.put("sky_one", (p, l, t) -> {
            // At home on the peaks: wind wisps and a faint gust high up under open sky.
            if (every("sky_one.wind", t, 200) && p.getY() > 120 && l.canSeeSky(p.blockPosition())) {
                puff(l, ParticleTypes.CLOUD, p.getX(), p.getY() + 1.4, p.getZ(), scaled(2), 0.8, 0.01);
                sound(l, p, SoundEvents.PHANTOM_FLAP, 0.08f, 0.5f);
            }
        });
        // runic_one: ward-zone glyphs already mark the ward via the RUNIC_WARD signature ring.

        // ----- Bestial -----
        ROUTINES.put("feline", (p, l, t) -> {
            // A cat by the hearth: quiet purr when idling near a lit campfire.
            if (every("feline.purr", t, 400) && p.getDeltaMovement().lengthSqr() < 0.01
                    && isNearLitForge(l, p.blockPosition())) {
                sound(l, p, SoundEvents.CAT_PURR, 0.3f, 1.0f);
            }
        });
        ROUTINES.put("kitsune", (p, l, t) -> {
            // A will-o'-wisp tail of foxfire when sprinting through the night.
            if (p.isSprinting() && l.isNight() && every("kitsune.tail", t, 30)) {
                puff(l, ModParticles.FOXFIRE.get(), p.getX(), p.getY() + 0.6, p.getZ(), 1, 0.25, 0.01);
            }
        });
        ROUTINES.put("serpen", (p, l, t) -> {
            // A slow tongue-taste of the air while sneaking near prey.
            if (p.isCrouching() && every("serpen.hiss", t, 160) && hasMobNearby(p, l, 6.0)) {
                sound(l, p, SoundEvents.CAT_HISS, 0.15f, 0.6f);
            }
        });
        ROUTINES.put("avian", (p, l, t) -> {
            maybeRunWingEnv(p, l, "avian", t);
            tickWingTrail(p, l, "avian", t);
        });
        // arachnid: web-sense tremble is a Phase-4 proc (needs threat detection, not idle state).

        // ----- Faeborne -----
        ROUTINES.put("changeling", (p, l, t) -> {
            // The glamour ripples: a sparkle passes head-to-toe.
            if (every("changeling.ripple", t, 400)) {
                for (int i = 0; i < 3; i++) {
                    l.addParticle(ModParticles.FAE_SPARKLE.get(),
                            p.getX(), p.getY() + 1.8 - i * 0.7, p.getZ(), 0, -0.05, 0);
                }
            }
        });
        ROUTINES.put("dryad", (p, l, t) -> {
            // Leaves drift from them while on living ground.
            if (every("dryad.petal", t, 120) && standsOnLivingGround(l, p)) {
                puff(l, ModParticles.LEAF_PETAL.get(), p.getX(), p.getY() + 1.5, p.getZ(), scaled(2), 0.5, -0.02);
            }
        });
        ROUTINES.put("nymph", (p, l, t) -> {
            // Always freshly emerged: soft drips near open water.
            if (every("nymph.drip", t, 100) && isNearWater(l, p.blockPosition())) {
                puff(l, ParticleTypes.FALLING_WATER, p.getX(), p.getY() + 1.2, p.getZ(), scaled(2), 0.4, 0.0);
            }
        });
        ROUTINES.put("sprite", (p, l, t) -> {
            maybeRunWingEnv(p, l, "sprite", t);
            tickWingTrail(p, l, "sprite", t);
        });
        ROUTINES.put("faerie", (p, l, t) -> {
            maybeRunWingEnv(p, l, "faerie", t);
            tickWingTrail(p, l, "faerie", t);
        });

        // ----- Undead -----
        ROUTINES.put("zombie", (p, l, t) -> {
            // Slow decay: a drifting ash mote and the occasional low self-groan.
            if (every("zombie.ash", t, 300)) {
                puff(l, ParticleTypes.ASH, p.getX(), p.getY() + 1.2, p.getZ(), scaled(2), 0.4, 0.01);
            }
            if (every("zombie.groan", t, 900)) {
                sound(l, p, SoundEvents.ZOMBIE_AMBIENT, 0.12f, 0.75f);
            }
        });
        ROUTINES.put("skeleton", (p, l, t) -> {
            // Dry bone-clicks when moving fast.
            if (p.isSprinting() && every("skeleton.rattle", t, 80)) {
                sound(l, p, SoundEvents.SKELETON_STEP, 0.2f, 0.7f);
            }
        });
        ROUTINES.put("wraith", (p, l, t) -> {
            // Half here, half not: shadow trail while sneaking, a soul slipping loose at rest.
            if (p.isCrouching() && every("wraith.trail", t, 40)) {
                puff(l, ModParticles.SHADOW_WISP.get(), p.getX(), p.getY() + 0.3, p.getZ(), 1, 0.3, 0.01);
            }
            if (every("wraith.soul", t, 300)) {
                puff(l, ModParticles.SOUL_WISP.get(), p.getX(), p.getY() + 1.2, p.getZ(), 1, 0.3, 0.03);
            }
        });
        ROUTINES.put("demon", (p, l, t) -> {
            // Perpetually smoldering.
            if (every("demon.smolder", t, 240)) {
                puff(l, ModParticles.EMBER_SCALE.get(), p.getX(), p.getY() + 1.5, p.getZ(), 1, 0.35, 0.02);
                puff(l, ParticleTypes.SMOKE, p.getX(), p.getY() + 1.8, p.getZ(), 1, 0.15, 0.02);
            }
        });
        // reaper: its moments are the mythic revival and harvest — idle quiet is intentional.

        // ----- Draconic -----
        ROUTINES.put("fire_drake", (p, l, t) -> {
            maybeRunWingEnv(p, l, "fire_drake", t);
            tickWingTrail(p, l, "fire_drake", t);
            if (every("fire_drake.ember", t, 160) && p.getDeltaMovement().lengthSqr() < 0.01) {
                puff(l, ModParticles.EMBER_SCALE.get(), p.getX(), p.getY() + 1.4, p.getZ(), 1, 0.4, 0.01);
            }
        });
        ROUTINES.put("ice_drake", (p, l, t) -> {
            maybeRunWingEnv(p, l, "ice_drake", t);
            tickWingTrail(p, l, "ice_drake", t);
            if (every("ice_drake.snow", t, 160) && isColdHere(l, p.blockPosition())) {
                puff(l, ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1.9, p.getZ(), scaled(2), 0.5, -0.02);
            }
        });
        ROUTINES.put("terra_drake", (p, l, t) -> {
            maybeRunWingEnv(p, l, "terra_drake", t);
            tickWingTrail(p, l, "terra_drake", t);
            // Heavy footfalls kick up dust while striding.
            if (p.onGround() && p.getDeltaMovement().horizontalDistanceSqr() > 0.01
                    && every("terra_drake.step", t, 20)) {
                puff(l, ParticleTypes.POOF, p.getX(), p.getY() + 0.05, p.getZ(), scaled(1), 0.3, 0.01);
            }
        });
        ROUTINES.put("volt_drake", (p, l, t) -> {
            maybeRunWingEnv(p, l, "volt_drake", t);
            tickWingTrail(p, l, "volt_drake", t);
            // Charged by the storm: static crackle under open thundering sky.
            if (every("volt_drake.static", t, 60) && l.isThundering() && l.canSeeSky(p.blockPosition())) {
                puff(l, ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1.0 + Math.random(), p.getZ(), scaled(2), 0.5, 0.0);
            }
        });
        ROUTINES.put("wind_wyrm", (p, l, t) -> {
            maybeRunWingEnv(p, l, "wind_wyrm", t);
            tickWingTrail(p, l, "wind_wyrm", t);
            if (every("wind_wyrm.breeze", t, 120) && p.getY() > 100 && l.canSeeSky(p.blockPosition())) {
                puff(l, ParticleTypes.CLOUD, p.getX(), p.getY() + 1.5, p.getZ(), 1, 0.7, 0.01);
            }
        });
        ROUTINES.put("sea_serpen", (p, l, t) -> {
            // Bubble wake while swimming, dripping sheen for a few seconds after surfacing.
            if (p.isInWater()) {
                lastWetTick = t;
                if (every("sea_serpen.bubbles", t, 20)) {
                    puff(l, ParticleTypes.BUBBLE, p.getX(), p.getY() + 0.6, p.getZ(), scaled(2), 0.4, 0.02);
                }
            } else if (t - lastWetTick < 200 && every("sea_serpen.drip", t, 30)) {
                puff(l, ParticleTypes.FALLING_WATER, p.getX(), p.getY() + 1.3, p.getZ(), scaled(2), 0.4, 0.0);
            }
        });
    }

    // ============================================================
    // Dispatch
    // ============================================================
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!RRClientConfig.AMBIENT_STATE_PARTICLES.get()) return;
        if (RRClientConfig.AMBIENT_PARTICLE_DENSITY.get() <= 0.0) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return;

        String race = RaceHelper.getRaceName(player).orElse(null);
        if (race == null) return;

        AmbienceRoutine routine = ROUTINES.get(race);
        if (routine != null) {
            routine.tick(player, level, level.getGameTime());
        }
    }

    // ============================================================
    // Shared helpers
    // ============================================================

    /** Scales a base particle count by {@code ambient.particleDensity}; fractional remainders spawn probabilistically. */
    private static int scaled(int base) {
        double density = RRClientConfig.AMBIENT_PARTICLE_DENSITY.get();
        double exact = base * density;
        int whole = (int) exact;
        return whole + (Math.random() < exact - whole ? 1 : 0);
    }

    /** Rate limiter: true (and stamps) when {@code interval} ticks have passed since this key last fired. */
    private static boolean every(String key, long gameTime, int interval) {
        Long last = LAST_FIRED.get(key);
        if (last != null && gameTime - last < interval) return false;
        LAST_FIRED.put(key, gameTime);
        return true;
    }

    private static void puff(ClientLevel level, ParticleOptions particle,
                             double x, double y, double z, int count, double spread, double vy) {
        for (int i = 0; i < count; i++) {
            level.addParticle(particle,
                    x + (Math.random() - 0.5) * 2 * spread,
                    y + (Math.random() - 0.5) * spread,
                    z + (Math.random() - 0.5) * 2 * spread,
                    0, vy, 0);
        }
    }

    private static void sound(ClientLevel level, LocalPlayer player, SoundEvent event, float volume, float pitch) {
        level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                event, SoundSource.PLAYERS, volume, pitch, false);
    }

    private static Vec3 handPos(LocalPlayer player) {
        // Rough main-hand position: half a block toward look, dropped to chest height.
        return player.position().add(player.getLookAngle().scale(0.5)).add(0, 1.0, 0);
    }

    private static boolean hasEnchantedArmor(LocalPlayer player) {
        for (var stack : player.getArmorSlots()) {
            if (stack.isEnchanted()) return true;
        }
        return false;
    }

    private static boolean isColdHere(ClientLevel level, BlockPos pos) {
        return level.getBiome(pos).value().coldEnoughToSnow(pos);
    }

    private static boolean isNearWater(ClientLevel level, BlockPos pos) {
        for (BlockPos check : new BlockPos[]{pos, pos.north(2), pos.south(2), pos.east(2), pos.west(2), pos.below()}) {
            if (level.getFluidState(check).is(FluidTags.WATER)) return true;
        }
        return false;
    }

    private static boolean standsOnLivingGround(ClientLevel level, LocalPlayer player) {
        BlockState below = level.getBlockState(player.blockPosition().below());
        return below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.MOSS_BLOCK) || below.is(Blocks.PODZOL);
    }

    /** True when a lit campfire or burning furnace sits within 4 blocks (fires at most on ambience cadence). */
    private static boolean isNearLitForge(ClientLevel level, BlockPos center) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    cursor.setWithOffset(center, dx, dy, dz);
                    BlockState state = level.getBlockState(cursor);
                    if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) return true;
                    if (state.getBlock() instanceof AbstractFurnaceBlock && state.getValue(AbstractFurnaceBlock.LIT)) return true;
                }
            }
        }
        return false;
    }

    private static boolean hasMobNearby(LocalPlayer player, ClientLevel level, double radius) {
        AABB box = player.getBoundingBox().inflate(radius);
        return !level.getEntitiesOfClass(Mob.class, box, LivingEntity::isAlive).isEmpty();
    }

    // ============================================================
    // Glide wingtip trails + landing puff (winged races)
    // ============================================================
    private static void tickWingTrail(LocalPlayer player, ClientLevel level, String race, long gameTime) {
        if (!RRClientConfig.WINGS_GLIDE_TRAILS.get()) return;
        WingType wingType = WingType.forRaceName(race).orElse(null);
        if (wingType == null) return;

        boolean gliding = player.isFallFlying()
                && player.getDeltaMovement().horizontalDistanceSqr() > 0.16; // > 0.4 blocks/tick

        if (gliding && gameTime % 3 == 0) {
            ParticleOptions trail = trailParticleFor(race);
            float scale = wingType.getScale();
            double yaw = Math.toRadians(player.yBodyRot);
            // right = (-cos, 0, -sin); back = (sin, 0, -cos) for MC yaw conventions
            double rx = -Math.cos(yaw) * 0.9 * scale;
            double rz = -Math.sin(yaw) * 0.9 * scale;
            double bx = Math.sin(yaw) * 0.3;
            double bz = -Math.cos(yaw) * 0.3;
            double y = player.getY() + 0.6;
            if (scaled(1) > 0) {
                level.addParticle(trail, player.getX() + rx + bx, y, player.getZ() + rz + bz, 0, 0, 0);
                level.addParticle(trail, player.getX() - rx + bx, y, player.getZ() - rz + bz, 0, 0, 0);
            }
        }

        // Landing puff: a dust ring scaled to the wing when touching down from a real descent.
        if (!wasGliding && player.isFallFlying()) {
            wasGliding = true;
        }
        if (!player.onGround()) {
            lastFallSpeed = -player.getDeltaMovement().y;
        } else {
            if (wasGliding && lastFallSpeed > 0.5) {
                int count = scaled((int) Math.ceil(6 * wingType.getScale()));
                for (int i = 0; i < count; i++) {
                    double angle = (Math.PI * 2 * i) / Math.max(1, count);
                    level.addParticle(ParticleTypes.POOF,
                            player.getX() + Math.cos(angle) * 0.8,
                            player.getY() + 0.1,
                            player.getZ() + Math.sin(angle) * 0.8,
                            Math.cos(angle) * 0.05, 0.02, Math.sin(angle) * 0.05);
                }
            }
            wasGliding = false;
            lastFallSpeed = 0.0;
        }
    }

    private static ParticleOptions trailParticleFor(String race) {
        return switch (race) {
            case "sprite", "faerie" -> ModParticles.FAE_SPARKLE.get();
            case "fire_drake" -> ModParticles.EMBER_SCALE.get();
            case "ice_drake" -> ModParticles.FROST_MOTE.get();
            case "volt_drake" -> ParticleTypes.ELECTRIC_SPARK;
            case "terra_drake" -> ParticleTypes.POOF;
            default -> ParticleTypes.CLOUD; // avian, wind_wyrm
        };
    }

    // ============================================================
    // Canine: Scent Trail
    // ============================================================
    private static void maybeRunScentTrail(LocalPlayer player, ClientLevel level, long gameTime) {
        if (!every("canine.scent", gameTime, 15)) return;

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

        // Quiet sniff when the trail first appears after a lull.
        if (every("canine.sniff", gameTime, 200)) {
            sound(level, player, SoundEvents.FOX_SNIFF, 0.25f, 0.8f);
        }

        Vec3 origin = player.position().add(0, player.getEyeHeight() * 0.5, 0);
        for (LivingEntity target : wounded) {
            if (!target.isAlive()) continue; // cached list may be up to SCENT_QUERY_MAX_AGE ticks old
            Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(origin);
            double dist = dir.length();
            if (dist < 2.0 || dist > 24.0) continue;
            Vec3 step = dir.normalize();
            int segments = scaled(Math.min(6, (int) (dist / 3.0)));
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
    // Deep One: Gleam Sense
    // ============================================================
    private static void maybeRunGleamSense(LocalPlayer player, ClientLevel level, long gameTime) {
        if (!every("deep_one.gleam", gameTime, GLEAM_SENSE_COOLDOWN)) return;

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

        double cx = found.getX() + 0.5;
        double cy = found.getY() + 0.7;
        double cz = found.getZ() + 0.5;

        level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.25f, 1.6f, false);
        // Treasure-gold glint (VOLT_GOLD dust) — on-palette for the gleam sense.
        for (int i = 0; i < scaled(8); i++) {
            level.addParticle(com.otectus.runic_races.presentation.RaceColors.VOLT_GOLD,
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
        if (!every("wing.env", gameTime, WING_ENV_INTERVAL)) return;

        double bx = player.getX();
        double by = player.getY() + 1.2;
        double bz = player.getZ();

        boolean isDrake = race.endsWith("_drake") || "wind_wyrm".equals(race);
        boolean isPixie = "sprite".equals(race) || "faerie".equals(race);

        if (isDrake && player.isOnFire()) {
            // Smoke wisps trail from the wings while burning.
            for (int i = 0; i < scaled(3); i++) {
                level.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                        bx + (Math.random() - 0.5) * 0.8,
                        by + 0.2,
                        bz + (Math.random() - 0.5) * 0.8,
                        0, 0.03, 0);
            }
        } else if (isDrake && player.isInWaterOrRain()) {
            // Drip particles when wet.
            for (int i = 0; i < scaled(2); i++) {
                level.addParticle(ParticleTypes.FALLING_WATER,
                        bx + (Math.random() - 0.5) * 0.7,
                        by + 0.1,
                        bz + (Math.random() - 0.5) * 0.7,
                        0, 0, 0);
            }
        } else if (isPixie && !player.onGround() && !player.isFallFlying()) {
            // Idle flutter sparkle while hovering — gossamer shimmer, not generic end-rod.
            for (int i = 0; i < scaled(1); i++) {
                level.addParticle(ModParticles.FAE_SPARKLE.get(),
                        bx + (Math.random() - 0.5) * 0.5,
                        by,
                        bz + (Math.random() - 0.5) * 0.5,
                        0, -0.01, 0);
            }
        } else if ("avian".equals(race) && !player.onGround() && !player.isFallFlying()) {
            // Soft feather-cloud while airborne.
            for (int i = 0; i < scaled(1); i++) {
                level.addParticle(ModParticles.FEATHER_DOWN.get(),
                        bx + (Math.random() - 0.5) * 0.5,
                        by,
                        bz + (Math.random() - 0.5) * 0.5,
                        0, -0.01, 0);
            }
        }
    }
}
