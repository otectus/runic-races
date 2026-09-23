package com.otectus.runic_races.integration.ars;

import com.hollingsworth.arsnouveau.api.event.SpellCastEvent;
import com.otectus.runic_races.ability.AbilityService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.*;

public final class ArsShellCasting {
    public static void register() { MinecraftForge.EVENT_BUS.register(new ArsShellCasting()); }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void cast(SpellCastEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && AbilityService.withdrawn(p)) event.setCanceled(true);
    }
}
