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
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Custom Apoli power: grants attribute modifiers when the player is in specific biomes.
 *
 * JSON usage:
 * {
 *   "type": "runic_races:biome_affinity",
 *   "home_biome_tag": "forge:is_mountain",
 *   "speed_bonus": 0.05,
 *   "damage_bonus": 0.0,
 *   "hostile_biome_tag": "forge:is_ocean",
 *   "speed_penalty": -0.03,
 *   "damage_penalty": 0.0
 * }
 */
public class BiomeAffinityPower extends PowerFactory<BiomeAffinityPower.Configuration> {

    private static final int FLAG_MASK = RaceStateFlags.of(RaceStateFlags.BIOME_HOME, RaceStateFlags.BIOME_HOSTILE);
    private static final ResourceLocation UNNAMED_SOURCE = new ResourceLocation(RunicRacesMod.MOD_ID, "biome_affinity");

    /** One attribute modifier this power may own; {@code home} picks which biome test drives it. */
    public record Role(boolean home, Attribute attribute, UUID uuid, String name, double value) {}

    /**
     * Everything the tick needs, derived once from the immutable configuration: parsed
     * tags, the modifier UUIDs and the roles that can ever apply a non-zero modifier.
     * A reload builds a new configuration, so this is also the configuration generation.
     */
    public static final class Derived {
        private final @Nullable TagKey<Biome> homeTag;
        private final @Nullable TagKey<Biome> hostileTag;
        private final List<Role> roles;
        private final AtomicBoolean tagsChecked = new AtomicBoolean();

        Derived(@Nullable TagKey<Biome> homeTag, @Nullable TagKey<Biome> hostileTag, List<Role> roles) {
            this.homeTag = homeTag;
            this.hostileTag = hostileTag;
            this.roles = roles;
        }

        @Nullable public TagKey<Biome> homeTag() { return homeTag; }
        @Nullable public TagKey<Biome> hostileTag() { return hostileTag; }
        public List<Role> roles() { return roles; }
        AtomicBoolean tagsChecked() { return tagsChecked; }

        // A pure function of the configuration's JSON fields, so it never decides equality.
        @Override public boolean equals(Object o) { return o instanceof Derived; }
        @Override public int hashCode() { return 0; }
        @Override public String toString() { return "Derived" + roles; }
    }

