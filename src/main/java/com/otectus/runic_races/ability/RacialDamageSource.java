package com.otectus.runic_races.ability;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.server.level.ServerPlayer;

/** Attributed ordinary damage with provenance that cannot feed or recursively counter. */
public final class RacialDamageSource extends DamageSource {
    public RacialDamageSource(net.minecraft.world.entity.LivingEntity player, boolean resonance) {
        super(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(resonance
                ? ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("runic_races", "resonance"))
                : player instanceof net.minecraft.world.entity.player.Player ? DamageTypes.PLAYER_ATTACK : DamageTypes.MOB_ATTACK), player, player);
    }
}
