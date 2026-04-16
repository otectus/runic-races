package com.otectus.runic_races.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.otectus.runic_races.block.TrapMarkerBlockEntity;
import com.otectus.runic_races.registry.ModBlocks;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Custom Apoli entity action: places a Kobold {@code trap_marker} block at the
 * caster's current position, tagged with the caster's UUID and an auto-despawn
 * timer. Used by {@code kobold/improvised_trap.json} to turn the former AoE
 * panic-explosion into a real deployable trap the caster sets and walks away from.
 * <p>
 * JSON usage:
 * <pre>
 * {
 *   "type": "runic_races:place_trap",
 *   "duration_ticks": 6000
 * }
 * </pre>
 * <p>
 * If the target block position is already occupied by a non-replaceable block,
 * the action no-ops silently.
 */
public class PlaceTrapAction extends EntityAction<PlaceTrapAction.Configuration> {

    public record Configuration(int durationTicks) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.optionalFieldOf("duration_ticks", TrapMarkerBlockEntity.DEFAULT_DURATION_TICKS)
                                .forGetter(Configuration::durationTicks)
                ).apply(instance, Configuration::new)
        );
    }

    public PlaceTrapAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration config, Entity entity) {
        if (!(entity instanceof Player player)) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        BlockPos pos = player.blockPosition();
        BlockState existing = level.getBlockState(pos);
        if (!existing.canBeReplaced() && existing.getFluidState().getType() != Fluids.EMPTY && !existing.isAir()) {
            // Don't overwrite non-replaceable blocks at the player's feet.
            return;
        }
        if (!existing.canBeReplaced()) return;

        BlockState trap = ModBlocks.TRAP_MARKER.get().defaultBlockState();
        if (!level.setBlock(pos, trap, Block.UPDATE_ALL)) return;

        if (level.getBlockEntity(pos) instanceof TrapMarkerBlockEntity be) {
            long expiresAt = level.getGameTime() + Math.max(1, config.durationTicks());
            be.setOwner(player.getUUID(), expiresAt);
        }
    }
}
