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
 * Two control schemes coexist:
 * <ol>
 *   <li><b>Dedicated keybinds</b> ({@link RRKeyBindings#FLAP} / {@link RRKeyBindings#CANCEL_GLIDE}):
 *       when bound, pressing them while gliding sends the flap/cancel packets directly.
 *       Single-press, no double-tap window.</li>
 *   <li><b>Jump override fallback</b> (unchanged legacy behavior): when {@code FLAP} is unbound,
 *       jump presses during glide are buffered — single press → flap, double-tap within 4 ticks
 *       → cancel glide. Preserves usability for users who never open the Controls screen.</li>
 * </ol>
 * When NOT gliding, the jump key is left to vanilla for normal jump and elytra activation.
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

        // Drain dedicated keybind queues regardless of gliding state so held-down
        // presses don't leak once the player starts gliding.
        boolean dedicatedFlapPressed = false;
        boolean dedicatedCancelPressed = false;
        while (RRKeyBindings.FLAP.consumeClick()) dedicatedFlapPressed = true;
        while (RRKeyBindings.CANCEL_GLIDE.consumeClick()) dedicatedCancelPressed = true;

        if (!player.isFallFlying()) {
            // Not gliding — reset fallback state, let vanilla handle everything
            pendingFlap = false;
            pendingFlapCountdown = 0;
            wasJumpDown = mc.options.keyJump.isDown();
            return;
        }

        // === Gliding ===

        // Dedicated keybinds take priority if they were pressed this tick
        if (dedicatedCancelPressed) {
            NetworkHandler.sendToServer(new FlightCancelPacket());
            pendingFlap = false;
            pendingFlapCountdown = 0;
            wasJumpDown = mc.options.keyJump.isDown();
            return;
        }
        if (dedicatedFlapPressed) {
            NetworkHandler.sendToServer(new FlightFlapPacket());
            pendingFlap = false;
            pendingFlapCountdown = 0;
            wasJumpDown = mc.options.keyJump.isDown();
            return;
        }

        // Fallback: jump-override only if FLAP is unbound, so players who
        // deliberately rebound still get vanilla jump behavior during glide.
        if (RRKeyBindings.flapIsBound()) {
            wasJumpDown = mc.options.keyJump.isDown();
            return;
        }

        boolean isJumpDown = mc.options.keyJump.isDown();
        boolean justPressed = isJumpDown && !wasJumpDown;
        wasJumpDown = isJumpDown;

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
