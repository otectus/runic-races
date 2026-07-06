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
import com.otectus.runic_races.race.RaceRegistry;
import com.otectus.runic_races.util.Hostility;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
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
 * - Feline Nine Lives (lethal damage prevention)
 * - Reaper death-site revival
 * - Forge One / Runic One forge blessings
 * - Valen shoulder-check and landing cues
 * - Lightweight-race knockback amplification (LivingKnockBackEvent)
 * - Venomous melee (Arachnid/Serpen) and Blood Elf lifesteal
 * - Brief contact-damage suppression during Pehkui resizes (both growing and shrinking)
 * - Jump height compensation for scaled-down races (Pehkui)
 */
public class RacialEventHandler {

    // Mod presence never changes at runtime — resolve once instead of per-jump.
    private static final boolean PEHKUI_LOADED = ModList.get().isLoaded("pehkui");

    // NBT tag keys for cooldown tracking.
    // NOTE: several key *strings* predate the current race names (vampire/revenant) — they are
    // persisted in player NBT, so the strings stay stable while the Java identifiers use the
    // real race names.
    private static final String ZOMBIE_SUN_TICKS = "runic_races:vampire_sun_ticks";
    private static final String ADAPT_STACKS = "runic_races:human_adapt_stacks";
    private static final String ADAPT_LAST_TICK = "runic_races:human_adapt_last_tick";
    private static final String ADAPT_BIOME = "runic_races:human_adapt_last_biome";
    private static final String ADAPT_SYNCED_STACKS = "runic_races:human_adapt_synced_stacks";
    private static final String ADAPT_LAST_KILL_TYPE = "runic_races:human_adapt_last_kill_type";
    private static final UUID ADAPT_UUID = UUID.fromString("a7b3c8d5-3456-6789-abcd-ef0123456789");
    private static final int ADAPT_STACK_DECAY_TICKS = 1200; // 60s
    private static final int ADAPT_STACK_CAP = 5;
    private static final String REAPER_REVIVAL_COOLDOWN = "runic_races:revenant_revival_cd";
    private static final String REAPER_DEATH_X = "runic_races:death_x";
    private static final String REAPER_DEATH_Y = "runic_races:death_y";
    private static final String REAPER_DEATH_Z = "runic_races:death_z";
    private static final String REAPER_DEATH_DIM = "runic_races:death_dim";
    private static final String RESIZE_PROTECTION_UNTIL = "runic_races:resize_protection_until";
    private static final String VALEN_SHOVE_LATCH = "runic_races:valen_shove_latch";

    // Cooldowns in ticks
    private static final int NINE_LIVES_CD_TICKS = 12000; // 10 minutes
    private static final int REAPER_REVIVAL_CD_TICKS = 36000; // 30 minutes
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

        // === Fragility procs: make "you are the squishy one" audible/visible (Minor tier) ===
        // One shared 3-second debounce so a mob combo doesn't spam the cue.
        if (event.getAmount() > 0.0f && player.level() instanceof ServerLevel level) {
            fireFragilityProc(player, level, race, event);
        }

