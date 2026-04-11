package com.otectus.runic_races.integration.spellsngods;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.integration.ModIntegration;
import com.otectus.runic_races.util.RaceHelper;
import com.otectus.spells_n_gods.compat.SpellsNGodsAPI;
import com.otectus.spells_n_gods.compat.SpellsNGodsEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Modifies divine favor gain based on race-god affinity.
 * <p>
 * Certain races have natural affinities (or antipathies) toward specific gods.
 * When a player makes an offering, the favor value is multiplied according to
 * the divine affinity map. Special cases include forbidden combinations (favor
 * set to 0) and family-wide penalties.
 */
public class SpellsNGodsIntegration implements ModIntegration {

    /**
     * Specific race + god affinity multipliers.
     * Key format: "raceName:godId"
     */
    private static final Map<String, Float> DIVINE_AFFINITY = new HashMap<>();

    /**
     * Gods considered to be "light" gods, against which the cursed family
     * receives a penalty.
     */
    private static final Set<String> LIGHT_GODS = Set.of("viren", "meridian", "aurex");

    static {
        // Affinity bonuses
        DIVINE_AFFINITY.put("mountain_dwarf:aurex",  1.20f);
        DIVINE_AFFINITY.put("deep_dwarf:aurex",      1.15f);
        DIVINE_AFFINITY.put("high_elf:nyxara",       1.15f);
        DIVINE_AFFINITY.put("wood_elf:viren",        1.15f);
        DIVINE_AFFINITY.put("dryad:viren",           1.15f);
        DIVINE_AFFINITY.put("wolfkin:khelr",         1.15f);
        DIVINE_AFFINITY.put("minotaur:khelr",        1.15f);
        DIVINE_AFFINITY.put("dragonborn:khelr",      1.10f);
        DIVINE_AFFINITY.put("vampire:mortyss",       1.20f);
        DIVINE_AFFINITY.put("revenant:mortyss",      1.20f);
        DIVINE_AFFINITY.put("goblin:umbriel",        1.15f);
        DIVINE_AFFINITY.put("kobold:meridian",       1.15f);
        DIVINE_AFFINITY.put("human:meridian",        1.10f);

        // Forbidden combination: vampire offering to Viren
        DIVINE_AFFINITY.put("vampire:viren", 0.0f);
    }

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
        RunicRacesMod.LOGGER.info("[RunicRaces] Runic Gods integration initialized");
    }

    @Override
    public String getName() {
        return "Runic Gods";
    }

    @SubscribeEvent
    public void onOffering(SpellsNGodsEvents.OfferingEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) return;

        String raceName = RaceHelper.getRaceName(player).orElse(null);
        if (raceName == null) return;

        String godId = SpellsNGodsAPI.getPlayerGodId(player);
        if (godId == null || godId.isEmpty()) return;

        float multiplier = calculateMultiplier(raceName, godId);

        if (multiplier != 1.0f) {
            float originalFavor = event.getFavorValue();
            event.setFavorValue(originalFavor * multiplier);

            if (multiplier == 0.0f) {
                RunicRacesMod.debug("[RunicRaces] Blocked offering from {} ({}) to {} -- forbidden pairing",
                        player.getName().getString(), raceName, godId);
            } else {
                RunicRacesMod.debug("[RunicRaces] Modified offering for {} ({}) to {}: {}x multiplier",
                        player.getName().getString(), raceName, godId, multiplier);
            }
        }
    }

    /**
     * Determines the favor multiplier for a given race and god combination.
     * <p>
     * Priority order:
     * <ol>
     *   <li>Elder drakes predate the gods -- always 1.0 (no bonus or penalty)</li>
     *   <li>Specific race + god affinity from the lookup table</li>
     *   <li>Cursed family + light god antipathy (-15%)</li>
     *   <li>Default: 1.0 (no modification)</li>
     * </ol>
     */
    private float calculateMultiplier(String raceName, String godId) {
        // Elder drakes predate the gods -- no divine interaction bonus
        if ("elder_drake".equals(raceName)) {
            return 1.0f;
        }

        // Check for specific race + god affinity (includes forbidden combos)
        String key = raceName + ":" + godId;
        Float specific = DIVINE_AFFINITY.get(key);
        if (specific != null) {
            return specific;
        }

        // Cursed family penalty with light gods
        String family = RaceHelper.getRaceFamily(raceName);
        if ("cursed".equals(family) && LIGHT_GODS.contains(godId)) {
            return 0.85f;
        }

        return 1.0f;
    }
}
