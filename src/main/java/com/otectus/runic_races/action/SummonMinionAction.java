package com.otectus.runic_races.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.otectus.runic_races.RunicRacesMod;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;

/**
 * Custom Apoli entity action: summon an entity as a short-lived minion tied to the caster.
 * The summoned entity is tagged via persistent NBT with the owner UUID and an expiry
 * game-time. The actual ownership AI lives on the summoned entity class
 * (e.g. {@code GraveServantEntity}); this action just handles spawning and tagging.
 * <p>
 * JSON usage:
 * <pre>
 * {
 *   "type": "runic_races:summon_minion",
 *   "entity": "runic_races:grave_servant",
 *   "count": 2,
 *   "duration_ticks": 1200,
 *   "radius": 3.0
 * }
 * </pre>
 * <p>
 * If the entity id does not resolve, the action logs a warning and no-ops.
 */
public class SummonMinionAction extends EntityAction<SummonMinionAction.Configuration> {

    public static final String OWNER_UUID_TAG = "runic_races:minion_owner";
    public static final String EXPIRY_TAG = "runic_races:minion_expires_at";

    public record Configuration(
            String entity,
            int count,
            int durationTicks,
            double radius
    ) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("entity").forGetter(Configuration::entity),
                        Codec.INT.optionalFieldOf("count", 1).forGetter(Configuration::count),
                        Codec.INT.optionalFieldOf("duration_ticks", 1200).forGetter(Configuration::durationTicks),
                        Codec.DOUBLE.optionalFieldOf("radius", 2.5).forGetter(Configuration::radius)
                ).apply(instance, Configuration::new)
        );
    }

    public SummonMinionAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration config, Entity caster) {
        if (!(caster instanceof LivingEntity livingCaster)) return;
        if (!(caster.level() instanceof ServerLevel level)) return;

        ResourceLocation entityId = ResourceLocation.tryParse(config.entity());
        if (entityId == null) {
            RunicRacesMod.LOGGER.warn("[RunicRaces] summon_minion: invalid entity id '{}'", config.entity());
            return;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
        if (type == null) {
            RunicRacesMod.LOGGER.warn("[RunicRaces] summon_minion: entity type '{}' is not registered", entityId);
            return;
        }

        long expiresAt = level.getGameTime() + Math.max(1, config.durationTicks());
        int count = Math.max(1, config.count());
        double radius = Math.max(0.5, config.radius());

        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 * i) / count;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;
            BlockPos spawnPos = BlockPos.containing(caster.getX() + dx, caster.getY(), caster.getZ() + dz);

            Entity spawned = type.create(level);
            if (spawned == null) continue;
            spawned.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    caster.getYRot(), 0);

            CompoundTag data = spawned.getPersistentData();
            if (caster instanceof Player player) {
                data.putUUID(OWNER_UUID_TAG, player.getUUID());
            } else {
                data.putUUID(OWNER_UUID_TAG, livingCaster.getUUID());
            }
            data.putLong(EXPIRY_TAG, expiresAt);

            if (spawned instanceof Mob mob) {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                        MobSpawnType.MOB_SUMMONED, null, null);
            }

            level.addFreshEntity(spawned);
        }
    }
}
