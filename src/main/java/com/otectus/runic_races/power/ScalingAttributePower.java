package com.otectus.runic_races.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.common.state.RaceStateFlags;
import com.otectus.runic_races.common.state.RaceStateTracker;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/**
 * Custom Apoli power: applies different attribute modifiers based on time of day.
 * Useful for races with day/night power variations (dark elves, wraiths, demons, etc).
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
            int checkInterval,
            boolean requireSkyExposure
    ) implements IDynamicFeatureConfiguration {

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("attribute").forGetter(Configuration::attribute),
                        Codec.DOUBLE.optionalFieldOf("day_value", 0.0).forGetter(Configuration::dayValue),
                        Codec.DOUBLE.optionalFieldOf("night_value", 0.0).forGetter(Configuration::nightValue),
                        Codec.STRING.optionalFieldOf("operation", "multiply_total").forGetter(Configuration::operation),
                        Codec.INT.optionalFieldOf("check_interval", 20).forGetter(Configuration::checkInterval),
                        Codec.BOOL.optionalFieldOf("require_sky_exposure", false).forGetter(Configuration::requireSkyExposure)
                ).apply(instance, Configuration::new)
        );
    }

    public ScalingAttributePower() {
        super(Configuration.CODEC);
    }

    @Override
    public boolean canTick(ConfiguredPower<Configuration, ?> power, Entity entity) {
        // Clamp: a datapack "check_interval": 0 must not become a modulo-by-zero server crash.
        int interval = Math.max(1, power.getConfiguration().checkInterval());
        return entity instanceof Player && entity.tickCount % interval == 0;
    }

    @Override
    public void tick(ConfiguredPower<Configuration, ?> power, Entity entity) {
        if (!(entity instanceof Player player)) return;

        Configuration config = power.getConfiguration();
        boolean isDaytime = player.level().isDay();
        double value;
        if (isDaytime) {
            // With require_sky_exposure, the day penalty only bites under open sky —
            // a sun-averse race sheltering underground or indoors is spared.
            boolean exposed = !config.requireSkyExposure()
                    || player.level().canSeeSky(player.blockPosition());
            value = exposed ? config.dayValue() : 0.0;
        } else {
            value = config.nightValue();
        }

        Attribute attr = resolveAttribute(config.attribute());
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

        // "Night empowered" = not daytime AND the night value is the stronger (non-zero) side.
        // Used by the HUD state-rune overlay to signal to night-empowered races that their buff is live.
        if (player instanceof ServerPlayer serverPlayer) {
            boolean nightEmpowered = !isDaytime && config.nightValue() > 0.0;
            RaceStateTracker.setFlag(serverPlayer, RaceStateFlags.NIGHT_EMPOWERED, nightEmpowered);
        }
    }

    @Override
    public void onRemoved(ConfiguredPower<Configuration, ?> power, Entity entity) {
        if (!(entity instanceof Player player)) return;
        Configuration config = power.getConfiguration();
        Attribute attr = resolveAttribute(config.attribute());
        if (attr == null) return;
        AttributeInstance instance = player.getAttribute(attr);
        if (instance != null) instance.removeModifier(SCALING_UUID);
        if (player instanceof ServerPlayer serverPlayer) {
            RaceStateTracker.setFlag(serverPlayer, RaceStateFlags.NIGHT_EMPOWERED, false);
        }
    }

    private Attribute resolveAttribute(String name) {
        ResourceLocation rl = ResourceLocation.tryParse(name);
        if (rl == null) {
            RunicRacesMod.LOGGER.warn("[RunicRaces] Invalid attribute name '{}' in ScalingAttributePower config", name);
            return null;
        }
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(rl);
        if (attr == null) {
            RunicRacesMod.LOGGER.warn("[RunicRaces] Unknown attribute '{}' in ScalingAttributePower — is the target mod loaded?", rl);
        }
        return attr;
    }

    private AttributeModifier.Operation resolveOperation(String name) {
        return switch (name) {
            case "addition" -> AttributeModifier.Operation.ADDITION;
            case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            default -> AttributeModifier.Operation.MULTIPLY_TOTAL;
        };
    }
}
