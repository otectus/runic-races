package com.otectus.runic_races.integration.irons;

import com.otectus.runic_races.ability.*;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.network.casting.CancelCastPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.*;

public final class IronsShellCasting {
    public static void register() {
        MinecraftForge.EVENT_BUS.register(new IronsShellCasting());
        ShellCasting.addInterrupter(p -> CancelCastPacket.cancelCast(p, true));
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void cast(SpellPreCastEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && AbilityService.withdrawn(p)) event.setCanceled(true);
    }
}
