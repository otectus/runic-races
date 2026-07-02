package com.otectus.runic_races.registry;

import com.otectus.runic_races.RunicRacesMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Custom sound events for race presentation. Each event is defined in
 * {@code assets/runic_races/sounds.json}, where it currently curates vanilla
 * {@code .ogg}s via {@code "type": "event"} references — resource packs (or a
 * future audio pass) can swap in bespoke recordings without touching code.
 */
public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RunicRacesMod.MOD_ID);

    // HUD / ability lifecycle
    public static final RegistryObject<SoundEvent> ABILITY_DENY = register("ability.deny");
    public static final RegistryObject<SoundEvent> ABILITY_READY = register("ability.ready");

    // Wing flaps (small = sprite/faerie, feather = avian, large = wind wyrm)
    public static final RegistryObject<SoundEvent> WINGS_FLAP_SMALL = register("wings.flap_small");
    public static final RegistryObject<SoundEvent> WINGS_FLAP_FEATHER = register("wings.flap_feather");
    public static final RegistryObject<SoundEvent> WINGS_FLAP_LARGE = register("wings.flap_large");

    // Draconic breath element layers (the shared dragon roar stays a separate layer)
    public static final RegistryObject<SoundEvent> BREATH_FIRE = register("breath.fire");
    public static final RegistryObject<SoundEvent> BREATH_FROST = register("breath.frost");
    public static final RegistryObject<SoundEvent> BREATH_WATER = register("breath.water");
    public static final RegistryObject<SoundEvent> BREATH_EARTH = register("breath.earth");
    public static final RegistryObject<SoundEvent> BREATH_SHOCK = register("breath.shock");
    public static final RegistryObject<SoundEvent> BREATH_WIND = register("breath.wind");

    // Signature moments
    public static final RegistryObject<SoundEvent> MYTHIC_REVIVAL = register("mythic.revival");
    public static final RegistryObject<SoundEvent> WARD_ACTIVATE = register("ward.activate");

    // Per-race signature layers (v1.4.0 identity pass)
    public static final RegistryObject<SoundEvent> DASH_WHOOSH = register("dash.whoosh");
    public static final RegistryObject<SoundEvent> HOWL_PACK = register("howl.pack");
    public static final RegistryObject<SoundEvent> WEB_SNARE = register("web.snare");
    public static final RegistryObject<SoundEvent> FOXFIRE_IGNITE = register("foxfire.ignite");
    public static final RegistryObject<SoundEvent> MIRROR_SHIFT = register("mirror.shift");
    public static final RegistryObject<SoundEvent> CHARM_SONG = register("charm.song");
    public static final RegistryObject<SoundEvent> TREMOR_PULSE = register("tremor.pulse");
    public static final RegistryObject<SoundEvent> SHIELD_BRACE = register("shield.brace");
    public static final RegistryObject<SoundEvent> BLOOM_VERDANT = register("bloom.verdant");
    public static final RegistryObject<SoundEvent> HARVEST_SOUL = register("harvest.soul");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(RunicRacesMod.MOD_ID, name)));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
