package com.otectus.runic_races.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.otectus.runic_races.RunicRacesMod;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom Apoli entity action: apply a list of status effects (and optionally fire)
 * to hostile mobs within {@code radius} blocks of the caster. Hostility uses the
 * same heuristic as {@link GlowHostilesAction} — the {@link Enemy} interface, or a
 * {@link Mob} that is currently targeting something — so party members, pets,
 * villagers, and passive animals are never afflicted.
 * <p>
 * This replaces {@code origins:area_of_effect} for the mod's offensive AoE actives,
 * whose living-only filter debuffed allies alongside enemies.
 * <p>
 * JSON usage:
 * <pre>
 * {
 *   "type": "runic_races:afflict_hostiles",
 *   "radius": 6.0,
 *   "effects": [
 *     {"effect": "minecraft:slowness", "duration_ticks": 80, "amplifier": 1}
 *   ],
 *   "set_on_fire_seconds": 0
 * }
 * </pre>
 */
public class AfflictHostilesAction extends EntityAction<AfflictHostilesAction.Configuration> {

    private static final Set<String> WARNED_EFFECTS = ConcurrentHashMap.newKeySet();

    public record EffectSpec(String effect, int durationTicks, int amplifier) {
        public static final Codec<EffectSpec> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("effect").forGetter(EffectSpec::effect),
                        Codec.INT.optionalFieldOf("duration_ticks", 100).forGetter(EffectSpec::durationTicks),
                        Codec.INT.optionalFieldOf("amplifier", 0).forGetter(EffectSpec::amplifier)
                ).apply(instance, EffectSpec::new)
        );
    }

    public record Configuration(
            double radius,
            List<EffectSpec> effects,
            int setOnFireSeconds
    ) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.DOUBLE.optionalFieldOf("radius", 6.0).forGetter(Configuration::radius),
                        EffectSpec.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(Configuration::effects),
                        Codec.INT.optionalFieldOf("set_on_fire_seconds", 0).forGetter(Configuration::setOnFireSeconds)
                ).apply(instance, Configuration::new)
        );
    }

    public AfflictHostilesAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration config, Entity entity) {
        if (!(entity instanceof LivingEntity caster)) return;
        if (!(caster.level() instanceof ServerLevel level)) return;

        AABB box = caster.getBoundingBox().inflate(config.radius());
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && e.isAlive() && isHostile(e));

        for (LivingEntity target : nearby) {
            for (EffectSpec spec : config.effects()) {
                MobEffect effect = resolveEffect(spec.effect());
                if (effect != null) {
                    target.addEffect(new MobEffectInstance(effect, spec.durationTicks(), spec.amplifier(), false, true));
                }
            }
            if (config.setOnFireSeconds() > 0) {
                target.setSecondsOnFire(config.setOnFireSeconds());
            }
        }
    }

    private static boolean isHostile(LivingEntity entity) {
        if (entity instanceof Enemy) return true;
        return entity instanceof Mob mob && mob.getTarget() != null;
    }

    private static MobEffect resolveEffect(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        MobEffect effect = rl == null ? null : ForgeRegistries.MOB_EFFECTS.getValue(rl);
        if (effect == null && WARNED_EFFECTS.add(id)) {
            RunicRacesMod.LOGGER.warn("[RunicRaces] afflict_hostiles references unknown effect '{}' — skipping", id);
        }
        return effect;
    }
}
