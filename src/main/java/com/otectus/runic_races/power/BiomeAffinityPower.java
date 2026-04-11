package com.otectus.runic_races.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    private static final UUID HOME_SPEED_UUID = UUID.fromString("a7b3c8d1-1234-4567-89ab-cdef01234567");
    private static final UUID HOME_DAMAGE_UUID = UUID.fromString("a7b3c8d2-1234-4567-89ab-cdef01234567");
    private static final UUID HOSTILE_SPEED_UUID = UUID.fromString("a7b3c8d3-1234-4567-89ab-cdef01234567");
    private static final UUID HOSTILE_DAMAGE_UUID = UUID.fromString("a7b3c8d4-1234-4567-89ab-cdef01234567");
    private static final ConcurrentHashMap<String, TagKey<Biome>> TAG_CACHE = new ConcurrentHashMap<>();

    public record Configuration(
            Optional<String> homeBiomeTag,
            double speedBonus,
            double damageBonus,
            Optional<String> hostileBiomeTag,
            double speedPenalty,
            double damagePenalty,
            int checkInterval
    ) implements IDynamicFeatureConfiguration {

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
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
    }

    public BiomeAffinityPower() {
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
        Holder<Biome> biomeHolder = player.level().getBiome(player.blockPosition());

        boolean inHome = config.homeBiomeTag().isPresent() &&
                biomeHolder.is(resolveTag(config.homeBiomeTag().get()));

        boolean inHostile = config.hostileBiomeTag().isPresent() &&
                biomeHolder.is(resolveTag(config.hostileBiomeTag().get()));

        applyModifier(player, Attributes.MOVEMENT_SPEED, HOME_SPEED_UUID,
                "Runic Races Home Speed", inHome ? config.speedBonus() : 0.0, inHome);
        applyModifier(player, Attributes.ATTACK_DAMAGE, HOME_DAMAGE_UUID,
                "Runic Races Home Damage", inHome ? config.damageBonus() : 0.0, inHome);
        applyModifier(player, Attributes.MOVEMENT_SPEED, HOSTILE_SPEED_UUID,
                "Runic Races Hostile Speed", inHostile ? config.speedPenalty() : 0.0, inHostile);
        applyModifier(player, Attributes.ATTACK_DAMAGE, HOSTILE_DAMAGE_UUID,
                "Runic Races Hostile Damage", inHostile ? config.damagePenalty() : 0.0, inHostile);
    }

    @Override
    public void onRemoved(ConfiguredPower<Configuration, ?> power, Entity entity) {
        if (entity instanceof Player player) {
            removeModifier(player, Attributes.MOVEMENT_SPEED, HOME_SPEED_UUID);
            removeModifier(player, Attributes.ATTACK_DAMAGE, HOME_DAMAGE_UUID);
            removeModifier(player, Attributes.MOVEMENT_SPEED, HOSTILE_SPEED_UUID);
            removeModifier(player, Attributes.ATTACK_DAMAGE, HOSTILE_DAMAGE_UUID);
        }
    }

    private void applyModifier(Player player, net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                UUID uuid, String name, double value, boolean shouldApply) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        AttributeModifier existing = instance.getModifier(uuid);
        if (shouldApply && value != 0.0) {
            if (existing == null) {
                instance.addTransientModifier(new AttributeModifier(uuid, name, value,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
            } else if (existing.getAmount() != value) {
                instance.removeModifier(uuid);
                instance.addTransientModifier(new AttributeModifier(uuid, name, value,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        } else if (existing != null) {
            instance.removeModifier(uuid);
        }
    }

    private static TagKey<Biome> resolveTag(String tagString) {
        return TAG_CACHE.computeIfAbsent(tagString,
                s -> TagKey.create(net.minecraft.core.registries.Registries.BIOME, new ResourceLocation(s)));
    }

    private void removeModifier(Player player, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID uuid) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(uuid);
        }
    }
}
