package com.otectus.runic_races.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.*;

/** Permission hook for non-damage abilities. Damage continues through vanilla/Forge hurt events. */
@Cancelable
public final class RacialTargetEvent extends Event {
    public enum Action { SUPPORT, CONTROL }
    private final ServerPlayer caster;
    private final LivingEntity target;
    private final Action action;
    private RacialTargetEvent(ServerPlayer caster, LivingEntity target, Action action) {
        this.caster = caster; this.target = target; this.action = action;
    }
    public ServerPlayer getCaster() { return caster; }
    public LivingEntity getTarget() { return target; }
    public Action getAction() { return action; }
    public static boolean allowed(ServerPlayer caster, LivingEntity target, Action action) {
        return !MinecraftForge.EVENT_BUS.post(new RacialTargetEvent(caster, target, action));
    }
}
