package com.otectus.runic_races.event;

import com.mojang.datafixers.util.Pair;
import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.common.state.RaceStateFlags;
import com.otectus.runic_races.common.state.RaceStateTracker;
import com.otectus.runic_races.network.NetworkHandler;
import com.otectus.runic_races.network.S2CAdaptationStacksPacket;
import com.otectus.runic_races.network.S2CScreenCuePacket;
import com.otectus.runic_races.presentation.CueType;
import com.otectus.runic_races.presentation.RunicPresentation;
import com.otectus.runic_races.presentation.SignatureKey;
import com.otectus.runic_races.util.RaceHelper;
import com.otectus.runic_races.util.OriginsPowerHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

import java.util.UUID;

/**
 * Central Forge event handler for racial mechanics that operate at the engine level,
 * outside of what Origins data-driven powers can express.
 *
 * Handles:
 * - Catfolk Nine Lives (lethal damage prevention)
 * - Revenant death-site revival
 * - Mountain/Deep Dwarf forge blessings
 * - Brief contact-damage suppression during Pehkui resizes (both growing and shrinking)
 * - Jump height compensation for scaled-down races (Pehkui)
 */
public class RacialEventHandler {

    // Mod presence never changes at runtime — resolve once instead of per-jump.
    private static final boolean PEHKUI_LOADED = ModList.get().isLoaded("pehkui");

    // NBT tag keys for cooldown tracking
    private static final String TROLL_FIRE_SUPPRESS_UNTIL = "runic_races:troll_fire_suppress_until";
    private static final String VAMPIRE_SUN_TICKS = "runic_races:vampire_sun_ticks";
    private static final String HUMAN_ADAPT_STACKS = "runic_races:human_adapt_stacks";
    private static final String HUMAN_ADAPT_LAST_TICK = "runic_races:human_adapt_last_tick";
    private static final String HUMAN_ADAPT_BIOME = "runic_races:human_adapt_last_biome";
    private static final String HUMAN_ADAPT_SYNCED_STACKS = "runic_races:human_adapt_synced_stacks";
    private static final UUID HUMAN_ADAPT_UUID = UUID.fromString("a7b3c8d5-3456-6789-abcd-ef0123456789");
    private static final int ADAPT_STACK_DECAY_TICKS = 1200; // 60s
    private static final int ADAPT_STACK_CAP = 5;
    private static final String REVENANT_REVIVAL_COOLDOWN = "runic_races:revenant_revival_cd";
    private static final String REVENANT_DEATH_X = "runic_races:death_x";
    private static final String REVENANT_DEATH_Y = "runic_races:death_y";
    private static final String REVENANT_DEATH_Z = "runic_races:death_z";
    private static final String REVENANT_DEATH_DIM = "runic_races:death_dim";
    private static final String RESIZE_PROTECTION_UNTIL = "runic_races:resize_protection_until";
    private static final String REVENANT_REVIVAL_ATTEMPTS = "runic_races:revenant_revival_attempts";
    private static final int MAX_REVIVAL_ATTEMPTS = 3;

    // Cooldowns in ticks
    private static final int NINE_LIVES_CD_TICKS = 12000; // 10 minutes
    private static final int REVENANT_REVIVAL_CD_TICKS = 36000; // 30 minutes
    private static final ResourceLocation NINE_LIVES_RESOURCE = new ResourceLocation(RunicRacesMod.MOD_ID, "feline/nine_lives_cooldown_timer");

    // ==================== FELINE: NINE LIVES ====================

