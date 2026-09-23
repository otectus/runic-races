package com.otectus.runic_races.client;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.ability.AbilityKind;
import com.otectus.runic_races.client.state.ClientCooldownReader;
import com.otectus.runic_races.network.*;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = RunicRacesMod.MOD_ID, value = Dist.CLIENT)
public final class ExpansionClient {
    private static AbilitySnapshot snapshot;
    private static int receivedTick;
    private static long sequence;
    private static boolean down;
    private ExpansionClient() {}
    public static void receive(AbilitySnapshot state) {
        snapshot = state; var p = Minecraft.getInstance().player; receivedTick = p == null ? 0 : p.tickCount;
    }
    public static boolean secondStage() { return snapshot != null && remaining() > 0 && Set.of("anchor", "shell").contains(snapshot.state()); }
    private static int elapsed() { var p = Minecraft.getInstance().player; return p == null ? 0 : Math.max(0, p.tickCount - receivedTick); }
    private static int remaining() { return snapshot == null ? 0 : Math.max(0, snapshot.activeTicks() - elapsed()); }
    public static Optional<ClientCooldownReader.CooldownState> cooldown(ResourceLocation id) {
        if (snapshot == null || snapshot.race().isEmpty()) return Optional.empty();
        return AbilityKind.forRace(snapshot.race()).filter(k -> k.cooldownId().equals(id.toString()))
                .map(k -> new ClientCooldownReader.CooldownState(Math.max(0, snapshot.cooldown() - elapsed()), snapshot.maximum()));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        boolean next = false;
        if (mc.screen == null && AbilityKind.forRace(RaceHelper.getRaceName(mc.player).orElse("")).isPresent())
            for (var key : mc.options.keyMappings) if ("key.origins.primary_active".equals(key.getName())) { next = key.isDown(); break; }
        if (next != down) { down = next; NetworkHandler.sendToServer(new AbilityInputPacket(++sequence, down)); }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e) { snapshot = null; sequence = 0; down = false; }
    @SubscribeEvent public static void mining(net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed e) {
        var p = e.getEntity();
        if (!p.level().isClientSide || e.getPosition().isEmpty()) return;
        String race = RaceHelper.getRaceName(p).orElse("");
        boolean live = snapshot != null && snapshot.race().equals(race) && remaining() > 0;
        if (live && "shell".equals(snapshot.state())) { e.setCanceled(true); return; }
        if (!e.getState().canHarvestBlock(p.level(), e.getPosition().get(), p)) return;
        if (race.equals("tide_elf") && p.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)
                && !net.minecraft.world.item.enchantment.EnchantmentHelper.hasAquaAffinity(p)) e.setNewSpeed(e.getNewSpeed() * 5);
        if (race.equals("mountain_one") && live && snapshot.charges() > 0
                && e.getState().is(com.otectus.runic_races.ability.RacialTags.QUARRY) && e.getState().getDestroySpeed(p.level(), e.getPosition().get()) >= 0) {
            var tuning = com.otectus.runic_races.ability.AbilityService.resolve(p, AbilityKind.MOUNTAIN_ONE);
            if (tuning != null) e.setNewSpeed(e.getNewSpeed() * (1 + tuning.amount("fraction")));
        }
    }
    public static List<Component> lines() {
        if (snapshot == null || snapshot.race().isEmpty()) return List.of();
        var player = Minecraft.getInstance().player;
        if (player == null || !snapshot.race().equals(RaceHelper.getRaceName(player).orElse(""))) return List.of();
        List<Component> result = new ArrayList<>();
        if (!snapshot.state().isEmpty() && (remaining() > 0 || snapshot.guard() > 0)) {
            String extra = snapshot.guard() > 0 ? String.format(Locale.ROOT, " %.1f HP", snapshot.guard())
                    : snapshot.charges() > 0 ? " ×" + snapshot.charges() : "";
            result.add(Component.translatable("hud.runic_races.state." + snapshot.state()).append(extra)
                    .append(remaining() > 0 ? " · " + (remaining() + 19) / 20 + "s" : ""));
        }
        if (snapshot.dry()) result.add(Component.translatable("hud.runic_races.dry"));
        if (snapshot.cold()) result.add(Component.translatable("hud.runic_races.cold"));
        if (snapshot.sunlight()) result.add(Component.translatable("hud.runic_races.sunlight"));
        var mc = Minecraft.getInstance();
        if (snapshot.targetId() >= 0 && mc.level != null && remaining() > 0) {
            var target = mc.level.getEntity(snapshot.targetId());
            if (target != null) result.add(Component.translatable("hud.runic_races.pursuit_target", target.getDisplayName()));
        }
        int width = Math.max(48, (int)((mc.getWindow().getGuiScaledWidth() - 20)
                / com.otectus.runic_races.config.RRClientConfig.HUD_SCALE.get()));
        List<Component> wrapped = new ArrayList<>();
        for (Component line : result)
            for (var part : mc.font.getSplitter().splitLines(line, width, net.minecraft.network.chat.Style.EMPTY))
                wrapped.add(Component.literal(part.getString()));
        return wrapped;
    }
    public static void render(GuiGraphics g, int y, float opacity, List<Component> lines) {
        var font = Minecraft.getInstance().font; int color = ((int)(opacity * 255) << 24) | 0xD7E8E1;
        for (Component line : lines) { g.drawString(font, line, 0, y, color, true); y += 11; }
    }
    @SubscribeEvent public static void waterFog(net.minecraftforge.client.event.ViewportEvent.RenderFog e) {
        var p = Minecraft.getInstance().player;
        if (p == null || !RaceHelper.isRace(p, "tide_elf") || e.getCamera().getFluidInCamera() != net.minecraft.world.level.material.FogType.WATER
                || p.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) || p.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS)) return;
        e.setFarPlaneDistance(Math.max(e.getFarPlaneDistance(), 32)); e.setCanceled(true);
    }
}
