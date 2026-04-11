package com.otectus.runic_races.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.otectus.runic_races.util.StaminaHelper;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityCondition;
import net.minecraft.world.entity.Entity;

/**
 * Custom Apoli entity condition: checks if the player has enough Feather's Mod stamina.
 * <p>
 * JSON usage: {@code "type": "runic_races:has_stamina", "amount": 3}
 * <p>
 * Returns true if Feather's Mod is not installed (graceful degradation).
 */
public class HasStaminaCondition extends EntityCondition<HasStaminaCondition.Configuration> {

    public record Configuration(int amount) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.fieldOf("amount").forGetter(Configuration::amount)
                ).apply(instance, Configuration::new)
        );
    }

    public HasStaminaCondition() {
        super(Configuration.CODEC);
    }

    @Override
    public boolean check(Configuration config, Entity entity) {
        return StaminaHelper.hasEnoughStamina(entity, config.amount());
    }
}