    /**
     * Intercepts lethal damage for Feline. If the player would die and Nine Lives
     * is off cooldown, cancel the death: set health to 1, grant brief invulnerability,
     * and start the cooldown.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (hasResizeProtection(player) && isResizeContactDamage(event.getSource())) {
            event.setAmount(0);
            RunicRacesMod.debug("[RunicRaces] Suppressed resize contact damage '{}' for {}",
                    event.getSource().getMsgId(), player.getName().getString());
            return;
        }

        String race = RaceHelper.getRaceName(player).orElse(null);
        if (race == null) return;

        // === FELINE: NINE LIVES ===
        if ("feline".equals(race)) {
            float currentHealth = player.getHealth();
            float incomingDamage = event.getAmount();

            // Would this kill or nearly kill the player?
            if (currentHealth - incomingDamage <= 0.0f) {
                if (OriginsPowerHelper.isResourceReady(player, NINE_LIVES_RESOURCE)) {
                    // Trigger Nine Lives: cancel the lethal damage
                    event.setAmount(0);
                    player.setHealth(2.0f); // 1 heart
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 4)); // Resistance V, 2s
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1)); // Regen II, 3s
                    OriginsPowerHelper.setResourceValue(player, NINE_LIVES_RESOURCE, NINE_LIVES_CD_TICKS);

                    RunicPresentation.fire(player, SignatureKey.FELINE_NINE_LIVES);
                    RunicRacesMod.debug("[RunicRaces] Nine Lives triggered for {}", player.getName().getString());
                }
            }
        }
    }

    // ==================== REVENANT: DEATH-SITE REVIVAL ====================

    /**
     * On death, if the player is a Revenant and revival is off cooldown,
     * record the death location for revival on respawn.
     */
    @SubscribeEvent
    public void onAnyLivingDeath(LivingDeathEvent event) {
        // Human Adaptation: bump a stack when a human kills any distinct mob type.
        // Separate @SubscribeEvent so it doesn't compete with the Revenant priority hook below.
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        if (!RaceHelper.isRace(killer, "primian")) return;

        CompoundTag data = killer.getPersistentData();
        int stacks = data.getInt(HUMAN_ADAPT_STACKS);
        if (stacks < ADAPT_STACK_CAP) {
            data.putInt(HUMAN_ADAPT_STACKS, stacks + 1);
            data.putLong(HUMAN_ADAPT_LAST_TICK, killer.level().getGameTime());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!RaceHelper.isRace(player, "reaper")) return;

        CompoundTag data = player.getPersistentData();
        long now = player.level().getGameTime();

        if (isCooldownReady(data, REVENANT_REVIVAL_COOLDOWN, now, REVENANT_REVIVAL_CD_TICKS)) {
            int attempts = data.getInt(REVENANT_REVIVAL_ATTEMPTS);
            if (attempts >= MAX_REVIVAL_ATTEMPTS) {
                data.putInt(REVENANT_REVIVAL_ATTEMPTS, 0);
                RunicRacesMod.LOGGER.warn("[RunicRaces] Revenant {} exceeded max revival attempts, using normal respawn",
                        player.getName().getString());
                return;
            }
            data.putInt(REVENANT_REVIVAL_ATTEMPTS, attempts + 1);

            // Record death position for revival
            data.putDouble(REVENANT_DEATH_X, player.getX());
            data.putDouble(REVENANT_DEATH_Y, player.getY());
            data.putDouble(REVENANT_DEATH_Z, player.getZ());
            data.putString(REVENANT_DEATH_DIM, player.level().dimension().location().toString());
            data.putLong(REVENANT_REVIVAL_COOLDOWN, now);

            RunicRacesMod.debug("[RunicRaces] Revenant death recorded for {} at ({}, {}, {})",
                    player.getName().getString(), player.getX(), player.getY(), player.getZ());
        }
    }

