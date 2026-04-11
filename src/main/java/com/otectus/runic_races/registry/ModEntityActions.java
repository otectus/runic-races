package com.otectus.runic_races.registry;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.action.ConsumeManaAction;
import com.otectus.runic_races.action.ConsumeStaminaAction;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import io.github.edwinmindcraft.apoli.api.registry.ApoliRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers custom Apoli entity actions for external resource consumption.
 */
public class ModEntityActions {

    public static final DeferredRegister<EntityAction<?>> ENTITY_ACTIONS =
            DeferredRegister.create(ApoliRegistries.ENTITY_ACTION_KEY, RunicRacesMod.MOD_ID);

    public static final RegistryObject<ConsumeManaAction> CONSUME_MANA =
            ENTITY_ACTIONS.register("consume_mana", ConsumeManaAction::new);

    public static final RegistryObject<ConsumeStaminaAction> CONSUME_STAMINA =
            ENTITY_ACTIONS.register("consume_stamina", ConsumeStaminaAction::new);

    public static void register(IEventBus modBus) {
        ENTITY_ACTIONS.register(modBus);
    }
}
