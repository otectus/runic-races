package com.otectus.runic_races.event;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.util.RaceHelper;
import com.otectus.runic_races.util.OriginsPowerHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

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

    // NBT tag keys for cooldown tracking
    private static final String REVENANT_REVIVAL_COOLDOWN = "runic_races:revenant_revival_cd";
    private static final String REVENANT_DEATH_X = "runic_races:death_x";
    private static final String REVENANT_DEATH_Y = "runic_races:death_y";
    private static final String REVENANT_DEATH_Z = "runic_races:death_z";
    private static final String REVENANT_DEATH_DIM = "runic_races:death_dim";
    private static final String RESIZE_PROTECTION_UNTIL = "runic_races:resize_protection_until";

    // Cooldowns in ticks
    private static final int NINE_LIVES_CD_TICKS = 12000; // 10 minutes
    private static final int REVENANT_REVIVAL_CD_TICKS = 36000; // 30 minutes
    private static final ResourceLocation NINE_LIVES_RESOURCE = new ResourceLocation(RunicRacesMod.MOD_ID, "catfolk/nine_lives_cooldown_timer");

    // ==================== CATFOLK: NINE LIVES ====================

    /**
     * Intercepts lethal damage for Catfolk. If the player would die and Nine Lives
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

        // === CATFOLK: NINE LIVES ===
        if ("catfolk".equals(race)) {
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

                    player.sendSystemMessage(Component.literal("\u00A76\u00A7lA life is spent... but you persist!"));
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
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!RaceHelper.isRace(player, "revenant")) return;

        CompoundTag data = player.getPersistentData();
        long now = player.level().getGameTime();

        if (isCooldownReady(data, REVENANT_REVIVAL_COOLDOWN, now, REVENANT_REVIVAL_CD_TICKS)) {
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
        if (!RaceHelper.isRace(player, "revenant")) return;

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
            player.teleportTo(targetLevel, x, y, z, player.getYRot(), player.getXRot());
            player.setHealth(6.0f); // 3 hearts
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 4)); // Resistance V, 30s
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600)); // Visible to others
            player.sendSystemMessage(Component.literal("\u00A75\u00A7lDeath cannot hold you. You rise where you fell."));

            RunicRacesMod.debug("[RunicRaces] Revenant {} revived at death site ({}, {}, {})",
                    player.getName().getString(), x, y, z);
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

        boolean isMountainDwarf = "mountain_dwarf".equals(race);
        boolean isDeepDwarf = "deep_dwarf".equals(race);
        if (!isMountainDwarf && !isDeepDwarf) return;

        ItemStack crafted = event.getCrafting();
        if (crafted.isEmpty() || !crafted.isEnchantable()) return;

        CompoundTag data = player.getPersistentData();
        long now = player.level().getGameTime();

        if (!isCooldownReady(data, FORGE_BLESSING_COOLDOWN, now, FORGE_BLESSING_CD_TICKS)) return;

        float chance = isMountainDwarf ? FORGE_BLESSING_CHANCE : 0.15f;
        if (player.getRandom().nextFloat() >= chance) return;

        // Apply Forge Blessing: add Unbreaking enchantment
        int level = isMountainDwarf ? (player.getRandom().nextFloat() < 0.3f ? 2 : 1) : 1;
        int existing = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, crafted);
        if (existing < level) {
            crafted.enchant(Enchantments.UNBREAKING, level);
            data.putLong(FORGE_BLESSING_COOLDOWN, now);
            player.sendSystemMessage(Component.literal(
                    "\u00A76\u00A7lThe forge blesses your work! \u00A7r\u00A77Unbreaking " + toRoman(level) + " applied."));
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


    // ==================== JUMP COMPENSATION ====================

    /**
     * Compensates jump height for races scaled down by Pehkui.
     * Boosts Y velocity so all races can clear a 1-block ledge regardless of scale.
     */
    @SubscribeEvent
    public void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ModList.get().isLoaded("pehkui")) return;

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
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag oldData = event.getOriginal().getPersistentData();
        CompoundTag newData = event.getEntity().getPersistentData();

        // Copy all runic_races persistent data
        for (String key : oldData.getAllKeys()) {
            if (key.startsWith("runic_races:")) {
                newData.put(key, oldData.get(key).copy());
            }
        }
    }
}
