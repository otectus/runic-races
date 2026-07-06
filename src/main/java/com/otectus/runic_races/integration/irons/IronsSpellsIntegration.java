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
            case "magi"      -> 1.15f;  // born of raw magic
            case "high_elf"  -> 1.10f;  // arcane aptitude
            case "blood_elf" -> 1.10f;  // blood magic affinity
            case "kitsune"   -> 1.10f;  // fox-spirit sorcery
            case "demon"     -> 1.10f;  // infernal magic
            case "runic_one" -> 1.08f;  // rune-craft
            case "moon_elf"  -> 1.05f;  // silvered spellcraft
            case "nymph"     -> 1.05f;  // nature affinity
            case "wraith"    -> 1.05f;  // soul magic
            case "reaper"    -> 1.05f;  // soul magic
            case "iron_one"  -> 0.90f;  // poor magical aptitude
            case "valen"     -> 0.90f;  // warrior, not mage
            default          -> 1.0f;
        };
    }
}
