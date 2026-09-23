package com.otectus.runic_races.mixin;

import com.otectus.runic_races.ability.HealingBudget;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(LivingEntity.class)
public abstract class LivingHealingMixin {
    @ModifyArg(method = "heal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"), index = 0)
    private float runic$finalHealCap(float health) { return HealingBudget.cap((LivingEntity)(Object)this, health); }
}
