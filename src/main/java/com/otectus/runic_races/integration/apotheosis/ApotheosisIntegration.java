package com.otectus.runic_races.integration.apotheosis;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.diagnostics.RRMetrics;
import com.otectus.runic_races.integration.ModIntegration;
import com.otectus.runic_races.race.RaceRegistry;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/** Optional Apotheosis-facing luck contribution, owned by UUID and derived from RaceRegistry. */
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

        double luckBonus = race == null || !com.otectus.runic_races.config.RRServerConfig.APOTHEOSIS_INTEGRATION.get()
                ? 0.0 : RaceRegistry.getLuckBonus(race);

        // Reconcile desired against actual: an unchanged owned modifier is left alone, so a
        // same-race respawn or dimension change sends no attribute update at all.
        AttributeModifier existing = luckAttr.getModifier(RACE_LUCK_UUID);
        if (existing != null && existing.getAmount() == luckBonus
                && existing.getOperation() == AttributeModifier.Operation.ADDITION) {
            return;
        }
        if (existing != null) {
            luckAttr.removeModifier(RACE_LUCK_UUID);
            RRMetrics.add(RRMetrics.Counter.MODIFIER_WRITES);
        }
        if (luckBonus != 0.0) {
            luckAttr.addTransientModifier(new AttributeModifier(
                    RACE_LUCK_UUID,
                    "Runic Races Loot Luck",
                    luckBonus,
                    AttributeModifier.Operation.ADDITION
            ));
            RRMetrics.add(RRMetrics.Counter.MODIFIER_WRITES);
        }
    }

}
