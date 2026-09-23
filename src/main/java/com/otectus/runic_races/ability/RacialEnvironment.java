package com.otectus.runic_races.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;

/** The same predicates feed gameplay, conditions and owner feedback. No chunk loading. */
public final class RacialEnvironment {
    private RacialEnvironment() {}
    public static boolean night(LivingEntity e) {
        long time = e.level().getDayTime() % 24000;
        return e.level().dimensionType().natural() && !e.level().dimensionType().hasFixedTime()
                && time >= 13000 && time < 23000;
    }
    public static boolean sunlight(LivingEntity e) {
        return e.level().dimensionType().natural() && !e.level().dimensionType().hasFixedTime()
                && e.level().isDay() && !e.level().isRaining() && !e.level().isThundering()
                && e.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                && e.level().canSeeSky(BlockPos.containing(e.getEyePosition()))
                && e.level().getBrightness(LightLayer.SKY, e.blockPosition()) >= 14;
    }
    public static boolean lowLight(LivingEntity e) { return e.level().getMaxLocalRawBrightness(BlockPos.containing(e.getEyePosition())) <= 7; }
    public static boolean water(LivingEntity e) { return e.isInWater() || e.isEyeInFluid(FluidTags.WATER); }
    public static boolean rain(LivingEntity e) { return e.level().isRainingAt(e.blockPosition()); }
    public static boolean cold(LivingEntity e) {
        if (!e.level().getBiome(e.blockPosition()).is(RacialTags.COLD)) return false;
        if (e.isOnFire() || e.hasEffect(MobEffects.FIRE_RESISTANCE)) return false;
        for (var stack : e.getArmorSlots()) if (stack.is(ItemTags.FREEZE_IMMUNE_WEARABLES)) return false;
        // One small, cached neighborhood: actual lighted heat sources, never a biome-name heuristic.
        for (BlockPos pos : BlockPos.betweenClosed(e.blockPosition().offset(-2, -1, -2), e.blockPosition().offset(2, 1, 2))) {
            if (!e.level().hasChunkAt(pos)) continue;
            var state = e.level().getBlockState(pos);
            if (state.is(Blocks.FIRE) || state.is(Blocks.LAVA)
                    || (state.is(Blocks.CAMPFIRE) && state.getValue(net.minecraft.world.level.block.CampfireBlock.LIT))) return false;
        }
        return true;
    }
    public static boolean stoneFooting(LivingEntity e) {
        return e.onGround() && e.level().getBlockState(BlockPos.containing(e.getX(), e.getBoundingBox().minY - 0.08, e.getZ())).is(RacialTags.STONE);
    }
}
