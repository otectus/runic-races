package com.otectus.runic_races.ability;

import com.otectus.runic_races.RunicRacesMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import java.util.*;
import java.util.function.Consumer;

/** Gameplay restrictions are independent of optional racial-affinity toggles. */
public final class ShellCasting {
    private static final List<Consumer<ServerPlayer>> INTERRUPTERS = new ArrayList<>();
    private ShellCasting() {}
    public static void init() {
        load("ars_nouveau", "ars.ArsShellCasting");
        load("irons_spellbooks", "irons.IronsShellCasting");
    }
    private static void load(String mod, String implementation) {
        if (!ModList.get().isLoaded(mod)) return;
        try { Class.forName("com.otectus.runic_races.integration." + implementation).getMethod("register").invoke(null); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Unable to install Shellfast casting restriction for " + mod, e); }
    }
    public static void addInterrupter(Consumer<ServerPlayer> interrupter) { INTERRUPTERS.add(interrupter); }
    public static void interrupt(ServerPlayer p) { for (var interrupter : INTERRUPTERS) interrupter.accept(p); }
}
