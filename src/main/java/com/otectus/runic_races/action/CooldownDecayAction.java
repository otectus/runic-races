package com.otectus.runic_races.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.otectus.runic_races.network.CooldownSync;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Custom Apoli entity action: one decay step of a Runic Races cooldown resource.
 * <p>
 * The mutation is exactly {@code origins:change_resource}'s additive path — same resource
 * object, same {@code change}, same ownership check, run on the same authored
 * {@code action_over_time} interval. Only the synchronization differs: instead of
 * re-sending the owner's whole power container to them and every tracker after each step,
 * {@link CooldownSync} coalesces the changed values into one small update per entity per
 * tick, and falls back to the full container sync for any resource whose change could
 * touch other powers.
 * <pre>
 * {
 *   "type": "runic_races:cooldown_decay",
 *   "resource": "runic_races:fire_drake/dragonfire_breath_cooldown_timer",
 *   "change": -10
 * }
 * </pre>
 */
public class CooldownDecayAction extends EntityAction<CooldownDecayAction.Configuration> {

    public record Configuration(Holder<ConfiguredPower<?, ?>> resource, int change) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        ConfiguredPower.CODEC_SET.holderRef().fieldOf("resource").forGetter(Configuration::resource),
                        CalioCodecHelper.INT.fieldOf("change").forGetter(Configuration::change)
                ).apply(instance, Configuration::new)
        );

        @Override
        public boolean isConfigurationValid() {
            return change != 0;
        }
    }

    public CooldownDecayAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration config, Entity entity) {
        if (!(entity instanceof LivingEntity living)) return;
        Holder<ConfiguredPower<?, ?>> resource = config.resource();
        if (!resource.isBound()) return;
        boolean held = IPowerContainer.get(entity).resolve()
                .flatMap(container -> resource.unwrapKey().map(container::hasPower))
                .orElse(false);
        if (!held) return;
        resource.value().change(entity, config.change());
        CooldownSync.changed(living, resource);
    }
}