    public record Configuration(
            Optional<String> homeBiomeTag,
            double speedBonus,
            double damageBonus,
            Optional<String> hostileBiomeTag,
            double speedPenalty,
            double damagePenalty,
            int checkInterval,
            Derived derived
    ) implements IDynamicFeatureConfiguration {

        private static final MapCodec<Configuration> RAW = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.STRING.optionalFieldOf("home_biome_tag").forGetter(Configuration::homeBiomeTag),
                        Codec.DOUBLE.optionalFieldOf("speed_bonus", 0.0).forGetter(Configuration::speedBonus),
                        Codec.DOUBLE.optionalFieldOf("damage_bonus", 0.0).forGetter(Configuration::damageBonus),
                        Codec.STRING.optionalFieldOf("hostile_biome_tag").forGetter(Configuration::hostileBiomeTag),
                        Codec.DOUBLE.optionalFieldOf("speed_penalty", 0.0).forGetter(Configuration::speedPenalty),
                        Codec.DOUBLE.optionalFieldOf("damage_penalty", 0.0).forGetter(Configuration::damagePenalty),
                        Codec.INT.optionalFieldOf("check_interval", 20).forGetter(Configuration::checkInterval)
                ).apply(instance, Configuration::new)
        );

        /**
         * Malformed tags and non-finite values fail at load instead of inside every tick.
         * Validated at the MapCodec level: Apoli only merges a factory's fields with the
         * shared power fields when the codec is a {@code MapCodec.MapCodecCodec}.
         */
        public static final Codec<Configuration> CODEC = RAW.flatXmap(Configuration::validate, DataResult::success).codec();

        public Configuration(Optional<String> homeBiomeTag, double speedBonus, double damageBonus,
                             Optional<String> hostileBiomeTag, double speedPenalty, double damagePenalty,
                             int checkInterval) {
            this(homeBiomeTag, speedBonus, damageBonus, hostileBiomeTag, speedPenalty, damagePenalty,
                    checkInterval, derive(homeBiomeTag, speedBonus, damageBonus, hostileBiomeTag, speedPenalty, damagePenalty));
        }

        private static DataResult<Configuration> validate(Configuration config) {
            for (Optional<String> tag : List.of(config.homeBiomeTag, config.hostileBiomeTag)) {
                if (tag.isPresent() && ResourceLocation.tryParse(tag.get()) == null) {
                    return DataResult.error(() -> "biome_affinity: malformed biome tag '" + tag.get() + "'");
                }
            }
            for (double value : new double[]{config.speedBonus, config.damageBonus, config.speedPenalty, config.damagePenalty}) {
                if (!Double.isFinite(value)) {
                    return DataResult.error(() -> "biome_affinity: modifier values must be finite");
                }
            }
            return DataResult.success(config);
        }

        /**
         * Per-role modifier UUID derived from the config so two biome_affinity powers
         * on one entity (with different tags/values) no longer silently overwrite
         * each other. Identical configs share modifiers — intended idempotency.
         */
        public UUID modifierUuid(String role) {
            return legacyUuid(role, homeBiomeTag, hostileBiomeTag, speedBonus, damageBonus, speedPenalty, damagePenalty);
        }
    }

    // Kept byte-for-byte: persisted/stacked modifier identity depends on this exact key.
    private static UUID legacyUuid(String role, Optional<String> homeBiomeTag, Optional<String> hostileBiomeTag,
                                   double speedBonus, double damageBonus, double speedPenalty, double damagePenalty) {
        String key = "runic_races:biome_affinity:" + role
                + ":" + homeBiomeTag.orElse("") + ":" + hostileBiomeTag.orElse("")
                + ":" + speedBonus + ":" + damageBonus + ":" + speedPenalty + ":" + damagePenalty;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private static Derived derive(Optional<String> homeBiomeTag, double speedBonus, double damageBonus,
                                  Optional<String> hostileBiomeTag, double speedPenalty, double damagePenalty) {
        TagKey<Biome> home = tag(homeBiomeTag);
        TagKey<Biome> hostile = tag(hostileBiomeTag);
        List<Role> roles = new ArrayList<>(4);
        // A role with no tag or a zero value can never hold a modifier, so the tick skips it.
        if (home != null) {
            addRole(roles, true, Attributes.MOVEMENT_SPEED, "home_speed", "Runic Races Home Speed", speedBonus,
                    homeBiomeTag, hostileBiomeTag, speedBonus, damageBonus, speedPenalty, damagePenalty);
            addRole(roles, true, Attributes.ATTACK_DAMAGE, "home_damage", "Runic Races Home Damage", damageBonus,
                    homeBiomeTag, hostileBiomeTag, speedBonus, damageBonus, speedPenalty, damagePenalty);
        }
        if (hostile != null) {
            addRole(roles, false, Attributes.MOVEMENT_SPEED, "hostile_speed", "Runic Races Hostile Speed", speedPenalty,
                    homeBiomeTag, hostileBiomeTag, speedBonus, damageBonus, speedPenalty, damagePenalty);
            addRole(roles, false, Attributes.ATTACK_DAMAGE, "hostile_damage", "Runic Races Hostile Damage", damagePenalty,
                    homeBiomeTag, hostileBiomeTag, speedBonus, damageBonus, speedPenalty, damagePenalty);
        }
        return new Derived(home, hostile, List.copyOf(roles));
    }

    private static void addRole(List<Role> roles, boolean home, Attribute attribute, String role, String name, double value,
                                Optional<String> homeBiomeTag, Optional<String> hostileBiomeTag,
                                double speedBonus, double damageBonus, double speedPenalty, double damagePenalty) {
        if (value == 0.0) return;
        roles.add(new Role(home, attribute,
                legacyUuid(role, homeBiomeTag, hostileBiomeTag, speedBonus, damageBonus, speedPenalty, damagePenalty),
                name, value));
    }

    @Nullable
    private static TagKey<Biome> tag(Optional<String> id) {
        if (id.isEmpty()) return null;
        ResourceLocation location = ResourceLocation.tryParse(id.get());
        return location == null ? null : TagKey.create(Registries.BIOME, location);
    }

    public BiomeAffinityPower() {
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

        Derived derived = power.getConfiguration().derived();
        if (derived.homeTag() == null && derived.hostileTag() == null) return;
        warnOnUndefinedTags(derived, player);
        Holder<Biome> biomeHolder = player.level().getBiome(player.blockPosition());

        boolean inHome = derived.homeTag() != null && biomeHolder.is(derived.homeTag());
        boolean inHostile = derived.hostileTag() != null && biomeHolder.is(derived.hostileTag());

        for (Role role : derived.roles()) {
            applyModifier(player, role, role.home() ? inHome : inHostile);
        }

        // Mirror state to the HUD via the race-state tracker (server-side only). This power's
        // view is one contribution; another affinity on the same race cannot erase it.
        if (player instanceof ServerPlayer serverPlayer) {
            int bits = (inHome ? RaceStateFlags.BIOME_HOME.mask() : 0)
                    | (inHostile ? RaceStateFlags.BIOME_HOSTILE.mask() : 0);
            RaceStateTracker.setContribution(serverPlayer, source(power), FLAG_MASK, bits);
        }
    }

    @Override
    public void onRemoved(ConfiguredPower<Configuration, ?> power, Entity entity) {
        if (entity instanceof Player player) {
            Configuration config = power.getConfiguration();
            removeModifier(player, Attributes.MOVEMENT_SPEED, config.modifierUuid("home_speed"));
            removeModifier(player, Attributes.ATTACK_DAMAGE, config.modifierUuid("home_damage"));
            removeModifier(player, Attributes.MOVEMENT_SPEED, config.modifierUuid("hostile_speed"));
            removeModifier(player, Attributes.ATTACK_DAMAGE, config.modifierUuid("hostile_damage"));
            if (player instanceof ServerPlayer serverPlayer) {
                RaceStateTracker.clearContribution(serverPlayer, source(power));
            }
        }
    }

    static ResourceLocation source(ConfiguredPower<?, ?> power) {
        ResourceLocation name = power.getRegistryName();
        return name != null ? name : UNNAMED_SOURCE;
    }

    private static void applyModifier(Player player, Role role, boolean shouldApply) {
        AttributeInstance instance = player.getAttribute(role.attribute());
        if (instance == null) return;

        AttributeModifier existing = instance.getModifier(role.uuid());
        if (shouldApply) {
            if (existing == null) {
                instance.addTransientModifier(new AttributeModifier(role.uuid(), role.name(), role.value(),
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
                RRMetrics.add(RRMetrics.Counter.MODIFIER_WRITES);
            } else if (existing.getAmount() != role.value()) {
                instance.removeModifier(role.uuid());
                instance.addTransientModifier(new AttributeModifier(role.uuid(), role.name(), role.value(),
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
                RRMetrics.add(RRMetrics.Counter.MODIFIER_WRITES, 2);
            }
        } else if (existing != null) {
            instance.removeModifier(role.uuid());
            RRMetrics.add(RRMetrics.Counter.MODIFIER_WRITES);
        }
    }

    /**
     * Warn once per configuration generation: Holder.is() on a tag nothing defines is
     * silently false forever, which reads as "the affinity just never procs" to pack authors.
     */
    private static void warnOnUndefinedTags(Derived derived, Player player) {
        if (derived.tagsChecked().get() || !derived.tagsChecked().compareAndSet(false, true)) return;
        var biomes = player.level().registryAccess().registryOrThrow(Registries.BIOME);
        warnIfUndefined(biomes, derived.homeTag());
        warnIfUndefined(biomes, derived.hostileTag());
    }

    private static void warnIfUndefined(net.minecraft.core.Registry<Biome> biomes, @Nullable TagKey<Biome> tag) {
        if (tag != null && biomes.getTag(tag).isEmpty()) {
            RunicRacesMod.LOGGER.warn(
                    "[RunicRaces] biome_affinity references biome tag '{}' which no datapack defines — "
                            + "this bonus/penalty will never activate. Did you mean a vanilla tag like 'minecraft:is_forest'?",
                    tag.location());
        }
    }

    private static void removeModifier(Player player, Attribute attribute, UUID uuid) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.getModifier(uuid) != null) {
            instance.removeModifier(uuid);
            RRMetrics.add(RRMetrics.Counter.MODIFIER_WRITES);
        }
    }
}
