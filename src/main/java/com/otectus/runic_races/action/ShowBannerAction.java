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

import java.util.List;

/**
 * Custom Apoli entity action: show a localized actionbar banner using Runic Races'
 * unified presentation styling. Use this instead of {@code origins:execute_command "/title @s actionbar ..."}
 * so text routes through the same path as {@link com.otectus.runic_races.presentation.RunicPresentation}.
 * <p>
 * Text is a translation key (never literal) so banners localize to the client's language.
 * Optional {@code args} are passed through as {@code Component.translatable} substitutions,
 * mapping to {@code %s} placeholders in the lang value.
 * <p>
 * JSON usage:
 * <pre>
 * {
 *   "type": "runic_races:show_banner",
 *   "translation_key": "message.runic_races.changeling.shadowmeld",
 *   "args": [],
 *   "color": "light_purple",
 *   "bold": true
 * }
 * </pre>
 * <p>
 * Color names match {@link ChatFormatting} (lowercase). Unknown colors fall back to white.
 */
public class ShowBannerAction extends EntityAction<ShowBannerAction.Configuration> {

    public record Configuration(String translationKey, List<String> args, String color, boolean bold)
            implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("translation_key").forGetter(Configuration::translationKey),
                        Codec.STRING.listOf().optionalFieldOf("args", List.of()).forGetter(Configuration::args),
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

        MutableComponent component = config.args().isEmpty()
                ? Component.translatable(config.translationKey()).withStyle(color)
                : Component.translatable(config.translationKey(), config.args().toArray()).withStyle(color);
        if (config.bold()) {
            component = component.withStyle(ChatFormatting.BOLD);
        }
        player.displayClientMessage(component, true);
    }
}
