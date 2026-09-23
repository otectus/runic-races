package com.otectus.runic_races.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.common.state.RaceStateFlags;
import com.otectus.runic_races.common.state.RaceStateTracker;
import com.otectus.runic_races.diagnostics.RRMetrics;
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

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
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

    private static final int FLAG_MASK = RaceStateFlags.NIGHT_EMPOWERED.mask();
    private static final ResourceLocation UNNAMED_SOURCE = new ResourceLocation(RunicRacesMod.MOD_ID, "scaling_attribute");

    /**
     * Values derived once from the immutable configuration. The attribute itself is
     * resolved on first use — registries are frozen by then — and a failed lookup is
     * remembered, so an unknown attribute warns once per configuration generation
     * instead of on every check interval.
     */
    public static final class Derived {
        final UUID uuid;
        final ResourceLocation attributeId;
        final AttributeModifier.Operation operation;
        // Benign race: both logical sides resolve the same registry value.
        private volatile Optional<Attribute> attribute;

        Derived(UUID uuid, ResourceLocation attributeId, AttributeModifier.Operation operation) {
            this.uuid = uuid;
            this.attributeId = attributeId;
            this.operation = operation;
        }

        @Nullable
        Attribute attribute() {
            Optional<Attribute> resolved = attribute;
            if (resolved == null) {
                Attribute found = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
                if (found == null) {
                    RunicRacesMod.LOGGER.warn("[RunicRaces] Unknown attribute '{}' in ScalingAttributePower — is the target mod loaded?", attributeId);
                }
                resolved = Optional.ofNullable(found);
                attribute = resolved;
            }
            return resolved.orElse(null);
        }

        // Identity-free: the derived values are a pure function of the configuration.
        @Override public boolean equals(Object o) { return o instanceof Derived; }
        @Override public int hashCode() { return 0; }
        @Override public String toString() { return "Derived[" + uuid + "]"; }
    }

    public record Configuration(
            String attribute,
            double dayValue,
            double nightValue,
            String operation,
            int checkInterval,
            boolean requireSkyExposure,
            Derived derived
    ) implements IDynamicFeatureConfiguration {

        private static final MapCodec<Configuration> RAW = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.STRING.fieldOf("attribute").forGetter(Configuration::attribute),
                        Codec.DOUBLE.optionalFieldOf("day_value", 0.0).forGetter(Configuration::dayValue),
                        Codec.DOUBLE.optionalFieldOf("night_value", 0.0).forGetter(Configuration::nightValue),
                        Codec.STRING.optionalFieldOf("operation", "multiply_total").forGetter(Configuration::operation),
                        Codec.INT.optionalFieldOf("check_interval", 20).forGetter(Configuration::checkInterval),
                        Codec.BOOL.optionalFieldOf("require_sky_exposure", false).forGetter(Configuration::requireSkyExposure)
                ).apply(instance, Configuration::new)
        );

        /**
         * A malformed attribute id or a non-finite value fails at load instead of inside every tick.
         * Validated at the MapCodec level: Apoli only merges a factory's fields with the
         * shared power fields when the codec is a {@code MapCodec.MapCodecCodec}.
         */
        public static final Codec<Configuration> CODEC = RAW.flatXmap(Configuration::validate, DataResult::success).codec();

        public Configuration(String attribute, double dayValue, double nightValue, String operation,
                             int checkInterval, boolean requireSkyExposure) {
            this(attribute, dayValue, nightValue, operation, checkInterval, requireSkyExposure,
                    derive(attribute, dayValue, nightValue, operation));
        }

        private static DataResult<Configuration> validate(Configuration config) {
            if (config.derived.attributeId == null) {
                return DataResult.error(() -> "scaling_attribute: malformed attribute id '" + config.attribute + "'");
            }
            if (!Double.isFinite(config.dayValue) || !Double.isFinite(config.nightValue)) {
                return DataResult.error(() -> "scaling_attribute: day_value/night_value must be finite");
            }
            return DataResult.success(config);
        }

        /**
         * Modifier UUID derived from the config so two scaling powers on the same
         * attribute (with different values) no longer silently overwrite each other.
         * Identical configs still share one modifier — intended idempotency.
         */
        public UUID modifierUuid() {
            return derived.uuid;
        }
    }

    // Kept byte-for-byte: modifier identity depends on this exact key.
    private static Derived derive(String attribute, double dayValue, double nightValue, String operation) {
        String key = "runic_races:scaling:" + attribute + ":" + operation
                + ":" + dayValue + ":" + nightValue;
        UUID uuid = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
        return new Derived(uuid, ResourceLocation.tryParse(attribute), resolveOperation(operation));
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

        Derived derived = config.derived();
        Attribute attr = derived.attribute();
        if (attr == null) return;

        AttributeInstance instance = player.getAttribute(attr);
        if (instance == null) return;

        AttributeModifier existing = instance.getModifier(derived.uuid);
        if (value != 0.0) {
            if (existing == null || existing.getAmount() != value) {
                if (existing != null) instance.removeModifier(derived.uuid);
                instance.addTransientModifier(new AttributeModifier(
                        derived.uuid, "Runic Races Scaling", value, derived.operation));
                RRMetrics.add(RRMetrics.Counter.MODIFIER_WRITES, existing == null ? 1 : 2);
            }
        } else if (existing != null) {
            instance.removeModifier(derived.uuid);
            RRMetrics.add(RRMetrics.Counter.MODIFIER_WRITES);
        }

        // "Night empowered" = not daytime AND the night value is the stronger (non-zero) side.
        // Used by the HUD state-rune overlay to signal to night-empowered races that their buff is live.
        if (player instanceof ServerPlayer serverPlayer) {
            boolean nightEmpowered = !isDaytime && config.nightValue() > 0.0;
            RaceStateTracker.setContribution(serverPlayer, source(power), FLAG_MASK, nightEmpowered ? FLAG_MASK : 0);
        }
    }

    @Override
    public void onRemoved(ConfiguredPower<Configuration, ?> power, Entity entity) {
        if (!(entity instanceof Player player)) return;
        Configuration config = power.getConfiguration();
        if (player instanceof ServerPlayer serverPlayer) {
            RaceStateTracker.clearContribution(serverPlayer, source(power));
        }
        Attribute attr = config.derived().attribute();
        if (attr == null) return;
        AttributeInstance instance = player.getAttribute(attr);
        if (instance != null && instance.getModifier(config.modifierUuid()) != null) {
            instance.removeModifier(config.modifierUuid());
            RRMetrics.add(RRMetrics.Counter.MODIFIER_WRITES);
        }
    }

    private static ResourceLocation source(ConfiguredPower<?, ?> power) {
        ResourceLocation name = power.getRegistryName();
        return name != null ? name : UNNAMED_SOURCE;
    }

    private static AttributeModifier.Operation resolveOperation(String name) {
        return switch (name) {
            case "addition" -> AttributeModifier.Operation.ADDITION;
            case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            default -> AttributeModifier.Operation.MULTIPLY_TOTAL;
        };
    }
}
