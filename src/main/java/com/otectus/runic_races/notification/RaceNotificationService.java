package com.otectus.runic_races.notification;

import com.otectus.runic_races.common.state.RaceStateFlags;
import com.otectus.runic_races.config.RRServerConfig;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Server-side glue that turns a confirmed {@link RaceStateFlags} transition into a
 * localized action-bar banner. Stateless by design: notifications are start/stop only
 * (no periodic reminders, no debounce store), and the transition edge is already
 * computed by {@link com.otectus.runic_races.common.state.RaceStateTracker} before
 * this is called.
 *
 * Copy selection lives in the Minecraft-free {@link NotificationRegistry}; this class
 * only resolves the player's race, looks up the spec, and renders/delivers the banner.
 */
public final class RaceNotificationService {

    /**
     * Informational flags that churn too often to banner by default. They are surfaced
     * only when the server's learning-mode toggle is on, via generic translation keys.
     */
    private static final Set<RaceStateFlags> LEARNING_FLAGS = EnumSet.of(
            RaceStateFlags.BIOME_HOME,
            RaceStateFlags.BIOME_HOSTILE,
            RaceStateFlags.NIGHT_EMPOWERED,
            RaceStateFlags.ADAPTATION_ACTIVE
    );

    private RaceNotificationService() {}

    /**
     * Called by {@code RaceStateTracker.setFlag} immediately after a flag transition is
     * confirmed and the state packet has been sent.
     *
     * @param player   the transition owner
     * @param flag     the flag that flipped
     * @param newValue {@code true} = start (false->true), {@code false} = stop (true->false)
     */
    public static void onFlagTransition(ServerPlayer player, RaceStateFlags flag, boolean newValue) {
        if (!RRServerConfig.NOTIFICATIONS_ENABLED.get()) return;

        String race = RaceHelper.getRaceName(player).orElse(null);
        if (race == null) return;

        String key;
        String colorName;

        NotificationSpec spec = NotificationRegistry.resolve(race, flag);
        if (spec != null) {
            key = newValue ? spec.startKey() : spec.stopKey();
            colorName = spec.colorName();
        } else if (RRServerConfig.NOTIFICATIONS_LEARNING_MODE.get() && LEARNING_FLAGS.contains(flag)) {
            // Generic learning-mode copy for the informational flags.
            key = "message.runic_races.learning." + flag.name().toLowerCase(Locale.ROOT)
                    + (newValue ? ".start" : ".stop");
            colorName = "aqua";
        } else {
            return;
        }

        if (key == null || key.isEmpty()) return;

        ChatFormatting color = ChatFormatting.getByName(colorName);
        if (color == null) color = ChatFormatting.WHITE;

        MutableComponent banner = Component.translatable(key).withStyle(color);
        player.displayClientMessage(banner, true);
        if (RRServerConfig.NOTIFICATIONS_CHAT_MIRROR.get()) {
            player.displayClientMessage(banner, false);
        }
    }
}
