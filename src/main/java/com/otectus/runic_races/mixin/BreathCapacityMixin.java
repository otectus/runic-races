package com.otectus.runic_races.mixin;

import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class BreathCapacityMixin {
    @Inject(method = "getMaxAirSupply", at = @At("RETURN"), cancellable = true)
    private void runic$air(CallbackInfoReturnable<Integer> cir) {
        if ((Object)this instanceof Player p && p.getGameProfile() != null) {
            int extra = (int) com.otectus.runic_races.ability.RacialTraits.value(p, "extra_air");
            if (extra > 0) cir.setReturnValue(cir.getReturnValue() + extra);
        }
    }
}
