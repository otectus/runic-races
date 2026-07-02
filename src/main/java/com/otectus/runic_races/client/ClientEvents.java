package com.otectus.runic_races.client;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.client.particle.RunicParticle;
import com.otectus.runic_races.client.render.WingModel;
import com.otectus.runic_races.client.render.WingRenderLayer;
import com.otectus.runic_races.registry.ModEntities;
import com.otectus.runic_races.registry.ModParticles;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RunicRacesMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(
                VanillaGuiOverlay.HOTBAR.id(),
                "racial_cooldowns",
                new RacialCooldownOverlay()
        );
        event.registerAbove(
                VanillaGuiOverlay.HOTBAR.id(),
                "racial_state_runes",
                new StateRuneOverlay()
        );
        RunicRacesMod.LOGGER.info("[RunicRaces] Registered racial HUD overlays (cooldowns + state runes)");
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(RRKeyBindings.FLAP);
        event.register(RRKeyBindings.CANCEL_GLIDE);
        RunicRacesMod.LOGGER.info("[RunicRaces] Registered flight keybinds (flap, cancel_glide — both unbound by default)");
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WingRenderLayer.WING_MODEL_LAYER, WingModel::createLayer);
        RunicRacesMod.LOGGER.info("[RunicRaces] Registered wing model layer definition");
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(net.minecraftforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.RUNE_GLYPH.get(), s -> new RunicParticle.Provider(s, RunicParticle.Behavior.RUNE));
        event.registerSpriteSet(ModParticles.SOUL_WISP.get(), s -> new RunicParticle.Provider(s, RunicParticle.Behavior.WISP));
        event.registerSpriteSet(ModParticles.FAE_SPARKLE.get(), s -> new RunicParticle.Provider(s, RunicParticle.Behavior.SPARKLE));
        event.registerSpriteSet(ModParticles.EMBER_SCALE.get(), s -> new RunicParticle.Provider(s, RunicParticle.Behavior.EMBER));
        event.registerSpriteSet(ModParticles.FROST_MOTE.get(), s -> new RunicParticle.Provider(s, RunicParticle.Behavior.MOTE));
        event.registerSpriteSet(ModParticles.VENOM_DRIP.get(), s -> new RunicParticle.Provider(s, RunicParticle.Behavior.DRIP));
        RunicRacesMod.LOGGER.info("[RunicRaces] Registered {} custom particle providers", 6);
    }

    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Grave Servant reuses the vanilla zombie renderer — purpose-built art can swap in later.
        event.registerEntityRenderer(ModEntities.GRAVE_SERVANT.get(), ZombieRenderer::new);
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        EntityModelSet modelSet = event.getEntityModels();
        for (String skin : event.getSkins()) {
            LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer =
                    (LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>)
                            (LivingEntityRenderer<?, ?>) event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new WingRenderLayer(renderer, modelSet));
            }
        }
        RunicRacesMod.LOGGER.info("[RunicRaces] Added wing render layer to all player skins");
    }
}
