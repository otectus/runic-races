package com.otectus.runic_races.client;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.network.C2SBackToFamilyPacket;
import com.otectus.runic_races.network.NetworkHandler;
import io.github.apace100.origins.screen.ChooseOriginScreen;
import io.github.edwinmindcraft.origins.client.OriginsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Injects a "&lt; Back" button into Origins' race selection screen so a player who has
 * already confirmed a heritage (family layer) can return and pick a different one.
 * The click round-trips through the server ({@link C2SBackToFamilyPacket}), which
 * un-chooses the family origin and re-opens selection from the family layer.
 */
@Mod.EventBusSubscriber(modid = RunicRacesMod.MOD_ID, value = Dist.CLIENT)
public final class OriginBackButtonHandler {

    private static final ResourceLocation RACE_LAYER_ID =
            new ResourceLocation(RunicRacesMod.MOD_ID, "race");

    private OriginBackButtonHandler() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof ChooseOriginScreen screen)) return;
        if (!isRaceLayer(screen)) return;

        event.addListener(Button.builder(
                        Component.translatable("gui.runic_races.back"),
                        b -> {
                            NetworkHandler.sendToServer(
                                    new C2SBackToFamilyPacket(OriginsClient.SHOW_DIRT_BACKGROUND));
                            // Origins' AWAITING_DISPLAY reopen only fires while no screen is
                            // open, so the race screen must be dismissed for the family
                            // screen to appear when the server round-trip completes.
                            Minecraft.getInstance().setScreen(null);
                        })
                .bounds(8, 8, 48, 20)
                .build());
    }

    private static boolean isRaceLayer(ChooseOriginScreen screen) {
        var layer = screen.getCurrentLayer();
        return layer != null && layer.unwrapKey()
                .map(key -> key.location().equals(RACE_LAYER_ID))
                .orElse(false);
    }
}
