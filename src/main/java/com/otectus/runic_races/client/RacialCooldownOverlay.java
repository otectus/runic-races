package com.otectus.runic_races.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.util.RaceHelper;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.*;

/**
 * Custom HUD overlay that renders racial ability cooldowns as repeating item icons,
 * similar to vanilla's heart display for health.
 *
 * Replaces Origins' built-in bar rendering with a thematic icon-based system
 * where each ability shows a row of small item sprites that fill/empty.
 */
public class RacialCooldownOverlay implements IGuiOverlay {

    private static final int MAX_ICONS = 5;
    private static final int ICON_SIZE = 9;     // Rendered size of each icon
    private static final int ICON_SPACING = 1;  // Gap between icons
    private static final int ROW_HEIGHT = 12;   // Vertical space per ability row
    private static final int RIGHT_MARGIN = 10;
    private static final int BOTTOM_OFFSET = 52; // Above hotbar

    // Tint colors
    private static final int DEPLETED_OVERLAY = 0xBB000000; // Dark overlay for depleted icons
    private static final int READY_TEXT_COLOR = 0xFF55FF55;  // Green for "Ready"
    private static final int COOLDOWN_TEXT_COLOR = 0xFFAAAAAA; // Gray for countdown

    // Cache to avoid querying Apoli every frame
    private String cachedRace = null;
    private long lastRaceCheck = 0;
    private final Map<ResourceLocation, ResourceState> resourceCache = new HashMap<>();
    private long lastResourceCheck = 0;

    private record ResourceState(int value, int max) {
        float readyProgress() {
            // Progress toward being ready: 0.0 = just triggered, 1.0 = fully ready
            return max > 0 ? 1.0f - ((float) value / max) : 1.0f;
        }
        boolean isReady() { return value <= 0; }
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isSpectator()) return;
        if (mc.options.hideGui) return;

        long gameTime = player.level().getGameTime();

        // Update race cache (every 5 seconds)
        if (gameTime - lastRaceCheck > 100 || cachedRace == null) {
            cachedRace = RaceHelper.getRaceName(player).orElse(null);
            lastRaceCheck = gameTime;
        }
        if (cachedRace == null) return;

        // Get abilities for this race
        List<AbilityIconRegistry.AbilityIcon> abilities = AbilityIconRegistry.getForRace(cachedRace);
        if (abilities.isEmpty()) return;

        // Update resource cache (5 times per second)
        if (gameTime - lastResourceCheck > 4) {
            updateResourceCache(player, abilities);
            lastResourceCheck = gameTime;
        }

        // Render each ability row
        Font font = mc.font;
        int totalWidth = MAX_ICONS * (ICON_SIZE + ICON_SPACING) + 30; // icons + text space
        int baseX = screenWidth - RIGHT_MARGIN - totalWidth;
        int baseY = screenHeight - BOTTOM_OFFSET;

        for (int i = 0; i < abilities.size(); i++) {
            AbilityIconRegistry.AbilityIcon ability = abilities.get(i);
            ResourceState state = resourceCache.get(ability.resourceId());

            int rowY = baseY - (i * ROW_HEIGHT);
            renderAbilityRow(graphics, font, ability, state, baseX, rowY);
        }
    }

    private void renderAbilityRow(GuiGraphics graphics, Font font, AbilityIconRegistry.AbilityIcon ability,
                                   ResourceState state, int x, int y) {
        ItemStack icon = ability.icon();

        if (state == null || state.isReady()) {
            // Ready state: render all icons bright
            for (int i = 0; i < MAX_ICONS; i++) {
                int ix = x + i * (ICON_SIZE + ICON_SPACING);
                renderScaledItem(graphics, icon, ix, y);
            }
        } else {
            // On cooldown: show progress via filled/depleted icons
            float progress = state.readyProgress();
            int filledIcons = Math.round(progress * MAX_ICONS);

            for (int i = 0; i < MAX_ICONS; i++) {
                int ix = x + i * (ICON_SIZE + ICON_SPACING);
                renderScaledItem(graphics, icon, ix, y);

                // Overlay dark tint on depleted (not-yet-ready) icons
                if (i >= filledIcons) {
                    graphics.fill(ix, y, ix + ICON_SIZE, y + ICON_SIZE, DEPLETED_OVERLAY);
                }
            }

            // Render remaining seconds
            int secondsRemaining = state.value() / 20;
            String timeText = secondsRemaining + "s";
            int textX = x + MAX_ICONS * (ICON_SIZE + ICON_SPACING) + 2;
            graphics.drawString(font, timeText, textX, y + 1, COOLDOWN_TEXT_COLOR, false);
        }
    }

    private void renderScaledItem(GuiGraphics graphics, ItemStack item, int x, int y) {
        graphics.pose().pushPose();
        // Scale 16x16 item to ICON_SIZE x ICON_SIZE
        float scale = ICON_SIZE / 16.0f;
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.renderItem(item, 0, 0);
        graphics.pose().popPose();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void updateResourceCache(Player player, List<AbilityIconRegistry.AbilityIcon> abilities) {
        try {
            IPowerContainer container = IPowerContainer.get(player).orElse(null);
            if (container == null) return;

            for (AbilityIconRegistry.AbilityIcon ability : abilities) {
                ResourceLocation resId = ability.resourceId();
                try {
                    if (!container.hasPower(resId)) continue;

                    // Use raw-typed getPower to avoid generic inference issues
                    Holder holder = container.getPower(resId);
                    if (holder == null || !holder.isBound()) continue;

                    ConfiguredPower cp = (ConfiguredPower) holder.value();
                    OptionalInt val = cp.getValue(player);
                    OptionalInt max = cp.getMaximum(player);

                    if (val.isPresent() && max.isPresent()) {
                        resourceCache.put(resId, new ResourceState(val.getAsInt(), max.getAsInt()));
                        continue;
                    }
                } catch (Exception ignored) {
                    // Power not found or incompatible type
                }
                // If we couldn't read, assume ready
                resourceCache.putIfAbsent(resId, new ResourceState(0, 1));
            }
        } catch (Exception e) {
            // Origins not loaded or other error
        }
    }
}
