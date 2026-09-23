package com.otectus.runic_races.mixin;

import com.otectus.runic_races.ability.DamageHooks;
import com.otectus.runic_races.ability.AbilityService;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerAttackMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void runic$attack(Entity target, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayer player) {
            if (AbilityService.withdrawn(player)) { ci.cancel(); return; }
            DamageHooks.beginAttack(player, target, player.getAttackStrengthScale(0.5f));
        }
    }
    @Inject(method = "attack", at = @At("RETURN"))
    private void runic$finishAttack(Entity target, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayer player) DamageHooks.endAttack(player);
    }
}
