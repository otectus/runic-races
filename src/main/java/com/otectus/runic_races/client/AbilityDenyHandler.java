package com.otectus.runic_races.client;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.client.state.ClientCooldownReader;
import com.otectus.runic_races.registry.ModSounds;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only "ability not ready" cue. Origins silently swallows the primary-active
 * keypress while the cooldown resource is nonzero; this handler watches the same key
 * and, on a blocked press, pulses the HUD icon red and plays a soft deny sound.
 *
 * Known limitation: only cooldown gating is detected — mana/stamina-gated failures
 * (Iron's Spellbooks / Feathers) are not visible client-side and stay silent.
 */
@Mod.EventBusSubscriber(modid = RunicRacesMod.MOD_ID, value = Dist.CLIENT)
public final class AbilityDenyHandler {

    private static final int DEBOUNCE_TICKS = 10;

    private static KeyMapping primaryActiveKey;
    private static boolean keyResolved = false;
    private static long lastDenyGameTime = Long.MIN_VALUE;

    private AbilityDenyHandler() {}

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        KeyMapping key = resolveKey();
        if (key != null && key.matches(event.getKey(), event.getScanCode())) {
            onPrimaryActivePressed();
        }
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        // InputEvent.Key never fires for mouse-bound keybinds — cover them here.
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        KeyMapping key = resolveKey();
        if (key != null && key.matchesMouse(event.getButton())) {
            onPrimaryActivePressed();
        }
    }

    private static void onPrimaryActivePressed() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return;

        long gameTime = player.level().getGameTime();
        if (gameTime - lastDenyGameTime < DEBOUNCE_TICKS) return;

        String race = RaceHelper.getRaceName(player).orElse(null);
        if (race == null) return;

        AbilityIconRegistry.getPrimaryActive(race).ifPresent(ability ->
                ClientCooldownReader.read(player, ability.resourceId()).ifPresent(state -> {
                    if (!state.isReady()) {
                        lastDenyGameTime = gameTime;
                        RacialCooldownOverlay.triggerDenyPulse(ability.resourceId());
                        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.ABILITY_DENY.get(), 1.0f, 0.7f));
                    }
                }));
    }

    private static KeyMapping resolveKey() {
        if (!keyResolved) {
            keyResolved = true;
            for (KeyMapping key : Minecraft.getInstance().options.keyMappings) {
                String name = key.getName();
                if (name != null && name.contains("origins.primary_active")) {
                    primaryActiveKey = key;
                    break;
                }
            }
            if (primaryActiveKey == null) {
                RunicRacesMod.LOGGER.warn("[RunicRaces] Origins primary-active keybind not found — ability deny cue disabled");
            }
        }
        return primaryActiveKey;
    }
}
