package com.otectus.runic_races.notification;

/**
 * Immutable description of the player-facing copy for one race-state transition.
 *
 * Holds translation keys (never literal text) plus a {@link net.minecraft.ChatFormatting}
 * color <em>name</em> — stored as a plain string so this class and {@link NotificationRegistry}
 * stay free of any Minecraft import and remain unit-testable on the bare JUnit classpath.
 * The color string is resolved to a real {@code ChatFormatting} in {@link RaceNotificationService}.
 *
 * @param startKey  translation key shown when the state begins (false -> true)
 * @param stopKey   translation key shown when the state ends (true -> false); may be {@code null} to suppress the stop banner
 * @param colorName lowercase {@code ChatFormatting} name (e.g. "gold", "dark_red")
 */
public record NotificationSpec(String startKey, String stopKey, String colorName) {
}
