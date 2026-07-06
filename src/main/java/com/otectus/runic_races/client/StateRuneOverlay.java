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
 * Runes are 16×16 white-on-transparent glyphs from {@code textures/gui/rune/}
 * (tools/generate_state_runes.py), tinted with each rune's semantic color over
 * a dark backing framed in the player's family accent. When a rune first
 * lights, its name fades in as a label above the row for a few seconds
 * (mouse-captured HUDs can't hover-tooltip), gated by {@code stateRunePulse}.
 */
public class StateRuneOverlay implements IGuiOverlay {

    private static final int RUNE_SIZE = 12;
    private static final int RUNE_GAP = 2;

    // Rune-pulse animation: brief grow-and-shrink when a flag first turns on.
    private static final int PULSE_TICKS = 6;
    private static final float PULSE_MAX_SCALE = 0.4f;
    private static final long PULSE_SENTINEL = Long.MIN_VALUE;

    // Fade-in/out label shown when a rune first lights.
    private static final int LABEL_TICKS = 50;
    private static final int LABEL_FADE_IN = 5;
    private static final int LABEL_FADE_OUT = 12;

    private static final java.util.Map<RaceStateFlags, net.minecraft.resources.ResourceLocation> RUNE_TEXTURES =
            new java.util.EnumMap<>(RaceStateFlags.class);

    static {
        for (RaceStateFlags flag : RaceStateFlags.values()) {
            RUNE_TEXTURES.put(flag, new net.minecraft.resources.ResourceLocation(
                    com.otectus.runic_races.RunicRacesMod.MOD_ID,
                    "textures/gui/rune/" + flag.name().toLowerCase(java.util.Locale.ROOT) + ".png"));
        }
    }

    private int lastFlags = 0;
    private final long[] pulseStart = new long[RaceStateFlags.values().length];

    {
        java.util.Arrays.fill(pulseStart, PULSE_SENTINEL);
    }

    // tooltipKey is a translation key (i18n-ready); not rendered yet — hover-tooltip rendering is a future pass.
    private record RuneDef(RaceStateFlags flag, int color, String glyph, String tooltipKey) {}

    private static final List<RuneDef> DEFS = List.of(
            new RuneDef(RaceStateFlags.BIOME_HOME,         0xFF66C266, "H", "tooltip.runic_races.state.home_biome"),
            new RuneDef(RaceStateFlags.BIOME_HOSTILE,      0xFFD94A2B, "X", "tooltip.runic_races.state.hostile_biome"),
            new RuneDef(RaceStateFlags.NIGHT_EMPOWERED,    0xFF9B59D9, "N", "tooltip.runic_races.state.night_empowered"),
            new RuneDef(RaceStateFlags.TIGHT_SPACE,        0xFF8899AA, "C", "tooltip.runic_races.state.tight_space"),
            new RuneDef(RaceStateFlags.SUNLIGHT_BURNING,   0xFFFFCC33, "S", "tooltip.runic_races.state.sunlight_burn"),
            new RuneDef(RaceStateFlags.FIRE_VULNERABLE,    0xFFFF7733, "F", "tooltip.runic_races.state.fire_vulnerable"),
            new RuneDef(RaceStateFlags.ADAPTATION_ACTIVE,  0xFFD4A235, "A", "tooltip.runic_races.state.adapting"),
            new RuneDef(RaceStateFlags.OPEN_SKY,           0xFF6688CC, "O", "tooltip.runic_races.state.open_sky"),
            new RuneDef(RaceStateFlags.SUBMERGED_WEAK,     0xFF3399DD, "W", "tooltip.runic_races.state.submerged_weak"),
            new RuneDef(RaceStateFlags.DRY_SLUGGISH,       0xFFC2A366, "D", "tooltip.runic_races.state.dry_sluggish"),
            new RuneDef(RaceStateFlags.RAVENOUS,           0xFFCC7722, "V", "tooltip.runic_races.state.ravenous"),
            new RuneDef(RaceStateFlags.COLD_IRON_GRIP,     0xFFAAB6C2, "I", "tooltip.runic_races.state.cold_iron")
    );

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        if (!RRClientConfig.HUD_ENABLED.get()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isSpectator() || mc.options.hideGui) return;
        String raceName = RaceHelper.getRaceName(player).orElse(null);
        if (raceName == null) return;
        com.otectus.runic_races.presentation.FamilyAccent accent =
                com.otectus.runic_races.presentation.FamilyAccent.forFamily(RaceHelper.getRaceFamily(raceName));

        int flags = ClientRaceState.get();

        // Record freshly-set flags for the pulse animation. Done before the flags==0 early
        // return so lastFlags stays in sync when every rune clears.
        long gameTime = mc.level != null ? mc.level.getGameTime() : 0L;
        if (RRClientConfig.STATE_RUNE_PULSE.get() && flags != lastFlags) {
            for (RaceStateFlags f : RaceStateFlags.values()) {
                if (f.isSet(flags) && !f.isSet(lastFlags)) {
                    pulseStart[f.ordinal()] = gameTime;
                }
            }
        }
        lastFlags = flags;

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
            float pulse = pulseScale(def.flag(), gameTime, partialTick);
            if (pulse != 1.0f) {
                float cx = cursorX + RUNE_SIZE / 2.0f;
                float cy = RUNE_SIZE / 2.0f;
                graphics.pose().pushPose();
                graphics.pose().translate(cx, cy, 0);
                graphics.pose().scale(pulse, pulse, 1.0f);
                graphics.pose().translate(-cx, -cy, 0);
                drawRune(graphics, font, cursorX, 0, def, alphaByte, accent);
                graphics.pose().popPose();
            } else {
                drawRune(graphics, font, cursorX, 0, def, alphaByte, accent);
            }
            cursorX += RUNE_SIZE + RUNE_GAP;
        }

        // Fade-in/out label naming the most recently lit rune (HUD can't hover-tooltip).
        if (RRClientConfig.STATE_RUNE_PULSE.get()) {
            drawFreshRuneLabel(graphics, font, active, totalWidth, gameTime, partialTick);
        }

        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    /** Draws the tooltip text of the most recently pulsed active rune above the row. */
    private void drawFreshRuneLabel(GuiGraphics graphics, Font font, List<RuneDef> active,
                                    int rowWidth, long gameTime, float partialTick) {
        RuneDef freshest = null;
        long freshestStart = Long.MIN_VALUE;
        for (RuneDef def : active) {
            long start = pulseStart[def.flag().ordinal()];
            if (start == PULSE_SENTINEL) continue;
            float age = (gameTime - start) + partialTick;
            if (age >= 0 && age < LABEL_TICKS && start > freshestStart) {
                freshest = def;
                freshestStart = start;
            }
        }
        if (freshest == null) return;

        float age = (gameTime - freshestStart) + partialTick;
        float alpha;
        if (age < LABEL_FADE_IN) {
            alpha = age / LABEL_FADE_IN;
        } else if (age > LABEL_TICKS - LABEL_FADE_OUT) {
            alpha = (LABEL_TICKS - age) / LABEL_FADE_OUT;
        } else {
            alpha = 1.0f;
        }
        if (alpha < 0.1f) return; // sub-4 alpha renders opaque; skip the tail

        net.minecraft.network.chat.Component label =
                net.minecraft.network.chat.Component.translatable(freshest.tooltipKey());
        int labelWidth = font.width(label);
        int lx = rowWidth - labelWidth; // right-align with the row
        int ly = -(font.lineHeight + 2);
        int color = ((int) (alpha * 255) << 24) | (freshest.color() & 0x00FFFFFF);
        graphics.drawString(font, label, lx, ly, color, true);
    }

    /** Returns the scale factor for a rune mid-pulse (1.0 when not pulsing). */
    private float pulseScale(RaceStateFlags flag, long gameTime, float partialTick) {
        long start = pulseStart[flag.ordinal()];
        if (start == PULSE_SENTINEL) return 1.0f;
        float t = (gameTime - start) + partialTick;
        if (t < 0 || t >= PULSE_TICKS) return 1.0f;
        return 1.0f + PULSE_MAX_SCALE * (float) Math.sin((t / PULSE_TICKS) * Math.PI);
    }

    private void drawRune(GuiGraphics graphics, Font font, int x, int y, RuneDef def, int alpha,
                          com.otectus.runic_races.presentation.FamilyAccent accent) {
        // Dark backing so the tinted glyph reads against any scene.
        int backing = ((alpha * 2 / 3) << 24);
        graphics.fill(x + 1, y + 1, x + RUNE_SIZE - 1, y + RUNE_SIZE - 1, backing);

        // Family-accent frame ties the runes to the rest of the racial HUD.
        int borderColor = accent.withAlpha(alpha);
        graphics.fill(x, y, x + RUNE_SIZE, y + 1, borderColor);
        graphics.fill(x, y + RUNE_SIZE - 1, x + RUNE_SIZE, y + RUNE_SIZE, borderColor);
        graphics.fill(x, y, x + 1, y + RUNE_SIZE, borderColor);
        graphics.fill(x + RUNE_SIZE - 1, y, x + RUNE_SIZE, y + RUNE_SIZE, borderColor);

        // Tinted 16x16 glyph scaled into the 10px interior.
        int rgb = def.color();
        RenderSystem.setShaderColor(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f,
                (rgb & 0xFF) / 255f, alpha / 255f);
        graphics.blit(RUNE_TEXTURES.get(def.flag()), x + 1, y + 1,
                RUNE_SIZE - 2, RUNE_SIZE - 2, 0f, 0f, 16, 16, 16, 16);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // The adaptation rune overlays the live stack count on its diamond glyph.
        if (def.flag() == RaceStateFlags.ADAPTATION_ACTIVE) {
            int stacks = ClientRaceState.getAdaptationStacks();
            if (stacks > 0) {
                String count = Integer.toString(stacks);
                int gx = x + (RUNE_SIZE - font.width(count)) / 2 + 1;
                int gy = y + 2;
                graphics.drawString(font, count, gx, gy, (alpha << 24) | 0xFFFFFF, false);
            }
        }
    }
}
