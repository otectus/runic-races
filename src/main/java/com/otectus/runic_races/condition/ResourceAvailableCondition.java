package com.otectus.runic_races.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.otectus.runic_races.util.ManaHelper;
import com.otectus.runic_races.util.StaminaHelper;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityCondition;
import net.minecraft.world.entity.Entity;

/**
 * Custom Apoli entity condition: checks whether an external resource system is
 * actually present and usable (mod installed and its API bound via reflection).
 * <p>
 * JSON usage: {@code "type": "runic_races:resource_available", "resource": "mana"}
 * ({@code "mana"} = Iron's Spellbooks, {@code "stamina"} = Feathers).
 * <p>
 * Unlike {@code has_mana}/{@code has_stamina}, this condition is independent of
 * {@code failClosedWhenResourceModMissing} — it answers "is the system here at all?"
 * so datapacks can wrap resource gates to degrade gracefully on standalone installs
 * (see {@code powers/magi/arcane_overflow.json} for the shipped pattern).
 */
public class ResourceAvailableCondition extends EntityCondition<ResourceAvailableCondition.Configuration> {

    public record Configuration(String resource) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("resource").forGetter(Configuration::resource)
                ).apply(instance, Configuration::new)
        );
    }

    public ResourceAvailableCondition() {
        super(Configuration.CODEC);
    }

    @Override
    public boolean check(Configuration config, Entity entity) {
        return switch (config.resource()) {
            case "mana" -> ManaHelper.isAvailable();
            case "stamina" -> StaminaHelper.isAvailable();
            default -> false;
        };
    }
}