    /**
     * On respawn, if the Revenant has a recorded death site, teleport them there
     * with reduced health and temporary invulnerability instead of normal respawn.
     */
    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!RaceHelper.isRace(player, "reaper")) return;

        CompoundTag data = player.getPersistentData();
        if (!data.contains(REVENANT_DEATH_X)) return;

        double x = data.getDouble(REVENANT_DEATH_X);
        double y = data.getDouble(REVENANT_DEATH_Y);
        double z = data.getDouble(REVENANT_DEATH_Z);
        String dim = data.getString(REVENANT_DEATH_DIM);

        ResourceLocation dimensionId = ResourceLocation.tryParse(dim);
        ServerLevel targetLevel = dimensionId == null ? null
                : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        if (targetLevel != null) {
            Vec3 safePos = findSafeRevivalPosition(targetLevel, x, y, z);
            if (safePos != null) {
                player.teleportTo(targetLevel, safePos.x, safePos.y, safePos.z, player.getYRot(), player.getXRot());
                player.setHealth(6.0f); // 3 hearts
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 4)); // Resistance V, 30s
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600)); // Visible to others
                RunicPresentation.fire(player, SignatureKey.REAPER_REVIVAL);
                data.putInt(REVENANT_REVIVAL_ATTEMPTS, 0);

                RunicRacesMod.debug("[RunicRaces] Revenant {} revived at safe position ({}, {}, {})",
                        player.getName().getString(), safePos.x, safePos.y, safePos.z);
            } else {
                RunicRacesMod.LOGGER.warn("[RunicRaces] No safe revival position for Revenant {} near ({}, {}, {}), using normal respawn",
                        player.getName().getString(), x, y, z);
                RunicPresentation.fire(player, SignatureKey.REAPER_REVIVAL_REJECTED);
            }
        } else {
            RunicRacesMod.LOGGER.warn("[RunicRaces] Could not restore Revenant {} to death dimension '{}'",
                    player.getName().getString(), dim);
        }

        data.remove(REVENANT_DEATH_X);
        data.remove(REVENANT_DEATH_Y);
        data.remove(REVENANT_DEATH_Z);
        data.remove(REVENANT_DEATH_DIM);
    }

    // ==================== MOUNTAIN DWARF: FORGE BLESSING ====================

    private static final String FORGE_BLESSING_COOLDOWN = "runic_races:forge_blessing_cd";
    private static final int FORGE_BLESSING_CD_TICKS = 12000; // 10 minutes
    private static final float FORGE_BLESSING_CHANCE = 0.25f; // 25% chance

    /**
     * Mountain Dwarf's Forge Blessing: when crafting items, there is a 25% chance
     * the item receives a bonus enchantment (Unbreaking I-II) representing
     * superior dwarven craftsmanship. 10-minute cooldown between procs.
     *
     * Deep Dwarves get a weaker version (15% chance, Unbreaking I only).
     */
    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        String race = RaceHelper.getRaceName(player).orElse(null);
        if (race == null) return;

        boolean isForgeOne = "forge_one".equals(race);
        boolean isRunicOne = "runic_one".equals(race);
        if (!isForgeOne && !isRunicOne) return;

        ItemStack crafted = event.getCrafting();
        if (crafted.isEmpty() || !crafted.isEnchantable()) return;

        CompoundTag data = player.getPersistentData();
        long now = player.level().getGameTime();

        if (!isCooldownReady(data, FORGE_BLESSING_COOLDOWN, now, FORGE_BLESSING_CD_TICKS)) return;

        float chance = isForgeOne ? FORGE_BLESSING_CHANCE : 0.15f;
        if (player.getRandom().nextFloat() >= chance) return;

        // Apply Forge Blessing: add Unbreaking enchantment
        int level = isForgeOne ? (player.getRandom().nextFloat() < 0.3f ? 2 : 1) : 1;
        int existing = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, crafted);
        if (existing < level) {
            crafted.enchant(Enchantments.UNBREAKING, level);
            data.putLong(FORGE_BLESSING_COOLDOWN, now);
            RunicPresentation.fire(player, SignatureKey.FORGE_BLESSING, "Unbreaking " + toRoman(level));
            RunicRacesMod.debug("[RunicRaces] Forge Blessing triggered for {} — Unbreaking {} on {}",
                    player.getName().getString(), level, crafted.getDisplayName().getString());
        }
    }

    private static String toRoman(int n) {
        return switch (n) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; default -> String.valueOf(n); };
    }

    private static boolean isCooldownReady(CompoundTag data, String key, long now, int cooldownTicks) {
        return !data.contains(key) || now - data.getLong(key) >= cooldownTicks;
    }

    public static void markResizeProtection(ServerPlayer player, int durationTicks) {
        long expiresAt = player.level().getGameTime() + Math.max(1, durationTicks);
        player.getPersistentData().putLong(RESIZE_PROTECTION_UNTIL, expiresAt);
    }

    private static boolean hasResizeProtection(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(RESIZE_PROTECTION_UNTIL)) {
            return false;
        }

        long expiresAt = data.getLong(RESIZE_PROTECTION_UNTIL);
        if (player.level().getGameTime() > expiresAt) {
            data.remove(RESIZE_PROTECTION_UNTIL);
            return false;
        }

        return true;
    }

    private static boolean isResizeContactDamage(DamageSource source) {
        return source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING) || source.is(DamageTypes.FALL);
    }

    /**
     * Scan upward from the death position to find 2 air blocks above a solid, non-hazardous floor.
     * Returns null if no safe position is found within 10 blocks vertically.
     */
    private static Vec3 findSafeRevivalPosition(ServerLevel level, double x, double y, double z) {
        int minY = level.getMinBuildHeight();
        if (y < minY + 1) y = minY + 1;

        net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int dy = 0; dy <= 10; dy++) {
            int checkY = (int) y + dy;
            pos.set((int) x, checkY, (int) z);

            BlockState below = level.getBlockState(pos.below());
            BlockState atFeet = level.getBlockState(pos);
            BlockState atHead = level.getBlockState(pos.above());

            boolean standable = below.blocksMotion();
            boolean feetClear = !atFeet.blocksMotion() && atFeet.getFluidState().isEmpty();
            boolean headClear = !atHead.blocksMotion() && atHead.getFluidState().isEmpty();
            boolean notHazardous = !below.is(Blocks.LAVA) && !below.is(Blocks.FIRE)
                    && !below.is(Blocks.MAGMA_BLOCK) && !below.is(Blocks.CAMPFIRE)
                    && !below.is(Blocks.SOUL_CAMPFIRE) && !below.is(Blocks.SOUL_FIRE);

            if (standable && feetClear && headClear && notHazardous) {
                return new Vec3(x, checkY, z);
            }
        }
        return null;
    }

    // ==================== PER-PLAYER STATE WATCHER ====================

    /**
     * Every 10 ticks, update race-driven transient state flags for the HUD.
     * Cheap work only — biome/daylight checks, sky-access, short NBT reads.
     * Anything more expensive belongs in its own dedicated power or handler.
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 10 != 0) return;

        String race = RaceHelper.getRaceName(player).orElse(null);
        if (race == null) return;

        long now = player.level().getGameTime();

        // --- Zombie sunlight decay escalation ---
        if ("zombie".equals(race)) {
            boolean exposedToSun = player.level().isDay()
                    && player.level().canSeeSky(player.blockPosition())
                    && !player.isInWaterOrRain();
            CompoundTag data = player.getPersistentData();
            int sunTicks = data.getInt(VAMPIRE_SUN_TICKS);
            if (exposedToSun) {
                int newTicks = sunTicks + 10;
                data.putInt(VAMPIRE_SUN_TICKS, newTicks);
                RaceStateTracker.setFlag(player, RaceStateFlags.SUNLIGHT_BURNING, true);
                if (sunTicks == 0 && player.level() instanceof ServerLevel level) {
                    // First tick in sun: quiet hiss + smoke puff
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,
                            net.minecraft.sounds.SoundSource.PLAYERS, 0.3f, 1.6f);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            6, 0.2, 0.4, 0.2, 0.02);
                }
                if (sunTicks < 60 && newTicks >= 60 && player.level() instanceof ServerLevel level) {
                    // Crossed the 3-second line: louder burn cue
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sounds.SoundEvents.BLAZE_SHOOT,
                            net.minecraft.sounds.SoundSource.PLAYERS, 0.4f, 1.2f);
                }
            } else {
                if (sunTicks > 0) data.putInt(VAMPIRE_SUN_TICKS, 0);
                RaceStateTracker.setFlag(player, RaceStateFlags.SUNLIGHT_BURNING, false);
            }
        }

        // --- Sun-sensitive races sunlight flag (mirrors their data-driven sun penalties for HUD) ---
        // Their damage/penalties stay in the power JSON (exposed_to_sun + daytime); this only drives
        // the shared SUNLIGHT_BURNING flag so the rune lights and the notification fires.
        if ("deep_one".equals(race) || "dark_elf".equals(race) || "skeleton".equals(race)
                || "wraith".equals(race) || "reaper".equals(race)) {
            boolean burning = player.level().isDay()
                    && player.level().canSeeSky(player.blockPosition())
                    && !player.isInWaterOrRain();
            RaceStateTracker.setFlag(player, RaceStateFlags.SUNLIGHT_BURNING, burning);
        }

        // --- Sky One / Wind Wyrm claustrophobia (tight-space) ---
        if ("sky_one".equals(race) || "wind_wyrm".equals(race)) {
            boolean tight = !player.level().canSeeSky(player.blockPosition())
                    && player.blockPosition().getY() < player.level().getSeaLevel();
            RaceStateTracker.setFlag(player, RaceStateFlags.TIGHT_SPACE, tight);
        }

        // --- Volt Drake open-sky exposure (mirrors its grounded penalty) ---
        if ("volt_drake".equals(race)) {
            boolean openSky = player.level().canSeeSky(player.blockPosition());
            RaceStateTracker.setFlag(player, RaceStateFlags.OPEN_SKY, openSky);
        }

        // --- Fire-vulnerable readout (fire-weak races) ---
        boolean fireWeak = "dryad".equals(race) || "arachnid".equals(race) || "nymph".equals(race)
                || "ice_drake".equals(race) || "frost_one".equals(race) || "sea_serpen".equals(race);
        if (fireWeak) {
            RaceStateTracker.setFlag(player, RaceStateFlags.FIRE_VULNERABLE, player.isOnFire());
        }

        // --- Primian Adaptation stacks: biome change bump + timed decay ---
        if ("primian".equals(race)) {
            tickHumanAdaptation(player, now);
        }

        // --- Wind Wyrm ancient presence at low HP (server-side sound emission) ---
        if ("wind_wyrm".equals(race) && player.getHealth() / player.getMaxHealth() < 0.5f
                && player.level() instanceof ServerLevel serverLevel
                && player.tickCount % 40 == 0) {
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.ENDER_DRAGON_AMBIENT,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.25f, 0.7f);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    3, 0.5, 0.8, 0.5, 0.02);
        }
    }

    // ==================== HUMAN ADAPTATION STACKS ====================

    /**
     * Tick-time state machine for Human "Adaptation" stacks.
     * <ul>
     *   <li>Entering a new biome bumps the stack counter up to {@value ADAPT_STACK_CAP}.</li>
     *   <li>{@link #onLivingDeath} also bumps for distinct mob kills (see below).</li>
     *   <li>Each bump resets the decay timestamp. After {@value ADAPT_STACK_DECAY_TICKS}
     *       ticks without a bump, one stack decays.</li>
     *   <li>Transient {@link Attributes#MOVEMENT_SPEED} modifier reflects 1% per stack.</li>
     * </ul>
     */
    private void tickHumanAdaptation(ServerPlayer player, long now) {
        CompoundTag data = player.getPersistentData();
        int stacks = data.getInt(HUMAN_ADAPT_STACKS);
        long lastBump = data.getLong(HUMAN_ADAPT_LAST_TICK);

        // Biome bump: compare against last recorded biome id.
        ResourceLocation biomeId = player.level().getBiome(player.blockPosition())
                .unwrapKey().map(k -> k.location()).orElse(null);
        if (biomeId != null) {
            String current = biomeId.toString();
            if (!current.equals(data.getString(HUMAN_ADAPT_BIOME))) {
                data.putString(HUMAN_ADAPT_BIOME, current);
                if (stacks < ADAPT_STACK_CAP) {
                    stacks++;
                    data.putInt(HUMAN_ADAPT_STACKS, stacks);
                    lastBump = now;
                    data.putLong(HUMAN_ADAPT_LAST_TICK, now);
                }
            }
        }

        // Decay: one stack per full decay window since last bump.
        if (stacks > 0 && now - lastBump >= ADAPT_STACK_DECAY_TICKS) {
            stacks--;
            lastBump = now;
            data.putInt(HUMAN_ADAPT_STACKS, stacks);
            data.putLong(HUMAN_ADAPT_LAST_TICK, now);
        }

        applyAdaptationModifier(player, stacks);
        RaceStateTracker.setFlag(player, RaceStateFlags.ADAPTATION_ACTIVE, stacks > 0);

        // Surface the exact stack count on the client (rendered on the "A" rune). Diff to avoid spam.
        if (data.getInt(HUMAN_ADAPT_SYNCED_STACKS) != stacks) {
            data.putInt(HUMAN_ADAPT_SYNCED_STACKS, stacks);
            NetworkHandler.sendToPlayer(player, new S2CAdaptationStacksPacket(stacks));
        }
    }

    /** Apply (or update / remove) the Human Adaptation speed attribute modifier. */
    private static void applyAdaptationModifier(ServerPlayer player, int stacks) {
        AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        AttributeModifier existing = attr.getModifier(HUMAN_ADAPT_UUID);
        double target = stacks * 0.01; // +1% per stack
        if (target <= 0.0) {
            if (existing != null) attr.removeModifier(HUMAN_ADAPT_UUID);
            return;
        }
        if (existing == null || existing.getAmount() != target) {
            if (existing != null) attr.removeModifier(HUMAN_ADAPT_UUID);
            attr.addTransientModifier(new AttributeModifier(HUMAN_ADAPT_UUID,
                    "Runic Races Human Adaptation", target, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static boolean isFireDamage(DamageSource source) {
        return source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.LAVA)
                || source.is(DamageTypes.HOT_FLOOR);
    }

    // ==================== HUMAN: UNIVERSAL PALATE ====================

    /**
     * Universal Palate: a human's iron stomach. When a human finishes eating, strip any
     * harmful effects that food just applied (rotten flesh Hunger, pufferfish Poison/Nausea)
     * and top up a little extra saturation. Precise by construction — only the effects the
     * eaten food itself declares are removed, right after it is consumed.
     */
    @SubscribeEvent
    public void onUniversalPalate(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!RaceHelper.isRace(player, "primian")) return;

        FoodProperties food = event.getItem().getFoodProperties(player);
        if (food == null) return;

        for (Pair<MobEffectInstance, Float> pair : food.getEffects()) {
            MobEffectInstance effect = pair.getFirst();
            if (effect != null && effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                player.removeEffect(effect.getEffect());
            }
        }

        // Bonus nourishment: small saturation top-up, never above the current food level.
        var foodData = player.getFoodData();
        foodData.setSaturation(Math.min(foodData.getSaturationLevel() + 2.0f, foodData.getFoodLevel()));
    }

    // ==================== VALEN PHYSICAL ====================

    /**
     * Valen: sprinting into a hostile mob delivers a "shoulder check" —
     * extra knockback on the first hit of a sprint. Non-damaging, fantasy-forward.
     */
    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (!attacker.isSprinting()) return;
        if (!RaceHelper.isRace(attacker, "valen")) return;
        net.minecraft.world.entity.LivingEntity target = event.getEntity();
        if (target == attacker) return;

        // Apply extra knockback in the attacker's look direction.
        Vec3 look = attacker.getLookAngle();
        target.push(look.x * 0.6, 0.25, look.z * 0.6);
        target.hurtMarked = true;

        if (attacker.level() instanceof ServerLevel level) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    net.minecraft.sounds.SoundEvents.IRON_GOLEM_ATTACK,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.4f, 0.7f);
        }
    }

    /**
     * Valen: landing from a fall of ≥3 blocks triggers a screen-shake cue
     * for the landing player. Fall damage itself is unaffected.
     */
    @SubscribeEvent
    public void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!RaceHelper.isRace(player, "valen")) return;
        if (event.getDistance() < 3.0f) return;

        NetworkHandler.sendToPlayer(player, new S2CScreenCuePacket(CueType.SHAKE, 5));

        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.IRON_GOLEM_STEP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 0.6f);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    player.getX(), player.getY() + 0.1, player.getZ(),
                    20, 0.8, 0.1, 0.8, 0.05);
        }
    }

    // ==================== JUMP COMPENSATION ====================

    /**
     * Compensates jump height for races scaled down by Pehkui.
     * Boosts Y velocity so all races can clear a 1-block ledge regardless of scale.
     */
    @SubscribeEvent
    public void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!PEHKUI_LOADED) return;

        String race = RaceHelper.getRaceName(player).orElse(null);
        if (race == null) return;

        float scale = RaceHelper.getRaceScale(race);
        if (scale >= 1.0f) return;

        // Multiply Y velocity by inverse of scale to counteract Pehkui's motion scaling.
        // A 0.7-scale player gets 1/0.7 ≈ 1.43x boost, reaching the same world-height as a 1.0-scale player.
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, motion.y / scale, motion.z);
        player.hurtMarked = true; // Sync velocity to client
    }

    // ==================== DATA PERSISTENCE ====================

    /**
     * Copy persistent race data (cooldowns, revival locations) when player
     * clones (death/respawn, end portal, etc).
     */
    /** Keys that should only persist across death clones, not end-portal returns. */
    private static final java.util.Set<String> EPHEMERAL_KEYS = java.util.Set.of(
            RESIZE_PROTECTION_UNTIL, "runic_races:last_synced_race"
    );

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag oldData = event.getOriginal().getPersistentData();
        CompoundTag newData = event.getEntity().getPersistentData();

        for (String key : oldData.getAllKeys()) {
            if (key.startsWith("runic_races:")) {
                // Skip ephemeral keys on non-death clones (e.g. End portal return)
                if (!event.isWasDeath() && EPHEMERAL_KEYS.contains(key)) continue;
                newData.put(key, oldData.get(key).copy());
            }
        }
    }
}
