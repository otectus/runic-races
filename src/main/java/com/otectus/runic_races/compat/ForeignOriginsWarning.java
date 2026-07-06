package com.otectus.runic_races.compat;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.config.RRCommonConfig;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.api.registry.OriginsDynamicRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Clean two-layer mode ({@code origins.disableDefaultOriginLayer=true}, the default)
 * disables the default {@code origins:origin} layer — which also hides any origins
 * that other Origins add-ons or datapacks register into it. That is intentional for
 * the flagship-pack experience, but it must never be a silent surprise: on server
 * start we scan the dynamic registries and shout about any origin that ended up
 * unreachable, naming the exact config remedy.
 */
@Mod.EventBusSubscriber(modid = RunicRacesMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForeignOriginsWarning {

    private ForeignOriginsWarning() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!RRCommonConfig.DISABLE_DEFAULT_ORIGIN_LAYER.get()) return;

        Optional<Registry<Origin>> originsOpt =
                event.getServer().registryAccess().registry(OriginsDynamicRegistries.ORIGINS_REGISTRY);
        Optional<Registry<OriginLayer>> layersOpt =
                event.getServer().registryAccess().registry(OriginsDynamicRegistries.LAYERS_REGISTRY);
        if (originsOpt.isEmpty() || layersOpt.isEmpty()) return;

        List<OriginLayer> enabledLayers = layersOpt.get().stream()
                .filter(OriginLayer::enabled)
                .toList();

        // Registry-based detection sees datapack-added origins too, not just mods.
        // Vanilla Origins' own roster ("origins:" namespace) is deliberately hidden
        // by clean two-layer mode, so only foreign namespaces are worth shouting about.
        List<ResourceLocation> hidden = new ArrayList<>();
        for (ResourceLocation id : originsOpt.get().keySet()) {
            String ns = id.getNamespace();
            if (ns.equals("origins") || ns.equals(RunicRacesMod.MOD_ID)) continue;
            if (enabledLayers.stream().noneMatch(layer -> layer.contains(id))) {
                hidden.add(id);
            }
        }
        if (hidden.isEmpty()) return;

        RunicRacesMod.LOGGER.warn("[RunicRaces] ================================================================");
        RunicRacesMod.LOGGER.warn("[RunicRaces] Clean two-layer mode is hiding {} origin(s) from other add-ons:", hidden.size());
        hidden.stream().sorted().forEach(id ->
                RunicRacesMod.LOGGER.warn("[RunicRaces]   - {}", id));
        RunicRacesMod.LOGGER.warn("[RunicRaces] These are registered but appear on no enabled origin layer, so");
        RunicRacesMod.LOGGER.warn("[RunicRaces] players cannot select them. To use Runic Races alongside other");
        RunicRacesMod.LOGGER.warn("[RunicRaces] Origins add-ons, set origins.disableDefaultOriginLayer = false");
        RunicRacesMod.LOGGER.warn("[RunicRaces] in config/runic_races-common.toml (re-enables the default layer).");
        RunicRacesMod.LOGGER.warn("[RunicRaces] ================================================================");
    }
}
