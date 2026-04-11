package com.otectus.runic_races.registry;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.condition.HasManaCondition;
import com.otectus.runic_races.condition.HasStaminaCondition;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityCondition;
import io.github.edwinmindcraft.apoli.api.registry.ApoliRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers custom Apoli entity conditions for external resource gating.
 */
public class ModEntityConditions {

    public static final DeferredRegister<EntityCondition<?>> ENTITY_CONDITIONS =
            DeferredRegister.create(ApoliRegistries.ENTITY_CONDITION_KEY, RunicRacesMod.MOD_ID);

    public static final RegistryObject<HasManaCondition> HAS_MANA =
            ENTITY_CONDITIONS.register("has_mana", HasManaCondition::new);

    public static final RegistryObject<HasStaminaCondition> HAS_STAMINA =
            ENTITY_CONDITIONS.register("has_stamina", HasStaminaCondition::new);

    public static void register(IEventBus modBus) {
        ENTITY_CONDITIONS.register(modBus);
    }
}
