package com.otectus.runic_races.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;

/** Full-body swept steps of at most 0.20 blocks. Geometry is checked before every movement. */
public final class SafeMovement {
    public record Step(boolean stopped, LivingEntity contact) {}
    private SafeMovement() {}
    public static boolean safe(ServerPlayer p, Vec3 position, boolean support) {
        return safe(p, position, support, true);
    }
    private static boolean safe(ServerPlayer p, Vec3 position, boolean support, boolean entities) {
        AABB box = p.getBoundingBox().move(position.subtract(p.position())).deflate(0.001);
        if (box.minY < p.level().getMinBuildHeight() || box.maxY > p.level().getMaxBuildHeight()
                || !p.level().getWorldBorder().isWithinBounds(box)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (!p.level().hasChunkAt(pos)) return false;
            var block = p.level().getBlockState(pos);
            if (block.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)
                    || block.is(net.minecraft.world.level.block.Blocks.FIRE)
                    || block.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)) return false;
        }
        if (entities ? !p.level().noCollision(p, box) : p.level().getBlockCollisions(p, box).iterator().hasNext()) return false;
        return !support || p.level().getBlockCollisions(p, box.move(0, -0.12, 0)).iterator().hasNext();
    }
    public static Step advance(ServerPlayer p, Vec3 displacement, boolean stopAtEntities) {
        return advance(p, displacement, stopAtEntities, false);
    }
    public static Step advance(ServerPlayer p, Vec3 displacement, boolean stopAtEntities, boolean support) {
        double length = displacement.length();
        if (!Double.isFinite(length) || length > 0.8) return new Step(true, null);
        int steps = Math.max(1, (int)Math.ceil(length / 0.20));
        Vec3 delta = displacement.scale(1.0 / steps);
        Vec3 destination = p.position();
        for (int i = 0; i < steps; i++) {
            Vec3 next = destination.add(delta);
            if (!safe(p, next, support, !stopAtEntities)) { relocate(p, destination); return new Step(true, null); }
            if (stopAtEntities) {
                AABB sweep = p.getBoundingBox().move(destination.subtract(p.position())).expandTowards(delta).inflate(0.03);
                var contact = p.level().getEntitiesOfClass(LivingEntity.class, sweep, e -> e != p && e.isAlive() && !e.isSpectator())
                        .stream().min(Comparator.<LivingEntity>comparingDouble(e -> e.distanceToSqr(p)).thenComparing(LivingEntity::getUUID)).orElse(null);
                if (contact != null) { relocate(p, destination); return new Step(true, contact); }
            }
            destination = next;
        }
        relocate(p, destination);
        return new Step(false, null);
    }
    private static void relocate(ServerPlayer p, Vec3 destination) {
        p.setDeltaMovement(Vec3.ZERO);
        if (destination.distanceToSqr(p.position()) > 1.0e-8)
            p.connection.teleport(destination.x, destination.y, destination.z, p.getYRot(), p.getXRot());
        p.hurtMarked = true;
    }
}
