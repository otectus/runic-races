package com.otectus.runic_races.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Custom Apoli entity action: show an actionbar banner using Runic Races'
 * unified presentation styling. Use this instead of {@code origins:execute_command "/title @s actionbar ..."}
 * so text routes through the same path as {@link com.otectus.runic_races.presentation.RunicPresentation}.
 * <p>
 * JSON usage:
 * <pre>
 * {
 *   "type": "runic_races:show_banner",
 *   "text": "You weave between shadows!",
 *   "color": "light_purple",
 *   "bold": true
 * }
 * </pre>
 * <p>
 * Color names match {@link ChatFormatting} (lowercase). Unknown colors fall back to white.
 */
public class ShowBannerAction extends EntityAction<ShowBannerAction.Configuration> {

    public record Configuration(String text, String color, boolean bold) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("text").forGetter(Configuration::text),
                        Codec.STRING.optionalFieldOf("color", "white").forGetter(Configuration::color),
                        Codec.BOOL.optionalFieldOf("bold", false).forGetter(Configuration::bold)
                ).apply(instance, Configuration::new)
        );
    }

    public ShowBannerAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration config, Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;

        ChatFormatting color = ChatFormatting.getByName(config.color());
        if (color == null) color = ChatFormatting.WHITE;

        MutableComponent component = Component.literal(config.text()).withStyle(color);
        if (config.bold()) {
            component = component.withStyle(ChatFormatting.BOLD);
        }
        player.displayClientMessage(component, true);
    }
}
