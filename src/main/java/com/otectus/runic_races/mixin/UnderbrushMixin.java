package com.otectus.runic_races.mixin;

import com.otectus.runic_races.ability.RacialTags;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class UnderbrushMixin {
    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void runic$underbrush(BlockState block, Vec3 multiplier, CallbackInfo ci) {
        if ((Object)this instanceof Player p && block.is(RacialTags.UNDERBRUSH) && RaceHelper.isRace(p, "grove_elf")) ci.cancel();
    }
}
