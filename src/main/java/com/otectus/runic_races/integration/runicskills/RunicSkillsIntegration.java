package com.otectus.runic_races.integration.runicskills;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.integration.ModIntegration;
import com.otectus.runic_races.util.RaceHelper;
import com.otectus.runicskills.common.capability.SkillCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Grants starting skill bonuses from Runic Skills based on the player's race.
 * <p>
 * When a player first logs in with a runic_races origin and their skills are
 * still at fresh defaults (all level 1), racial starting bonuses are applied
 * by directly modifying the {@code skillLevel} map on the skill capability.
 * A persistent NBT tag prevents bonuses from being applied more than once.
 */
public class RunicSkillsIntegration implements ModIntegration {

    private static final String BONUS_APPLIED_TAG = "runic_races:skill_bonus_applied";

    /**
     * Racial starting bonuses. Each entry maps a race name to a map of
     * skill name -> bonus levels to add on top of the default level 1.
     */
    private static final Map<String, Map<String, Integer>> RACIAL_BONUSES = new HashMap<>();

    static {
        RACIAL_BONUSES.put("human",           Map.of("fortune", 1));
        RACIAL_BONUSES.put("halfling",        Map.of("fortune", 1, "dexterity", 1));
        RACIAL_BONUSES.put("nomad",           Map.of("endurance", 1));
        RACIAL_BONUSES.put("giant_blooded",   Map.of("strength", 1, "constitution", 1));
        RACIAL_BONUSES.put("high_elf",        Map.of("magic", 2, "wisdom", 1));
        RACIAL_BONUSES.put("wood_elf",        Map.of("dexterity", 1));
        RACIAL_BONUSES.put("sprite",          Map.of("magic", 2, "fortune", 1));
        RACIAL_BONUSES.put("changeling",      Map.of("dexterity", 1, "intelligence", 1));
        RACIAL_BONUSES.put("dryad",           Map.of("intelligence", 1, "constitution", 1));
        RACIAL_BONUSES.put("wolfkin",         Map.of("dexterity", 1, "constitution", 1));
        RACIAL_BONUSES.put("dragonborn",      Map.of("strength", 1, "endurance", 1));
        RACIAL_BONUSES.put("catfolk",         Map.of("dexterity", 2));
        RACIAL_BONUSES.put("minotaur",        Map.of("strength", 1, "building", 1));
        RACIAL_BONUSES.put("serpentfolk",     Map.of("intelligence", 1, "dexterity", 1));
        RACIAL_BONUSES.put("mountain_dwarf",  Map.of("building", 2, "endurance", 1));
        RACIAL_BONUSES.put("deep_dwarf",      Map.of("endurance", 1, "building", 2));
        RACIAL_BONUSES.put("goblin",          Map.of("tinkering", 2, "fortune", 1));
        RACIAL_BONUSES.put("troll",           Map.of("constitution", 2, "strength", 1));
        RACIAL_BONUSES.put("kobold",          Map.of("tinkering", 1, "building", 1, "intelligence", 1));
        RACIAL_BONUSES.put("wyvern_blooded",  Map.of("dexterity", 1, "endurance", 1));
        RACIAL_BONUSES.put("elder_drake",     Map.of("strength", 2, "constitution", 1));
        RACIAL_BONUSES.put("vampire",         Map.of("dexterity", 1, "intelligence", 1));
        RACIAL_BONUSES.put("lycanthrope",     Map.of("strength", 1, "constitution", 1, "dexterity", 1));
        RACIAL_BONUSES.put("revenant",        Map.of("endurance", 1, "constitution", 1));
    }

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
        RunicRacesMod.LOGGER.info("[RunicRaces] Runic Skills integration initialized");
    }

    @Override
    public String getName() {
        return "Runic Skills";
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Check if bonuses were already applied
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.getBoolean(BONUS_APPLIED_TAG)) return;

        // Check if the player has a runic_races origin
        String raceName = RaceHelper.getRaceName(player).orElse(null);
        if (raceName == null) return;

        Map<String, Integer> bonuses = RACIAL_BONUSES.get(raceName);
        if (bonuses == null || bonuses.isEmpty()) return;

        try {
            SkillCapability skillCap = SkillCapability.get(player);
            if (skillCap == null) return;

            // Only apply if the player's skills look like fresh defaults (all level 1)
            if (!areSkillsAtDefaults(skillCap)) return;

            // Apply racial starting bonuses
            for (Map.Entry<String, Integer> entry : bonuses.entrySet()) {
                String skill = entry.getKey();
                int bonus = entry.getValue();
                int current = skillCap.skillLevel.getOrDefault(skill, 1);
                skillCap.skillLevel.put(skill, current + bonus);
            }

            // Mark bonuses as applied so they are never re-applied
            persistentData.putBoolean(BONUS_APPLIED_TAG, true);

            RunicRacesMod.debug("[RunicRaces] Applied {} skill bonuses for race '{}' to player {}",
                    bonuses.size(), raceName, player.getName().getString());
        } catch (Exception e) {
            RunicRacesMod.LOGGER.error("[RunicRaces] Failed to apply skill bonuses for player {}: {}",
                    player.getName().getString(), e.getMessage());
        }
    }

    /**
     * Checks whether all skills in the capability are still at the default level (1).
     * If any skill has been leveled up, we assume the player is not a fresh character
     * and skip applying racial bonuses to avoid double-dipping.
     */
    private static boolean areSkillsAtDefaults(SkillCapability skillCap) {
        for (int level : skillCap.skillLevel.values()) {
            if (level != 1) return false;
        }
        return true;
    }
}