        // === FELINE: NINE LIVES ===
        if ("feline".equals(race)) {
            // Don't burn the 10-minute charge on deaths it can't prevent (void, /kill):
            // Resistance V doesn't stop invulnerability-bypassing damage, so the player
            // would spend the charge and die a second later anyway.
            if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

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

    private static final String FRAGILITY_CUE_TICK = "runic_races:fragility_cue_tick";

    /**
     * Victim-side weakness cues (Minor tier, ≤8 particles). Each fragile race gets its own
     * "that hurt more than it should" audio-visual language when its weakness actually bites.
     */
    private void fireFragilityProc(ServerPlayer player, ServerLevel level, String race, LivingDamageEvent event) {
        long now = level.getGameTime();
        CompoundTag data = player.getPersistentData();
        if (now - data.getLong(FRAGILITY_CUE_TICK) < 60) return; // shared 3s debounce

        float amount = event.getAmount();
        boolean fall = event.getSource().is(DamageTypeTags.IS_FALL);
        boolean magic = event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
                || event.getSource().is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC);
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        boolean fired = true;
        switch (race) {
            case "high_elf" -> {
                // Fragile Grace: a heavy hit rings like struck crystal.
                if (amount < 5.0f) { fired = false; break; }
                level.playSound(null, x, y, z, net.minecraft.sounds.SoundEvents.AMETHYST_CLUSTER_BREAK,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.5f);
                NetworkHandler.sendToPlayer(player, new S2CScreenCuePacket(CueType.VIGNETTE_PULSE, 8));
            }
            case "arachnid" -> {
                // Fragile Carapace: chitin cracks under a heavy blow.
                if (amount < 5.0f) { fired = false; break; }
                level.playSound(null, x, y, z, net.minecraft.sounds.SoundEvents.TURTLE_EGG_CRACK,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 0.6f);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, x, y, z, 6, 0.3, 0.3, 0.3, 0.1);
            }
            case "skeleton" -> {
                // Brittle Bones: chips fly on falls and heavy hits.
                if (!fall && amount < 5.0f) { fired = false; break; }
                level.playSound(null, x, y, z, net.minecraft.sounds.SoundEvents.BONE_BLOCK_BREAK,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.2f);
                level.sendParticles(com.otectus.runic_races.registry.ModParticles.BONE_CHIP.get(),
                        x, y, z, 6, 0.3, 0.3, 0.3, 0.08);
            }
            case "avian" -> {
                // Hollow Bones: feathers burst loose when the landing goes wrong.
                if (!fall) { fired = false; break; }
                level.playSound(null, x, y, z, net.minecraft.sounds.SoundEvents.BONE_BLOCK_BREAK,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.35f, 1.5f);
                level.sendParticles(com.otectus.runic_races.registry.ModParticles.FEATHER_DOWN.get(),
                        x, y, z, 8, 0.4, 0.4, 0.4, 0.05);
            }
            case "sprite" -> {
                // Fragile Essence: the gossamer form scatters sparks and cries out.
                if (amount < 4.0f) { fired = false; break; }
                level.playSound(null, x, y, z, net.minecraft.sounds.SoundEvents.BAT_HURT,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.4f, 1.6f);
                level.sendParticles(com.otectus.runic_races.registry.ModParticles.FAE_SPARKLE.get(),
                        x, y, z, 6, 0.4, 0.4, 0.4, 0.1);
            }
            case "celeron" -> {
                // Featherweight Frame: knocked around hard enough to shake feathers loose.
                if (amount < 4.0f) { fired = false; break; }
                level.sendParticles(com.otectus.runic_races.registry.ModParticles.FEATHER_DOWN.get(),
                        x, y, z, 5, 0.4, 0.4, 0.4, 0.06);
            }
            case "demon" -> {
                // Holy Vulnerability: sanctified magic flares gold and tolls a bell.
                if (!magic) { fired = false; break; }
                level.playSound(null, x, y, z, net.minecraft.sounds.SoundEvents.BELL_BLOCK,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.4f, 2.0f);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, x, y, z, 6, 0.3, 0.4, 0.3, 0.05);
            }
            default -> fired = false;
        }
        if (fired) {
            data.putLong(FRAGILITY_CUE_TICK, now);
        }
    }

    // ==================== REAPER: DEATH-SITE REVIVAL ====================

    /**
     * On death, if the player is a Reaper and revival is off cooldown,
     * record the death location for revival on respawn.
     */
    @SubscribeEvent
    public void onAnyLivingDeath(LivingDeathEvent event) {
        // Primian Adaptation: bump a stack when a kill is a *different* mob type than the
        // last one — farming a single spawner doesn't stack. Separate @SubscribeEvent so it
        // doesn't compete with the Reaper priority hook below.
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        if (!RaceHelper.isRace(killer, "primian")) return;

        CompoundTag data = killer.getPersistentData();
        ResourceLocation killedId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                .getKey(event.getEntity().getType());
        String killedType = killedId == null ? "" : killedId.toString();
        if (killedType.equals(data.getString(ADAPT_LAST_KILL_TYPE))) return;
        data.putString(ADAPT_LAST_KILL_TYPE, killedType);

        int stacks = data.getInt(ADAPT_STACKS);
        if (stacks < ADAPT_STACK_CAP) {
            data.putInt(ADAPT_STACKS, stacks + 1);
            data.putLong(ADAPT_LAST_TICK, killer.level().getGameTime());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!RaceHelper.isRace(player, "reaper")) return;

        CompoundTag data = player.getPersistentData();
        long now = player.level().getGameTime();

        // Only record the death site here. The cooldown is stamped on *successful* revival
        // (onPlayerRespawn) so a failed attempt (no safe spot, missing dimension) doesn't
        // burn 30 minutes for nothing.
        if (isCooldownReady(data, REAPER_REVIVAL_COOLDOWN, now, REAPER_REVIVAL_CD_TICKS)) {
            data.putDouble(REAPER_DEATH_X, player.getX());
            data.putDouble(REAPER_DEATH_Y, player.getY());
            data.putDouble(REAPER_DEATH_Z, player.getZ());
            data.putString(REAPER_DEATH_DIM, player.level().dimension().location().toString());

            RunicRacesMod.debug("[RunicRaces] Reaper death recorded for {} at ({}, {}, {})",
                    player.getName().getString(), player.getX(), player.getY(), player.getZ());
        }
    }

    /**
     * On respawn, if the Reaper has a recorded death site, teleport them there
     * with reduced health and temporary invulnerability instead of normal respawn.
     */
    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!RaceHelper.isRace(player, "reaper")) return;

        CompoundTag data = player.getPersistentData();
        if (!data.contains(REAPER_DEATH_X)) return;

        double x = data.getDouble(REAPER_DEATH_X);
        double y = data.getDouble(REAPER_DEATH_Y);
        double z = data.getDouble(REAPER_DEATH_Z);
        String dim = data.getString(REAPER_DEATH_DIM);

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
                // Success is what consumes the 30-minute cooldown.
                data.putLong(REAPER_REVIVAL_COOLDOWN, player.level().getGameTime());

                RunicRacesMod.debug("[RunicRaces] Reaper {} revived at safe position ({}, {}, {})",
                        player.getName().getString(), safePos.x, safePos.y, safePos.z);
            } else {
                RunicRacesMod.LOGGER.warn("[RunicRaces] No safe revival position for Reaper {} near ({}, {}, {}), using normal respawn",
                        player.getName().getString(), x, y, z);
                RunicPresentation.fire(player, SignatureKey.REAPER_REVIVAL_REJECTED);
            }
        } else {
            RunicRacesMod.LOGGER.warn("[RunicRaces] Could not restore Reaper {} to death dimension '{}'",
                    player.getName().getString(), dim);
            RunicPresentation.fire(player, SignatureKey.REAPER_REVIVAL_REJECTED);
        }

        data.remove(REAPER_DEATH_X);
        data.remove(REAPER_DEATH_Y);
        data.remove(REAPER_DEATH_Z);
        data.remove(REAPER_DEATH_DIM);
    }

    // ==================== MOUNTAIN DWARF: FORGE BLESSING ====================

    private static final String FORGE_BLESSING_COOLDOWN = "runic_races:forge_blessing_cd";
    private static final int FORGE_BLESSING_CD_TICKS = 12000; // 10 minutes
    private static final float FORGE_BLESSING_CHANCE = 0.25f; // 25% chance

    /**
     * Forge One's Forge Blessing crafting proc: when crafting items, there is a 25% chance
     * the item receives a bonus enchantment (Unbreaking I-II) representing
     * superior dwarven craftsmanship. 10-minute cooldown between procs.
     *
     * Runic Ones get a weaker version (15% chance, Unbreaking I only).
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

        // Apply Forge Blessing: add Unbreaking enchantment. Skip items that already carry
        // Unbreaking — ItemStack.enchant appends, so upgrading would leave a duplicate entry.
        int level = isForgeOne ? (player.getRandom().nextFloat() < 0.3f ? 2 : 1) : 1;
        int existing = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, crafted);
        if (existing == 0) {
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

        // --- Zombie sunlight audio cues + HUD flag (the burn damage itself is JSON-driven) ---
        if ("zombie".equals(race)) {
            boolean exposedToSun = player.level().isDay()
                    && player.level().canSeeSky(player.blockPosition())
                    && !player.isInWaterOrRain();
            CompoundTag data = player.getPersistentData();
            int sunTicks = data.getInt(ZOMBIE_SUN_TICKS);
            if (exposedToSun) {
                int newTicks = sunTicks + 10;
                data.putInt(ZOMBIE_SUN_TICKS, newTicks);
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
                if (sunTicks > 0) data.putInt(ZOMBIE_SUN_TICKS, 0);
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
            boolean wasBurning = RaceStateFlags.SUNLIGHT_BURNING.isSet(RaceStateTracker.get(player));
            RaceStateTracker.setFlag(player, RaceStateFlags.SUNLIGHT_BURNING, burning);
            // Grave-touched flesh sizzles the moment the sun finds it (dark_elf/reaper share
            // the zombie's audio language at lower volume — deep_one/skeleton are dazzled, not burned).
            if (burning && !wasBurning && ("dark_elf".equals(race) || "reaper".equals(race))
                    && player.level() instanceof ServerLevel level) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.2f, 1.4f);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                        player.getX(), player.getY() + 1.2, player.getZ(),
                        4, 0.2, 0.3, 0.2, 0.02);
            }
        }

        // --- Sky One / Wind Wyrm claustrophobia (tight-space) ---
        // Mirrors the JSON penalty condition exactly (!exposed_to_sky): any roof at any
        // height counts, so the warning rune lights whenever the debuff is actually live.
        if ("sky_one".equals(race) || "wind_wyrm".equals(race)) {
            boolean tight = !player.level().canSeeSky(player.blockPosition());
            RaceStateTracker.setFlag(player, RaceStateFlags.TIGHT_SPACE, tight);
        }

        // --- Valen shoulder-check latch: re-arm once the sprint ends ---
        if ("valen".equals(race) && !player.isSprinting()) {
            CompoundTag data = player.getPersistentData();
            if (data.getBoolean(VALEN_SHOVE_LATCH)) {
                data.putBoolean(VALEN_SHOVE_LATCH, false);
            }
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

        // --- Submerged-weakness readout (mirrors origins:submerged_in weakness conditions) ---
        // iron_one/sky_one sink like stones and drown fast; feline/volt_drake take amplified damage.
        if ("feline".equals(race) || "volt_drake".equals(race)
                || "iron_one".equals(race) || "sky_one".equals(race)) {
            boolean submerged = player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
            boolean wasSubmerged = RaceStateFlags.SUBMERGED_WEAK.isSet(RaceStateTracker.get(player));
            RaceStateTracker.setFlag(player, RaceStateFlags.SUBMERGED_WEAK, submerged);
            // Storm scales short-circuit the moment they go under.
            if ("volt_drake".equals(race) && submerged && !wasSubmerged
                    && player.level() instanceof ServerLevel level) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        8, 0.4, 0.5, 0.4, 0.1);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.8f);
            }
        }

        // --- Dry-land sluggishness readout (Sea Serpen landbound coils, Nymph bound-to-water) ---
        if ("sea_serpen".equals(race)) {
            boolean dry = !player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
            RaceStateTracker.setFlag(player, RaceStateFlags.DRY_SLUGGISH, dry);
        }
        if ("nymph".equals(race)) {
            RaceStateTracker.setFlag(player, RaceStateFlags.DRY_SLUGGISH, !player.isInWaterOrRain());
        }

        // --- Canine ravenous hunger readout ---
        if ("canine".equals(race)) {
            boolean starving = player.getFoodData().getFoodLevel() < 6;
            RaceStateTracker.setFlag(player, RaceStateFlags.RAVENOUS, starving);
            // Stomach growl about every 30 seconds while running on empty.
            if (starving && now % 600 < 10 && player.level() instanceof ServerLevel level) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.WOLF_GROWL,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.25f, 0.6f);
            }
        }

        // --- Faerie cold-iron grip readout (iron tool/weapon in either hand) ---
        if ("faerie".equals(race)) {
            boolean gripping = isColdIron(player.getMainHandItem()) || isColdIron(player.getOffhandItem());
            boolean wasGripping = RaceStateFlags.COLD_IRON_GRIP.isSet(RaceStateTracker.get(player));
            RaceStateTracker.setFlag(player, RaceStateFlags.COLD_IRON_GRIP, gripping);
            if (gripping && !wasGripping && player.level() instanceof ServerLevel level) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.LAVA_EXTINGUISH,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.25f, 1.7f);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        3, 0.2, 0.2, 0.2, 0.02);
            }
        }

        // --- Blood Elf: the price of power — heartbeat cue while critically wounded ---
        if ("blood_elf".equals(race) && player.getHealth() / player.getMaxHealth() < 0.2f
                && now % 100 < 10) {
            NetworkHandler.sendToPlayer(player, new S2CScreenCuePacket(CueType.HEARTBEAT_FLASH, 15));
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
        int stacks = data.getInt(ADAPT_STACKS);
        long lastBump = data.getLong(ADAPT_LAST_TICK);

        // Biome bump: compare against last recorded biome id.
        ResourceLocation biomeId = player.level().getBiome(player.blockPosition())
                .unwrapKey().map(k -> k.location()).orElse(null);
        if (biomeId != null) {
            String current = biomeId.toString();
            if (!current.equals(data.getString(ADAPT_BIOME))) {
                data.putString(ADAPT_BIOME, current);
                if (stacks < ADAPT_STACK_CAP) {
                    stacks++;
                    data.putInt(ADAPT_STACKS, stacks);
                    lastBump = now;
                    data.putLong(ADAPT_LAST_TICK, now);
                }
            }
        }

        // Decay: one stack per full decay window since last bump.
        if (stacks > 0 && now - lastBump >= ADAPT_STACK_DECAY_TICKS) {
            stacks--;
            lastBump = now;
            data.putInt(ADAPT_STACKS, stacks);
            data.putLong(ADAPT_LAST_TICK, now);
        }

        applyAdaptationModifier(player, stacks);
        RaceStateTracker.setFlag(player, RaceStateFlags.ADAPTATION_ACTIVE, stacks > 0);

        // Surface the exact stack count on the client (rendered on the "A" rune). Diff to avoid spam.
        if (data.getInt(ADAPT_SYNCED_STACKS) != stacks) {
            data.putInt(ADAPT_SYNCED_STACKS, stacks);
            NetworkHandler.sendToPlayer(player, new S2CAdaptationStacksPacket(stacks));
        }
    }

    /** Apply (or update / remove) the Human Adaptation speed attribute modifier. */
    private static void applyAdaptationModifier(ServerPlayer player, int stacks) {
        AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        AttributeModifier existing = attr.getModifier(ADAPT_UUID);
        double target = stacks * 0.01; // +1% per stack
        if (target <= 0.0) {
            if (existing != null) attr.removeModifier(ADAPT_UUID);
            return;
        }
        if (existing == null || existing.getAmount() != target) {
            if (existing != null) attr.removeModifier(ADAPT_UUID);
            attr.addTransientModifier(new AttributeModifier(ADAPT_UUID,
                    "Runic Races Human Adaptation", target, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
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
     * extra knockback on the first melee hit of a sprint. Non-damaging, fantasy-forward.
     * Gated three ways: direct melee only (no projectiles), hostile targets only
     * (no players/pets/villagers), and once per sprint (latch cleared in the player
     * tick watcher when sprinting stops).
     */
    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (event.getSource().getDirectEntity() != attacker) return; // melee only, no arrows/tridents
        if (!attacker.isSprinting()) return;
        LivingEntity target = event.getEntity();
        if (target == attacker || !Hostility.isThreatTo(attacker, target)) return;
        if (!RaceHelper.isRace(attacker, "valen")) return;

        CompoundTag data = attacker.getPersistentData();
        if (data.getBoolean(VALEN_SHOVE_LATCH)) return; // one shove per sprint
        data.putBoolean(VALEN_SHOVE_LATCH, true);

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

    /** Faerie cold-iron heuristic: any item whose registry path names iron (sword, tools, armor, ingot). */
    private static boolean isColdIron(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getPath().contains("iron");
    }

    // ==================== KNOCKBACK & MELEE RIDERS ====================

    /**
     * Lightweight races take amplified knockback. Implemented here because a negative
     * {@code generic.knockback_resistance} attribute modifier clamps to the attribute's
     * [0,1] range and silently does nothing.
     */
    @SubscribeEvent
    public void onLivingKnockBack(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        String race = RaceHelper.getRaceName(player).orElse(null);
        if (race == null) return;

        float multiplier = RaceRegistry.getKnockbackTaken(race);
        if (multiplier != 1.0f) {
            event.setStrength(event.getStrength() * multiplier);
        }
    }

    /**
     * Direct-melee riders keyed off {@link RaceRegistry} metadata:
     * venomous races (Arachnid, Serpen) inject Poison I; Blood Elves leech 20% of
     * damage dealt (their own -30% modify_healing weakness taxes the returned heal).
     */
    @SubscribeEvent
    public void onMeleeDamageDealt(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (event.getSource().getDirectEntity() != attacker) return; // melee only
        LivingEntity target = event.getEntity();
        if (target == attacker || event.getAmount() <= 0.0f) return;

        String race = RaceHelper.getRaceName(attacker).orElse(null);
        if (race == null) return;

        if (RaceRegistry.isVenomous(race)) {
            // addEffect returns false on poison-immune targets (undead, other venomous races) —
            // only show the venom landing when it actually took hold. Minor tier: 6 particles.
            boolean applied = target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            if (applied && attacker.level() instanceof ServerLevel level) {
                level.sendParticles(com.otectus.runic_races.registry.ModParticles.VENOM_DRIP.get(),
                        target.getX(), target.getY(0.6), target.getZ(),
                        6, 0.3, 0.3, 0.3, 0.02);
                // Serpen strikes hiss higher than the arachnid's skittering bite.
                level.playSound(null, target.getX(), target.getY(), target.getZ(),
                        net.minecraft.sounds.SoundEvents.SPIDER_STEP,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.4f, "serpen".equals(race) ? 1.6f : 0.9f);
            }
        }

        if ("blood_elf".equals(race)) {
            // Lifesteal proc cue only when the heal actually matters (attacker is wounded).
            boolean wounded = attacker.getHealth() < attacker.getMaxHealth();
            attacker.heal(event.getAmount() * 0.2f);
            if (wounded && attacker.level() instanceof ServerLevel level) {
                // Visible siphon: crimson droplets on a line from the victim into the elf.
                Vec3 from = new Vec3(target.getX(), target.getY(0.6), target.getZ());
                Vec3 to = new Vec3(attacker.getX(), attacker.getY(0.8), attacker.getZ());
                Vec3 dir = to.subtract(from);
                for (int i = 0; i < 5; i++) {
                    Vec3 p = from.add(dir.scale(i / 4.0));
                    level.sendParticles(com.otectus.runic_races.presentation.RaceColors.CRIMSON_BLOOD,
                            p.x, p.y, p.z, 0,
                            dir.x, dir.y, dir.z, 0.05);
                }
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR,
                        target.getX(), target.getY(0.5), target.getZ(),
                        3, 0.2, 0.2, 0.2, 0.1);
                level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                        net.minecraft.sounds.SoundEvents.HONEY_DRINK,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.3f, 1.4f);
            }
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
