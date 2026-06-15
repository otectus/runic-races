package com.otectus.runic_races.compat;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.config.RRCommonConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Path;

/**
 * Registers the bundled "clean two-layer" datapack that disables the vanilla
 * {@code origins:origin} layer (so only the Family/Race screens appear).
 *
 * Instead of shipping that override as always-on data, we ship it as a built-in
 * datapack whose <em>required</em> (force-enabled) flag is driven by
 * {@link RRCommonConfig#DISABLE_DEFAULT_ORIGIN_LAYER}:
 * <ul>
 *   <li>config {@code true}  (default) → {@code required = true}  → pack always enabled →
 *       default layer disabled → clean two-layer experience.</li>
 *   <li>config {@code false}           → {@code required = false} → pack off by default →
 *       the vanilla layer stays enabled → other Origins add-ons coexist.</li>
 * </ul>
 *
 * Common config is read here, which is safe: it loads at mod construction, well before
 * datapack discovery. {@link AddPackFindersEvent} is a MOD-bus event.
 */
@Mod.EventBusSubscriber(modid = RunicRacesMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class OriginLayerPacks {

    private static final String PACK_ID = "runic_races_clean_two_layer";
    private static final String PACK_ROOT = "builtin_packs/clean_two_layer";

    private OriginLayerPacks() {}

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;

        Path root = ModList.get().getModFileById(RunicRacesMod.MOD_ID).getFile().findResource(PACK_ROOT);
        boolean required = RRCommonConfig.DISABLE_DEFAULT_ORIGIN_LAYER.get();

        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate(
                    PACK_ID,
                    Component.literal("Runic Races: Clean Two-Layer Mode"),
                    required,
                    id -> new PathPackResources(id, root, true),
                    PackType.SERVER_DATA,
                    Pack.Position.TOP,
                    PackSource.BUILT_IN
            );
            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }
}
