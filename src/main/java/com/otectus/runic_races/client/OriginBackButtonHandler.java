package com.otectus.runic_races.client;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.network.C2SBackToFamilyPacket;
import com.otectus.runic_races.network.NetworkHandler;
import com.otectus.runic_races.util.RaceHelper;
import io.github.apace100.origins.screen.ChooseOriginScreen;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.client.OriginsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Injects a "&lt; Back" button into Origins' race selection screen so a player who has
 * already confirmed a heritage (family layer) can return and pick a different one.
 *
 * <p>The click is a client-driven screen-to-screen swap that mirrors what Origins itself
 * does at login: the family origin is cleared in the client container (exactly what
 * {@code S2CConfirmOrigin} does locally for a chosen one), the pending-layer list is rebuilt
 * from {@link OriginsAPI#getActiveLayers()}, and a fresh {@link ChooseOriginScreen} replaces
 * the race screen directly. The screen is never set to {@code null} in between: that would
 * grab and re-release the mouse, which pulls the cursor to the window centre. The server is
 * told through {@link C2SBackToFamilyPacket} so it clears the same layer and re-syncs.
 *
 * <p>Origins' {@code OriginsClient.OPEN_NEXT_LAYER} latch is set by every confirm packet and
 * never cleared by Origins. Left stale, the second heritage pick advances past the race layer
 * before the confirm arrives; the race layer is conditioned on the family origin, evaluates to
 * zero options while the family is still empty client-side, and the menu closes. Clearing the
 * latch whenever a choose screen opens is always safe (no confirm can be in flight while the
 * player has a choice in front of them) and also covers joining a second world in one session.
 */
@Mod.EventBusSubscriber(modid = RunicRacesMod.MOD_ID, value = Dist.CLIENT)
public final class OriginBackButtonHandler {

    private static final ResourceLocation RACE_LAYER_ID =
            new ResourceLocation(RunicRacesMod.MOD_ID, "race");

    private OriginBackButtonHandler() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof ChooseOriginScreen screen)) return;
        // A choose screen being open means the previous layer's confirm has already been
        // consumed; any leftover latch is stale and would skip the next layer prematurely.
        OriginsClient.OPEN_NEXT_LAYER.set(false);
        if (!isRaceLayer(screen)) return;

        event.addListener(Button.builder(
                        Component.translatable("gui.runic_races.back"),
                        b -> backToFamily())
                .bounds(8, 8, 48, 20)
                .build());
    }

    private static void backToFamily() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        IOriginContainer container = IOriginContainer.get(player).orElse(null);
        if (container == null) return;

        OriginsClient.OPEN_NEXT_LAYER.set(false);
        NetworkHandler.sendToServer(new C2SBackToFamilyPacket(OriginsClient.SHOW_DIRT_BACKGROUND));

        // Optimistic local un-choose; the server's sync packet reaffirms it a moment later.
        container.setOrigin(RaceHelper.FAMILY_LAYER, RaceHelper.EMPTY_ORIGIN);

        // Same snapshot Origins takes when it opens the screen at login: every enabled layer
        // the player has not chosen yet, in layer order -> [family, race].
        List<Holder<OriginLayer>> pending = new ArrayList<>();
        for (Holder.Reference<OriginLayer> layer : OriginsAPI.getActiveLayers()) {
            if (!container.hasOrigin(layer)) pending.add(layer);
        }
        if (pending.isEmpty()) return;

        // Screen -> screen: the mouse is never grabbed, so the cursor stays where it was.
        mc.setScreen(new ChooseOriginScreen(pending, 0, OriginsClient.SHOW_DIRT_BACKGROUND));
    }

    private static boolean isRaceLayer(ChooseOriginScreen screen) {
        var layer = screen.getCurrentLayer();
        return layer != null && layer.unwrapKey()
                .map(key -> key.location().equals(RACE_LAYER_ID))
                .orElse(false);
    }
}
