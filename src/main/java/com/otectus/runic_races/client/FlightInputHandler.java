package com.otectus.runic_races.client;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.network.FlightCancelPacket;
import com.otectus.runic_races.network.FlightFlapPacket;
import com.otectus.runic_races.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side input handler for flight controls.
 * <p>
 * While the player is gliding ({@code isFallFlying()}):
 * <ul>
 *   <li>Single jump press → flap wings (buffered 4 ticks for double-tap detection)</li>
 *   <li>Double-tap jump within 4 ticks → cancel glide</li>
 * </ul>
 * When NOT gliding, jump key is left to vanilla/Origins for normal jump and elytra activation.
 */
@Mod.EventBusSubscriber(modid = RunicRacesMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FlightInputHandler {

    private static final int DOUBLE_TAP_WINDOW = 4; // ticks

    private static boolean wasJumpDown = false;
    private static boolean pendingFlap = false;
    private static int pendingFlapCountdown = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return;

        boolean isJumpDown = mc.options.keyJump.isDown();
        boolean justPressed = isJumpDown && !wasJumpDown;
        wasJumpDown = isJumpDown;

        if (!player.isFallFlying()) {
            // Not gliding — reset state, let vanilla handle everything
            pendingFlap = false;
            pendingFlapCountdown = 0;
            return;
        }

        // Player is fall-flying — handle flap / cancel
        if (justPressed) {
            if (pendingFlap && pendingFlapCountdown > 0) {
                // Second press within buffer window → cancel glide
                pendingFlap = false;
                pendingFlapCountdown = 0;
                NetworkHandler.sendToServer(new FlightCancelPacket());
            } else {
                // First press → start buffer, wait for possible double-tap
                pendingFlap = true;
                pendingFlapCountdown = DOUBLE_TAP_WINDOW;
            }
        }

        // Tick down the pending flap buffer
        if (pendingFlap) {
            pendingFlapCountdown--;
            if (pendingFlapCountdown <= 0) {
                // Buffer expired, no second press → send flap
                pendingFlap = false;
                NetworkHandler.sendToServer(new FlightFlapPacket());
            }
        }
    }
}
