package com.otectus.runic_races.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Custom Apoli entity action: remove all active {@link MobEffectInstance}s whose
 * {@link MobEffectCategory} matches the configured category.
 * <p>
 * JSON usage: {@code "type": "runic_races:clear_effects_by_category", "category": "harmful"}
 * <p>
 * Accepted category values: {@code "harmful"}, {@code "beneficial"}, {@code "neutral"}, {@code "all"}.
 * Unknown values default to {@code "harmful"}. Replaces command-spam effect clears
 * in abilities like Serpentfolk Shed Skin.
 */
public class ClearEffectsByCategoryAction extends EntityAction<ClearEffectsByCategoryAction.Configuration> {

    public record Configuration(String category) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.optionalFieldOf("category", "harmful").forGetter(Configuration::category)
                ).apply(instance, Configuration::new)
        );
    }

    public ClearEffectsByCategoryAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration config, Entity entity) {
        if (!(entity instanceof LivingEntity living)) return;

        String cat = config.category().toLowerCase(Locale.ROOT);
        boolean all = "all".equals(cat);
        MobEffectCategory target = switch (cat) {
            case "beneficial" -> MobEffectCategory.BENEFICIAL;
            case "neutral" -> MobEffectCategory.NEUTRAL;
            default -> MobEffectCategory.HARMFUL;
        };

        // Collect first, then remove, to avoid ConcurrentModificationException.
        List<MobEffectInstance> toRemove = new ArrayList<>();
        for (MobEffectInstance effect : living.getActiveEffects()) {
            if (all || effect.getEffect().getCategory() == target) {
                toRemove.add(effect);
            }
        }
        for (MobEffectInstance effect : toRemove) {
            living.removeEffect(effect.getEffect());
        }
    }
}
