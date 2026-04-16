package com.otectus.runic_races.block;

import com.otectus.runic_races.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Low-profile "trap marker" block placed by Kobold Improvised Trap. Has an
 * extremely thin collision / visual shape so it can be dropped onto any floor
 * without being obtrusive. When a non-owner {@link LivingEntity} steps on it,
 * the trap triggers: damage + slowness, sfx, particles, then self-destructs.
 *
 * Owner identity and expiry are tracked on the block entity (NBT-persisted
 * via {@link TrapMarkerBlockEntity}).
 */
public class TrapMarkerBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

    public TrapMarkerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide) return;
        if (!(level instanceof ServerLevel server)) return;
        if (!(entity instanceof LivingEntity victim)) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof TrapMarkerBlockEntity trap)) return;

        UUID owner = trap.getOwnerUuid();
        if (owner != null && victim.getUUID().equals(owner)) return;
        if (victim instanceof Player player && player.isCreative()) return;

        // Trigger.
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
        victim.hurt(level.damageSources().generic(), 4.0f);

        server.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.8f, 1.6f);
        server.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.4f, 1.8f);
        server.sendParticles(ParticleTypes.FLAME,
                pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                18, 0.4, 0.2, 0.4, 0.04);
        server.sendParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                12, 0.4, 0.2, 0.4, 0.02);

        level.removeBlock(pos, false);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrapMarkerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.TRAP_MARKER.get(),
                (lvl, pos, st, be) -> be.serverTick((ServerLevel) lvl, pos));
    }
}
