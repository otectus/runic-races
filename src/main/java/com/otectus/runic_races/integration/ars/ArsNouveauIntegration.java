package com.otectus.runic_races.integration.ars;

import com.hollingsworth.arsnouveau.api.event.MaxManaCalcEvent;
import com.hollingsworth.arsnouveau.api.event.SpellCostCalcEvent;
import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.integration.ModIntegration;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Integration with Ars Nouveau.
 * Modifies max mana and spell costs based on the player's race.
 */
public class ArsNouveauIntegration implements ModIntegration {

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
        RunicRacesMod.LOGGER.info("[RunicRaces] Ars Nouveau integration active");
    }

    @Override
    public String getName() {
        return "Ars Nouveau";
    }

    // ------------------------------------------------------------------
    // Max Mana
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onMaxManaCalc(MaxManaCalcEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        String race = RaceHelper.getRaceName(player).orElse(null);
        if (race == null) return;

        double multiplier = getManaMultiplier(race, player);
        if (multiplier != 1.0) {
            event.setMax((int) (event.getMax() * multiplier));
        }
    }

    /**
     * Determine the max-mana multiplier for the given race.
     * Specific race overrides take priority over family defaults.
     */
    private double getManaMultiplier(String race, Player player) {
        // Specific race overrides first
        return switch (race) {
            case "high_elf"    -> 1.20;  // +20% (overrides fae family default)
            case "sprite"      -> 1.20;  // +20%
            case "dryad"       -> 1.10;  // +10%
            case "troll"       -> 0.85;  // -15%
            case "elder_drake" -> 0.90;  // -10%
            default -> {
                // Fall back to family defaults
                String family = RaceHelper.getRaceFamily(race);
                yield switch (family) {
                    case "fae"   -> 1.15;  // +15%
                    case "beast" -> 0.95;  // -5%
                    default      -> 1.0;
                };
            }
        };
    }

    // ------------------------------------------------------------------
    // Spell Cost
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onSpellCostCalc(SpellCostCalcEvent event) {
        try {
            LivingEntity caster = event.context.getUnwrappedCaster();
            if (!(caster instanceof Player player)) return;

            String race = RaceHelper.getRaceName(player).orElse(null);
            if (race == null) return;

            double multiplier = getCostMultiplier(race, player);
            if (multiplier != 1.0) {
                event.currentCost = (int) Math.max(0, event.currentCost * multiplier);
            }
        } catch (Exception e) {
            // Gracefully handle API changes or unexpected nulls
            RunicRacesMod.debug("[RunicRaces] Could not process SpellCostCalcEvent: {}", e.getMessage());
        }
    }

    /**
     * Determine the spell-cost multiplier for the given race.
     * Specific race overrides take priority over family defaults.
     */
    private double getCostMultiplier(String race, Player player) {
        return switch (race) {
            case "high_elf"    -> 0.85;  // -15% (overrides fae family default)
            case "troll"       -> 1.30;  // +30%
            case "elder_drake" -> 1.20;  // +20%
            default -> {
                String family = RaceHelper.getRaceFamily(race);
                yield switch (family) {
                    case "fae"   -> 0.90;  // -10%
                    case "beast" -> 1.05;  // +5%
                    default      -> 1.0;
                };
            }
        };
    }
}
