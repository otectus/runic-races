package com.otectus.runic_races.integration.irons;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.integration.ModIntegration;
import com.otectus.runic_races.util.RaceHelper;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Integration with Iron's Spellbooks.
 * Modifies outgoing spell damage based on the caster's race.
 * <p>
 * Phase 3: applies flat per-race damage multipliers. School-specific logic
 * (fire, blood, nature, etc.) will be added in a future phase once the
 * spell school can be reliably extracted from SpellDamageSource.
 */
public class IronsSpellsIntegration implements ModIntegration {

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
        RunicRacesMod.LOGGER.info("[RunicRaces] Iron's Spellbooks integration active");
    }

    @Override
    public String getName() {
        return "Iron's Spellbooks";
    }

    // ------------------------------------------------------------------
    // Spell Damage
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onSpellDamage(SpellDamageEvent event) {
        try {
            // The event entity is the victim; the caster is the damage source entity.
            Entity sourceEntity = event.getSpellDamageSource().getEntity();
            if (!(sourceEntity instanceof Player caster)) return;

            String race = RaceHelper.getRaceName(caster).orElse(null);
            if (race == null) return;

            float multiplier = getDamageMultiplier(race);
            if (multiplier != 1.0f) {
                event.setAmount(event.getAmount() * multiplier);
            }
        } catch (Exception e) {
            // Gracefully handle API changes or unexpected nulls
            RunicRacesMod.debug("[RunicRaces] Could not process SpellDamageEvent: {}", e.getMessage());
        }
    }

    /**
     * Flat spell-damage multiplier per race.
     * These are intentionally conservative for Phase 3; school-specific
     * bonuses will layer on top in a later phase.
     */
    private float getDamageMultiplier(String race) {
        return switch (race) {
            case "dragonborn"  -> 1.10f;  // fire-focused, general spell boost
            case "high_elf"    -> 1.10f;  // arcane aptitude
            case "wood_elf"    -> 1.05f;  // nature affinity
            case "vampire"     -> 1.10f;  // blood magic affinity
            case "revenant"    -> 1.08f;  // blood / ender affinity
            case "serpentfolk" -> 1.05f;  // blood / ender affinity
            case "dryad"       -> 1.05f;  // nature affinity
            case "troll"       -> 0.80f;  // poor magical aptitude
            case "elder_drake" -> 0.95f;  // fire strong but others weak, net mild penalty
            default            -> 1.0f;
        };
    }
}
