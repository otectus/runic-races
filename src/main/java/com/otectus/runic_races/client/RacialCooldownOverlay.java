package com.otectus.runic_races.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.config.RRClientConfig;
import com.otectus.runic_races.presentation.FamilyAccent;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.*;

/**
 * Custom racial cooldown HUD. One slot per ability (16×16 icon + family-colored
 * frame). Cooldown progress is drawn as a vertical clock-wipe mask (dark overlay
 * shrinks from top → ready). When an ability is ready the frame alpha-pulses and
 * the cooldown→ready transition fires a one-shot white flash. Layout, scale,
 * opacity and feature toggles come from {@link RRClientConfig}.
 */
public class RacialCooldownOverlay implements IGuiOverlay {

    private static final int ICON = 16;
    private static final int FRAME = 1;
    private static final int SLOT = ICON + FRAME * 2;        // 18
    private static final int ROW_SPACING = 4;
    private static final int TEXT_GAP = 4;
    private static final int DEPLETED_OVERLAY = 0xCC000000;
    private static final int READY_TEXT_COLOR = 0xFF9BE0A2;
    private static final int COOLDOWN_TEXT_COLOR = 0xFFBBBBBB;
    private static final int FLASH_TICKS = 6;
    private static final int DENY_TICKS = 12;

    private static boolean debugLogged = false;

    // Deny pulses are triggered from AbilityDenyHandler (a different class), so this
    // map is static; decremented per render frame like the ready flashes.
    private static final Map<ResourceLocation, Integer> denyRemaining = new HashMap<>();

    /** Pulse the given ability slot red for a few frames (pressed while on cooldown). */
    public static void triggerDenyPulse(ResourceLocation resourceId) {
        denyRemaining.put(resourceId, DENY_TICKS);
    }

    // Cached race + resource state
    private String cachedRace = null;
    private long lastRaceCheck = -1;
    private final Map<ResourceLocation, ResourceState> resourceCache = new HashMap<>();
    private final Map<ResourceLocation, Boolean> wasReady = new HashMap<>();
    private final Map<ResourceLocation, Integer> flashRemaining = new HashMap<>();
    // Whether each ability's custom HUD texture is present in the loaded resource packs.
    // Resolved once per texture (lookups are cheap but we avoid hitting the manager per frame).
    private final Map<ResourceLocation, Boolean> textureExists = new HashMap<>();
    private long lastResourceCheck = -1;
    private Player cachedPlayer = null;

    private record ResourceState(int value, int max) {
        float readyProgress() {
            return max > 0 ? 1.0f - ((float) value / max) : 1.0f;
        }
        boolean isReady() { return value <= 0; }
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!RRClientConfig.HUD_ENABLED.get()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isSpectator()) return;
        if (mc.options.hideGui) return;

        long gameTime = player.level().getGameTime();

        if (player != cachedPlayer) {
            cachedPlayer = player;
            cachedRace = null;
            resourceCache.clear();
            wasReady.clear();
            flashRemaining.clear();
            lastRaceCheck = -1;
            lastResourceCheck = -1;
            debugLogged = false;
        }

        if (gameTime < lastRaceCheck || gameTime - lastRaceCheck > 100 || cachedRace == null) {
            String newRace = RaceHelper.getRaceName(player).orElse(null);
            if (newRace != null && !newRace.equals(cachedRace)) {
                resourceCache.clear();
                wasReady.clear();
                flashRemaining.clear();
            }
            cachedRace = newRace;
            lastRaceCheck = gameTime;
        }
        if (cachedRace == null) return;

        List<AbilityIconRegistry.AbilityIcon> abilities = AbilityIconRegistry.getForRace(cachedRace);
        if (abilities.isEmpty()) {
            if (!debugLogged) {
                RunicRacesMod.LOGGER.warn("[RunicRaces] CooldownOverlay: no abilities for race '{}' — HUD will not render", cachedRace);
                debugLogged = true;
            }
            return;
        }
        if (!debugLogged) {
            RunicRacesMod.LOGGER.info("[RunicRaces] CooldownOverlay: race='{}', abilities={}, rendering enabled", cachedRace, abilities.size());
            debugLogged = true;
        }

        if (gameTime - lastResourceCheck > 4) {
            updateResourceCache(player, abilities);
            lastResourceCheck = gameTime;
        }

        updateReadyFlashes(abilities);

        float scale = (float) (double) RRClientConfig.HUD_SCALE.get();
        float opacity = (float) (double) RRClientConfig.HUD_OPACITY.get();
        if (opacity <= 0.01f) return;

