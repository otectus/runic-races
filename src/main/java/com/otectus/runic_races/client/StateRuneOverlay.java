package com.otectus.runic_races.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.otectus.runic_races.client.state.ClientRaceState;
import com.otectus.runic_races.common.state.RaceStateFlags;
import com.otectus.runic_races.config.RRClientConfig;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * Secondary HUD overlay: renders small "state runes" — one per active
 * {@link RaceStateFlags} flag — so players can tell at a glance which passive
 * states are currently firing (home biome, hostile biome, night empowered,
 * claustrophobia, sunlight burn, fire-vulnerability, regrowth suppression).
 *
 * State is read from {@link ClientRaceState}, pushed by the server whenever
 * a flag changes. This overlay does not query Origins directly.
 *
 * Runes are procedurally drawn (colored 10×10 squares + single-letter glyphs)
 * so no texture asset is needed yet; a future rune atlas can drop in without
 * changing the layout logic.
 */
public class StateRuneOverlay implements IGuiOverlay {

    private static final int RUNE_SIZE = 10;
    private static final int RUNE_GAP = 2;

    private record RuneDef(RaceStateFlags flag, int color, String glyph, String tooltip) {}

    private static final List<RuneDef> DEFS = List.of(
            new RuneDef(RaceStateFlags.BIOME_HOME,         0xFF66C266, "H", "Home biome"),
            new RuneDef(RaceStateFlags.BIOME_HOSTILE,      0xFFD94A2B, "X", "Hostile biome"),
            new RuneDef(RaceStateFlags.NIGHT_EMPOWERED,    0xFF9B59D9, "N", "Night empowered"),
            new RuneDef(RaceStateFlags.TIGHT_SPACE,        0xFF8899AA, "C", "Tight space"),
            new RuneDef(RaceStateFlags.SUNLIGHT_BURNING,   0xFFFFCC33, "S", "Sunlight burn"),
            new RuneDef(RaceStateFlags.FIRE_VULNERABLE,    0xFFFF7733, "F", "Fire vulnerable"),
            new RuneDef(RaceStateFlags.REGROWTH_SUPPRESSED,0xFF995511, "R", "Regrowth suppressed"),
            new RuneDef(RaceStateFlags.BEAST_SURGE,        0xFFB03030, "B", "Beast Surge"),
            new RuneDef(RaceStateFlags.ADAPTATION_ACTIVE,  0xFFD4A235, "A", "Adapting")
    );

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!RRClientConfig.HUD_ENABLED.get()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isSpectator() || mc.options.hideGui) return;
        if (RaceHelper.getRaceName(player).isEmpty()) return;

        int flags = ClientRaceState.get();
        if (flags == 0) return;

        List<RuneDef> active = new ArrayList<>();
        for (RuneDef def : DEFS) {
            if (def.flag().isSet(flags)) active.add(def);
        }
        if (active.isEmpty()) return;

        float opacity = (float) (double) RRClientConfig.HUD_OPACITY.get();
        float scale = (float) (double) RRClientConfig.HUD_SCALE.get();
        int alphaByte = (int) (opacity * 255) & 0xFF;

        // Anchor: directly above the cooldown overlay region when ABOVE_HOTBAR,
        // otherwise sit just inside the opposite corner so it doesn't overlap.
        int totalWidth = active.size() * (RUNE_SIZE + RUNE_GAP) - RUNE_GAP;
        int scaledWidth = (int) (totalWidth * scale);
        int scaledHeight = (int) (RUNE_SIZE * scale);

        int x;
        int y;
        switch (RRClientConfig.HUD_ANCHOR.get()) {
            case TOP_LEFT -> { x = 6; y = 20; }
            case TOP_RIGHT -> { x = screenWidth - scaledWidth - 6; y = 20; }
            case BOTTOM_LEFT -> { x = 6; y = screenHeight - scaledHeight - 24; }
            case BOTTOM_RIGHT -> { x = screenWidth - scaledWidth - 6; y = screenHeight - scaledHeight - 24; }
            default -> { // ABOVE_HOTBAR
                x = screenWidth - scaledWidth - 10;
                y = screenHeight - 74; // above the cooldown slot region
            }
        }
        x += RRClientConfig.HUD_OFFSET_X.get();
        y += RRClientConfig.HUD_OFFSET_Y.get();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        Font font = mc.font;
        int cursorX = 0;
        for (RuneDef def : active) {
            drawRune(graphics, font, cursorX, 0, def, alphaByte);
            cursorX += RUNE_SIZE + RUNE_GAP;
        }

        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    private void drawRune(GuiGraphics graphics, Font font, int x, int y, RuneDef def, int alpha) {
        int fillColor = (alpha / 2 << 24) | (def.color() & 0x00FFFFFF);
        int borderColor = (alpha << 24) | (def.color() & 0x00FFFFFF);

        // Body
        graphics.fill(x + 1, y + 1, x + RUNE_SIZE - 1, y + RUNE_SIZE - 1, fillColor);
        // Border
        graphics.fill(x, y, x + RUNE_SIZE, y + 1, borderColor);
        graphics.fill(x, y + RUNE_SIZE - 1, x + RUNE_SIZE, y + RUNE_SIZE, borderColor);
        graphics.fill(x, y, x + 1, y + RUNE_SIZE, borderColor);
        graphics.fill(x + RUNE_SIZE - 1, y, x + RUNE_SIZE, y + RUNE_SIZE, borderColor);

        // Glyph letter — centered horizontally, slightly high
        int glyphWidth = font.width(def.glyph());
        int gx = x + (RUNE_SIZE - glyphWidth) / 2;
        int gy = y + 1;
        int glyphColor = (alpha << 24) | 0x000000; // dark on colored background
        graphics.drawString(font, def.glyph(), gx, gy, glyphColor, false);
    }
}
