package com.otectus.runic_races.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.otectus.runic_races.util.ManaHelper;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityCondition;
import net.minecraft.world.entity.Entity;

/**
 * Custom Apoli entity condition: checks if the player has enough Iron's Spellbooks mana.
 * <p>
 * JSON usage: {@code "type": "runic_races:has_mana", "amount": 50}
 * <p>
 * Returns true if Iron's Spellbooks is not installed (graceful degradation).
 */
public class HasManaCondition extends EntityCondition<HasManaCondition.Configuration> {

    public record Configuration(float amount) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.FLOAT.fieldOf("amount").forGetter(Configuration::amount)
                ).apply(instance, Configuration::new)
        );
    }

    public HasManaCondition() {
        super(Configuration.CODEC);
    }

    @Override
    public boolean check(Configuration config, Entity entity) {
        return ManaHelper.hasEnoughMana(entity, config.amount());
    }
}
