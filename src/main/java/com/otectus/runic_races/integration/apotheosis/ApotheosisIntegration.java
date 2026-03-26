package com.otectus.runic_races.integration.apotheosis;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.integration.ModIntegration;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
        MinecraftForge.EVENT_BUS.register(this);
        RunicRacesMod.LOGGER.info("[RunicRaces] Apotheosis integration initialized — loot luck modifiers active");
    }

    @Override
    public String getName() {
        return "Apotheosis";
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        applyLuckModifier(player);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        applyLuckModifier(player);
    }

    @SubscribeEvent
    public void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        applyLuckModifier(player);
    }

    private void applyLuckModifier(ServerPlayer player) {
        String race = RaceHelper.getRaceName(player).orElse(null);
        AttributeInstance luckAttr = player.getAttribute(Attributes.LUCK);
        if (luckAttr == null) return;

        // Remove existing racial luck modifier
        luckAttr.removeModifier(RACE_LUCK_UUID);

        if (race == null) return;

        double luckBonus = getLuckBonus(race);
        if (luckBonus != 0.0) {
            luckAttr.addPermanentModifier(new AttributeModifier(
                    RACE_LUCK_UUID,
                    "Runic Races Loot Luck",
                    luckBonus,
                    AttributeModifier.Operation.ADDITION
            ));
        }
    }

    private double getLuckBonus(String race) {
        return switch (race) {
            case "goblin" -> 2.0;       // Best treasure hunter
            case "halfling" -> 1.5;     // Lucky by nature
            case "mountain_dwarf" -> 1.0; // Craftsman's eye
            case "deep_dwarf" -> 0.5;   // Underground treasures
            case "human" -> 0.5;        // Adaptable fortune
            case "elder_drake" -> -2.0; // Too proud for trinkets
            case "troll" -> -1.0;       // Oblivious to quality
            default -> 0.0;
        };
    }
}
