package com.otectus.runic_races.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.otectus.runic_races.util.ManaHelper;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.world.entity.Entity;

/**
 * Custom Apoli entity action: consumes Iron's Spellbooks mana from the player.
 * <p>
 * JSON usage: {@code "type": "runic_races:consume_mana", "amount": 50}
 * <p>
 * Does nothing if Iron's Spellbooks is not installed.
 */
public class ConsumeManaAction extends EntityAction<ConsumeManaAction.Configuration> {

    public record Configuration(float amount) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.FLOAT.fieldOf("amount").forGetter(Configuration::amount)
                ).apply(instance, Configuration::new)
        );
    }

    public ConsumeManaAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration config, Entity entity) {
        ManaHelper.consumePlayerMana(entity, config.amount());
    }
}
