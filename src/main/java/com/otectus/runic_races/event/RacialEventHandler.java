package com.otectus.runic_races.event;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Central Forge event handler for racial mechanics that operate at the engine level,
 * outside of what Origins data-driven powers can express.
 *
 * Handles:
 * - Catfolk Nine Lives (lethal damage prevention)
 * - Revenant death-site revival
 * - Revenant healing inversion
 * - Vampire enhanced sun feedback
 * - Halfling lucky dodge enhancement
 */
public class RacialEventHandler {

    // NBT tag keys for cooldown tracking
    private static final String NINE_LIVES_COOLDOWN = "runic_races:nine_lives_cd";
    private static final String REVENANT_REVIVAL_COOLDOWN = "runic_races:revenant_revival_cd";
    private static final String REVENANT_DEATH_X = "runic_races:death_x";
    private static final String REVENANT_DEATH_Y = "runic_races:death_y";
    private static final String REVENANT_DEATH_Z = "runic_races:death_z";
    private static final String REVENANT_DEATH_DIM = "runic_races:death_dim";

    // Cooldowns in ticks
    private static final int NINE_LIVES_CD_TICKS = 12000; // 10 minutes
    private static final int REVENANT_REVIVAL_CD_TICKS = 36000; // 30 minutes

    // ==================== CATFOLK: NINE LIVES ====================

    /**
     * Intercepts lethal damage for Catfolk. If the player would die and Nine Lives
     * is off cooldown, cancel the death: set health to 1, grant brief invulnerability,
     * and start the cooldown.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        String race = RaceHelper.getRaceName(player).orElse(null);
        if (race == null) return;

        // === CATFOLK: NINE LIVES ===
        if ("catfolk".equals(race)) {
            float currentHealth = player.getHealth();
            float incomingDamage = event.getAmount();

            // Would this kill or nearly kill the player?
            if (currentHealth - incomingDamage <= 0.0f) {
                CompoundTag data = player.getPersistentData();
                long lastUsed = data.getLong(NINE_LIVES_COOLDOWN);
                long now = player.level().getGameTime();

                if (now - lastUsed >= NINE_LIVES_CD_TICKS) {
                    // Trigger Nine Lives: cancel the lethal damage
                    event.setAmount(0);
                    player.setHealth(2.0f); // 1 heart
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 4)); // Resistance V, 2s
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1)); // Regen II, 3s
                    data.putLong(NINE_LIVES_COOLDOWN, now);

                    player.sendSystemMessage(Component.literal("\u00A76\u00A7lA life is spent... but you persist!"));
                    RunicRacesMod.LOGGER.debug("[RunicRaces] Nine Lives triggered for {}", player.getName().getString());
                }
            }
        }

        // === REVENANT: HEALING INVERSION ===
        // Revenant takes more damage from healing-type sources (handled in onHeal below)
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
        long lastRevival = data.getLong(REVENANT_REVIVAL_COOLDOWN);
        long now = player.level().getGameTime();

        if (now - lastRevival >= REVENANT_REVIVAL_CD_TICKS) {
            // Record death position for revival
            data.putDouble(REVENANT_DEATH_X, player.getX());
            data.putDouble(REVENANT_DEATH_Y, player.getY());
            data.putDouble(REVENANT_DEATH_Z, player.getZ());
            data.putString(REVENANT_DEATH_DIM, player.level().dimension().location().toString());
            data.putLong(REVENANT_REVIVAL_COOLDOWN, now);

            RunicRacesMod.LOGGER.debug("[RunicRaces] Revenant death recorded for {} at ({}, {}, {})",
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

        // Clean up the stored position
        data.remove(REVENANT_DEATH_X);
        data.remove(REVENANT_DEATH_Y);
        data.remove(REVENANT_DEATH_Z);
        data.remove(REVENANT_DEATH_DIM);

        // Only teleport if same dimension
        if (dim.equals(player.level().dimension().location().toString())) {
            player.teleportTo((ServerLevel) player.level(), x, y, z, player.getYRot(), player.getXRot());
            player.setHealth(6.0f); // 3 hearts
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 4)); // Resistance V, 30s
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600)); // Visible to others
            player.sendSystemMessage(Component.literal("\u00A75\u00A7lDeath cannot hold you. You rise where you fell."));

            RunicRacesMod.LOGGER.debug("[RunicRaces] Revenant {} revived at death site ({}, {}, {})",
                    player.getName().getString(), x, y, z);
        }
    }

    // ==================== REVENANT: HEALING INVERSION ====================

    /**
     * Revenants receive reduced healing from all sources (50% reduction).
     * This represents their undead nature conflicting with positive life energy.
     */
    @SubscribeEvent
    public void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!RaceHelper.isRace(player, "revenant")) return;

        // Reduce all healing by 50%
        event.setAmount(event.getAmount() * 0.5f);
    }

    // ==================== VAMPIRE: BLOOD VIAL TRACKING ====================

    /**
     * Vampires receive reduced healing from food (they need blood, not bread).
     * Full food restriction is Phase 5+; for now, food heals 50% less.
     */
    @SubscribeEvent
    public void onVampireHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!RaceHelper.isRace(player, "vampire")) return;

        // Natural regen (from saturation) is reduced for vampires
        // The player still regenerates but more slowly
        if (event.getAmount() <= 1.0f) { // Natural regen ticks are small amounts
            event.setAmount(event.getAmount() * 0.3f);
        }
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
        long lastProc = data.getLong(FORGE_BLESSING_COOLDOWN);
        long now = player.level().getGameTime();

        if (now - lastProc < FORGE_BLESSING_CD_TICKS) return;

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
            RunicRacesMod.LOGGER.debug("[RunicRaces] Forge Blessing triggered for {} — Unbreaking {} on {}",
                    player.getName().getString(), level, crafted.getDisplayName().getString());
        }
    }

    private static String toRoman(int n) {
        return switch (n) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; default -> String.valueOf(n); };
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
