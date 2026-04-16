package com.otectus.runic_races.registry;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.action.ClearEffectsByCategoryAction;
import com.otectus.runic_races.action.ConeBreathAction;
import com.otectus.runic_races.action.ConsumeManaAction;
import com.otectus.runic_races.action.ConsumeStaminaAction;
import com.otectus.runic_races.action.GlowHostilesAction;
import com.otectus.runic_races.action.PlaceTrapAction;
import com.otectus.runic_races.action.ShowBannerAction;
import com.otectus.runic_races.action.SignaturePresentationAction;
import com.otectus.runic_races.action.SummonMinionAction;
import com.otectus.runic_races.action.TremorPingAction;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import io.github.edwinmindcraft.apoli.api.registry.ApoliRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers custom Apoli entity actions for external resource consumption and
 * for the Runic Races presentation/action toolkit (clear effects, cone breath,
 * tremor ping, summon minion, banner, signature presentation).
 */
public class ModEntityActions {

    public static final DeferredRegister<EntityAction<?>> ENTITY_ACTIONS =
            DeferredRegister.create(ApoliRegistries.ENTITY_ACTION_KEY, RunicRacesMod.MOD_ID);

    public static final RegistryObject<ConsumeManaAction> CONSUME_MANA =
            ENTITY_ACTIONS.register("consume_mana", ConsumeManaAction::new);

    public static final RegistryObject<ConsumeStaminaAction> CONSUME_STAMINA =
            ENTITY_ACTIONS.register("consume_stamina", ConsumeStaminaAction::new);

    public static final RegistryObject<ClearEffectsByCategoryAction> CLEAR_EFFECTS_BY_CATEGORY =
            ENTITY_ACTIONS.register("clear_effects_by_category", ClearEffectsByCategoryAction::new);

    public static final RegistryObject<ConeBreathAction> CONE_BREATH =
            ENTITY_ACTIONS.register("cone_breath", ConeBreathAction::new);

    public static final RegistryObject<TremorPingAction> TREMOR_PING =
            ENTITY_ACTIONS.register("tremor_ping", TremorPingAction::new);

    public static final RegistryObject<SummonMinionAction> SUMMON_MINION =
            ENTITY_ACTIONS.register("summon_minion", SummonMinionAction::new);

    public static final RegistryObject<ShowBannerAction> SHOW_BANNER =
            ENTITY_ACTIONS.register("show_banner", ShowBannerAction::new);

    public static final RegistryObject<SignaturePresentationAction> SIGNATURE_PRESENTATION =
            ENTITY_ACTIONS.register("signature_presentation", SignaturePresentationAction::new);

    public static final RegistryObject<GlowHostilesAction> GLOW_HOSTILES =
            ENTITY_ACTIONS.register("glow_hostiles", GlowHostilesAction::new);

    public static final RegistryObject<PlaceTrapAction> PLACE_TRAP =
            ENTITY_ACTIONS.register("place_trap", PlaceTrapAction::new);

    public static void register(IEventBus modBus) {
        ENTITY_ACTIONS.register(modBus);
    }
}
