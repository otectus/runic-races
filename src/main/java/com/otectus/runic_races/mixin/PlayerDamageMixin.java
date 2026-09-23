package com.otectus.runic_races.mixin;

import com.otectus.runic_races.ability.DamageHooks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Player overrides actuallyHurt without calling super; both engine paths need accounting. */
@Mixin(Player.class)
public abstract class PlayerDamageMixin {
    @Inject(method = "actuallyHurt", at = @At("HEAD"))
    private void runic$begin(DamageSource source, float amount, CallbackInfo ci) {
        DamageHooks.begin((LivingEntity)(Object)this, source);
    }
    @Redirect(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/ForgeHooks;onLivingDamage(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;F)F", remap = false))
    private float runic$finalAmount(LivingEntity entity, DamageSource source, float amount) {
        return DamageHooks.finalAmount(entity, source, ForgeHooks.onLivingDamage(entity, source, amount));
    }
    @Inject(method = "actuallyHurt", at = @At("RETURN"))
    private void runic$finish(DamageSource source, float amount, CallbackInfo ci) {
        DamageHooks.finish((LivingEntity)(Object)this);
    }
}
