package com.otectus.runic_races.integration.apotheosis;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.integration.ModIntegration;
import com.otectus.runic_races.race.RaceRegistry;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Apotheosis integration: Modifies loot luck and crafting bonuses based on race.
 *
 * Phase 4 scope:
 * - Goblin: +2 luck attribute (better Apotheosis loot rolls)
 * - Mountain Dwarf: +1 luck when crafting-related (represented as flat luck bonus)
 * - Elder Drake: -2 luck (prideful, scorns trinkets)
 * - Halfling: +1.5 luck (lucky by nature)
 *
 * Forge Blessing (Mountain Dwarf's signature ability that grants Apotheosis affixes
 * on crafted items) requires deep Apotheosis API integration and is deferred to Phase 5.
 * The Appraise ability (Goblin) similarly requires custom UI and is Phase 5+.
 *
 * For now, luck attribute modifiers serve as the mechanical proxy for loot interaction.
 */
public class ApotheosisIntegration implements ModIntegration {

    private static final UUID RACE_LUCK_UUID = UUID.fromString("d2e3f4a5-6789-abcd-ef01-234567890001");

    @Override
    public void init() {
        RunicRacesMod.LOGGER.info("[RunicRaces] Apotheosis integration initialized — loot luck modifiers active");
    }

    @Override
    public String getName() {
        return "Apotheosis";
    }

    @Override
    public void syncPlayer(ServerPlayer player) {
        applyLuckModifier(player);
    }

    private void applyLuckModifier(ServerPlayer player) {
        String race = RaceHelper.getRaceName(player).orElse(null);
        AttributeInstance luckAttr = player.getAttribute(Attributes.LUCK);
        if (luckAttr == null) return;

        // Remove existing racial luck modifier
        luckAttr.removeModifier(RACE_LUCK_UUID);

        if (race == null) return;

        double luckBonus = RaceRegistry.getLuckBonus(race);
        if (luckBonus != 0.0) {
            luckAttr.addTransientModifier(new AttributeModifier(
                    RACE_LUCK_UUID,
                    "Runic Races Loot Luck",
                    luckBonus,
                    AttributeModifier.Operation.ADDITION
            ));
        }
    }

}