        boolean minimal = RRClientConfig.HUD_MINIMAL_MODE.get();
        boolean showNames = RRClientConfig.HUD_SHOW_NAMES.get() && !minimal;
        boolean readyGlow = RRClientConfig.HUD_READY_GLOW.get();

        Font font = mc.font;
        int rowHeight = SLOT + ROW_SPACING;
        int totalHeight = abilities.size() * rowHeight - ROW_SPACING;
        int contentWidth = SLOT + TEXT_GAP + 48; // icon + gap + text budget

        int[] anchor = resolveAnchor(screenWidth, screenHeight, contentWidth, totalHeight, scale);
        int baseX = anchor[0];
        int baseY = anchor[1];

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.pose().pushPose();
        graphics.pose().translate(baseX, baseY, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        for (int i = 0; i < abilities.size(); i++) {
            AbilityIconRegistry.AbilityIcon ability = abilities.get(i);
            ResourceState state = resourceCache.get(ability.resourceId());
            int rowY = i * rowHeight;
            renderSlot(graphics, font, ability, state, 0, rowY, opacity, minimal, showNames, readyGlow, gameTime);
        }

        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    private int[] resolveAnchor(int screenWidth, int screenHeight, int contentWidth, int totalHeight, float scale) {
        int scaledWidth = (int) (contentWidth * scale);
        int scaledHeight = (int) (totalHeight * scale);
        RRClientConfig.HudAnchor anchor = RRClientConfig.HUD_ANCHOR.get();
        int x, y;
        switch (anchor) {
            case TOP_LEFT -> { x = 6; y = 6; }
            case TOP_RIGHT -> { x = screenWidth - scaledWidth - 6; y = 6; }
            case BOTTOM_LEFT -> { x = 6; y = screenHeight - scaledHeight - 6; }
            case BOTTOM_RIGHT -> { x = screenWidth - scaledWidth - 6; y = screenHeight - scaledHeight - 6; }
            default -> { // ABOVE_HOTBAR
                x = screenWidth - scaledWidth - 10;
                y = screenHeight - 52 - scaledHeight;
            }
        }
        x += RRClientConfig.HUD_OFFSET_X.get();
        y += RRClientConfig.HUD_OFFSET_Y.get();
        return new int[]{x, y};
    }

    private void renderSlot(GuiGraphics graphics, Font font, AbilityIconRegistry.AbilityIcon ability,
                            ResourceState state, int x, int y, float opacity, boolean minimal,
                            boolean showNames, boolean readyGlow, long gameTime) {
        FamilyAccent accent = ability.accent();
        boolean ready = state == null || state.isReady();
        int alphaByte = (int) (opacity * 255) & 0xFF;

        float pulse = readyGlow && ready ? 0.75f + 0.25f * (float) Math.sin(gameTime * 0.15) : 1.0f;
        int frameAlpha = (int) (alphaByte * pulse) & 0xFF;

        // --- Frame (1px border + 1px shadow) ---
        drawFrame(graphics, x, y, accent, frameAlpha);

        // --- Icon ---
        int ix = x + FRAME;
        int iy = y + FRAME;
        RenderSystem.enableBlend();
        if (hasTexture(ability.texture())) {
            // Icons are authored at 32x32 (Scale2x-polished); blit scales them into the 16px slot.
            graphics.blit(ability.texture(), ix, iy, ICON, ICON, 0f, 0f, 32, 32, 32, 32);
        } else {
            graphics.renderItem(ability.icon(), ix, iy); // fallback: vanilla item stand-in
        }

        // --- Vertical clock-wipe cooldown mask ---
        if (state != null && !ready) {
            float progress = state.readyProgress(); // 0 = just triggered, 1 = ready
            int maskHeight = Math.max(0, (int) ((1.0f - progress) * ICON));
            if (maskHeight > 0) {
                graphics.fill(ix, iy, ix + ICON, iy + maskHeight, DEPLETED_OVERLAY);
            }
        }

        // --- Ready transition flash (white overlay fading out) ---
        Integer flash = flashRemaining.get(ability.resourceId());
        if (flash != null && flash > 0) {
            float flashAlpha = (flash / (float) FLASH_TICKS) * 0.7f;
            int flashColor = ((int) (flashAlpha * 255) << 24) | 0xFFFFFF;
            graphics.fill(ix, iy, ix + ICON, iy + ICON, flashColor);
        }

        // --- Deny pulse (red overlay: pressed while on cooldown) ---
        Integer deny = denyRemaining.get(ability.resourceId());
        if (deny != null && deny > 0) {
            denyRemaining.put(ability.resourceId(), deny - 1);
            float denyAlpha = (deny / (float) DENY_TICKS) * 0.6f;
            int denyColor = ((int) (denyAlpha * 255) << 24) | 0xFF3333;
            graphics.fill(ix, iy, ix + ICON, iy + ICON, denyColor);
        }

        // --- Side text: seconds remaining / key hint ---
        int textX = x + SLOT + TEXT_GAP;
        int textY = y + (SLOT - font.lineHeight) / 2;
        if (!ready) {
            int secondsRemaining = Math.max(0, state.value() / 20);
            graphics.drawString(font, secondsRemaining + "s", textX, textY, COOLDOWN_TEXT_COLOR, false);
        } else if (!minimal) {
            String keyHint = resolveKeyHint(ability);
            if (keyHint != null) {
                graphics.drawString(font, keyHint, textX, textY, READY_TEXT_COLOR, false);
            }
        }

        // --- Ability name label (learning mode) ---
        if (showNames) {
            graphics.drawString(font, ability.name(), textX, textY + font.lineHeight + 1, 0xFFDDDDDD, false);
        }
    }

    /** True if a custom ability texture is present; result is cached per texture. */
    private boolean hasTexture(ResourceLocation texture) {
        if (texture == null) return false;
        Boolean cached = textureExists.get(texture);
        if (cached != null) return cached;
        boolean present = Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
        textureExists.put(texture, present);
        return present;
    }

    private void drawFrame(GuiGraphics graphics, int x, int y, FamilyAccent accent, int alpha) {
        int accentArgb = (alpha << 24) | (accent.accent() & 0x00FFFFFF);
        int shadowArgb = (Math.max(0, alpha - 60) << 24) | (accent.shadow() & 0x00FFFFFF);
        int outer = SLOT;
        // Shadow edge (bottom/right) — 1px outside the accent border
        graphics.fill(x + 1, y + outer - 1, x + outer, y + outer, shadowArgb);
        graphics.fill(x + outer - 1, y + 1, x + outer, y + outer, shadowArgb);
        // Accent border (top/left and inner)
        graphics.fill(x, y, x + outer, y + 1, accentArgb);                   // top
        graphics.fill(x, y, x + 1, y + outer, accentArgb);                   // left
        graphics.fill(x + outer - 1, y, x + outer, y + outer - 1, accentArgb); // right
        graphics.fill(x, y + outer - 1, x + outer - 1, y + outer, accentArgb); // bottom
    }

    private String resolveKeyHint(AbilityIconRegistry.AbilityIcon ability) {
        // Best-effort: show the vanilla "primary active" or "secondary active" key if Origins
        // binds them. Fall back to no hint rather than misleading text.
        Minecraft mc = Minecraft.getInstance();
        KeyMapping[] keys = mc.options.keyMappings;
        for (KeyMapping key : keys) {
            String name = key.getName();
            if (name == null) continue;
            if (name.contains("origins.primary_active") || name.contains("origins.secondary_active")) {
                Component bound = key.getTranslatedKeyMessage();
                if (bound != null) return bound.getString();
            }
        }
        return null;
    }

    private void updateReadyFlashes(List<AbilityIconRegistry.AbilityIcon> abilities) {
        for (AbilityIconRegistry.AbilityIcon ability : abilities) {
            ResourceLocation id = ability.resourceId();
            ResourceState state = resourceCache.get(id);
            boolean nowReady = state == null || state.isReady();
            boolean prevReady = wasReady.getOrDefault(id, nowReady);
            if (nowReady && !prevReady) {
                flashRemaining.put(id, FLASH_TICKS);
                if (RRClientConfig.HUD_READY_GLOW.get()) {
                    // forUI(sound, pitch, volume) — pitch varies per family for identity
                    Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            com.otectus.runic_races.registry.ModSounds.ABILITY_READY.get(),
                            ability.accent().readyPitch(), 0.6f));
                }
            } else {
                Integer remaining = flashRemaining.get(id);
                if (remaining != null && remaining > 0) {
                    flashRemaining.put(id, remaining - 1);
                }
            }
            wasReady.put(id, nowReady);
        }
    }

    private void updateResourceCache(Player player, List<AbilityIconRegistry.AbilityIcon> abilities) {
        for (AbilityIconRegistry.AbilityIcon ability : abilities) {
            ResourceLocation resId = ability.resourceId();
            com.otectus.runic_races.client.state.ClientCooldownReader.read(player, resId).ifPresentOrElse(
                    s -> resourceCache.put(resId, new ResourceState(s.value(), s.max())),
                    () -> resourceCache.putIfAbsent(resId, new ResourceState(0, 1)));
        }
    }
}
