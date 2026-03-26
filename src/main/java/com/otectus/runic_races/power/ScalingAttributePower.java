package com.otectus.runic_races.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Custom Apoli power: applies different attribute modifiers based on time of day.
 * Useful for races with day/night power variations (vampires, moon elves, etc).
 *
 * JSON usage:
 * {
 *   "type": "runic_races:scaling_attribute",
 *   "attribute": "generic.attack_damage",
 *   "day_value": -0.10,
 *   "night_value": 0.20,
 *   "operation": "multiply_total"
 * }
 */
public class ScalingAttributePower extends PowerFactory<ScalingAttributePower.Configuration> {

    private static final UUID SCALING_UUID = UUID.fromString("b8c4d9e2-2345-5678-9abc-def012345678");

    public record Configuration(
            String attribute,
            double dayValue,
            double nightValue,
            String operation,
            int checkInterval
    ) implements IDynamicFeatureConfiguration {

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("attribute").forGetter(Configuration::attribute),
                        Codec.DOUBLE.optionalFieldOf("day_value", 0.0).forGetter(Configuration::dayValue),
                        Codec.DOUBLE.optionalFieldOf("night_value", 0.0).forGetter(Configuration::nightValue),
                        Codec.STRING.optionalFieldOf("operation", "multiply_total").forGetter(Configuration::operation),
                        Codec.INT.optionalFieldOf("check_interval", 20).forGetter(Configuration::checkInterval)
                ).apply(instance, Configuration::new)
        );
    }

    public ScalingAttributePower() {
        super(Configuration.CODEC);
    }

    @Override
    public boolean canTick(ConfiguredPower<Configuration, ?> power, Entity entity) {
        return entity instanceof Player && entity.tickCount % power.getConfiguration().checkInterval() == 0;
    }

    @Override
    public void tick(ConfiguredPower<Configuration, ?> power, Entity entity) {
        if (!(entity instanceof Player player)) return;

        Configuration config = power.getConfiguration();
        boolean isDaytime = player.level().isDay();
        double value = isDaytime ? config.dayValue() : config.nightValue();

        net.minecraft.world.entity.ai.attributes.Attribute attr = resolveAttribute(config.attribute());
        if (attr == null) return;

        AttributeModifier.Operation op = resolveOperation(config.operation());
        AttributeInstance instance = player.getAttribute(attr);
        if (instance == null) return;

        AttributeModifier existing = instance.getModifier(SCALING_UUID);
        if (value != 0.0) {
            if (existing == null || existing.getAmount() != value) {
                if (existing != null) instance.removeModifier(SCALING_UUID);
                instance.addTransientModifier(new AttributeModifier(
                        SCALING_UUID, "Runic Races Scaling", value, op));
            }
        } else if (existing != null) {
            instance.removeModifier(SCALING_UUID);
        }
    }

    @Override
    public void onRemoved(ConfiguredPower<Configuration, ?> power, Entity entity) {
        if (!(entity instanceof Player player)) return;
        Configuration config = power.getConfiguration();
        net.minecraft.world.entity.ai.attributes.Attribute attr = resolveAttribute(config.attribute());
        if (attr == null) return;
        AttributeInstance instance = player.getAttribute(attr);
        if (instance != null) instance.removeModifier(SCALING_UUID);
    }

    private net.minecraft.world.entity.ai.attributes.Attribute resolveAttribute(String name) {
        return switch (name) {
            case "generic.attack_damage" -> Attributes.ATTACK_DAMAGE;
            case "generic.movement_speed" -> Attributes.MOVEMENT_SPEED;
            case "generic.max_health" -> Attributes.MAX_HEALTH;
            case "generic.armor" -> Attributes.ARMOR;
            case "generic.attack_speed" -> Attributes.ATTACK_SPEED;
            case "generic.knockback_resistance" -> Attributes.KNOCKBACK_RESISTANCE;
            case "generic.luck" -> Attributes.LUCK;
            default -> null;
        };
    }

    private AttributeModifier.Operation resolveOperation(String name) {
        return switch (name) {
            case "addition" -> AttributeModifier.Operation.ADDITION;
            case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            default -> AttributeModifier.Operation.MULTIPLY_TOTAL;
        };
    }
}
