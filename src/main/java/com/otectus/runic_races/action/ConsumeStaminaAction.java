package com.otectus.runic_races.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.otectus.runic_races.util.StaminaHelper;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.world.entity.Entity;

/**
 * Custom Apoli entity action: consumes Feather's Mod stamina from the player.
 * <p>
 * JSON usage: {@code "type": "runic_races:consume_stamina", "amount": 3}
 * <p>
 * Does nothing if Feather's Mod is not installed.
 */
public class ConsumeStaminaAction extends EntityAction<ConsumeStaminaAction.Configuration> {

    public record Configuration(int amount) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.fieldOf("amount").forGetter(Configuration::amount)
                ).apply(instance, Configuration::new)
        );
    }

    public ConsumeStaminaAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration config, Entity entity) {
        StaminaHelper.consumePlayerStamina(entity, config.amount());
    }
}
