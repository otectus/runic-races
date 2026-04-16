package com.otectus.runic_races.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.presentation.RunicPresentation;
import com.otectus.runic_races.presentation.SignatureKey;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Locale;

/**
 * Custom Apoli entity action: fire a named {@link SignatureKey} through
 * {@link RunicPresentation#fire}. Used by JSON-driven abilities that want the
 * same polished presentation as code-handled ones (Nine Lives, Dwarf Forge
 * Blessing, etc.).
 * <p>
 * JSON usage: {@code "type": "runic_races:signature_presentation", "key": "catfolk_nine_lives"}
 * <p>
 * Unknown keys log a warning and no-op.
 */
public class SignaturePresentationAction extends EntityAction<SignaturePresentationAction.Configuration> {

    public record Configuration(String key) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("key").forGetter(Configuration::key)
                ).apply(instance, Configuration::new)
        );
    }

    public SignaturePresentationAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration config, Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;

        SignatureKey key;
        try {
            key = SignatureKey.valueOf(config.key().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            RunicRacesMod.LOGGER.warn("[RunicRaces] signature_presentation: unknown key '{}'", config.key());
            return;
        }
        RunicPresentation.fire(player, key);
    }
}
