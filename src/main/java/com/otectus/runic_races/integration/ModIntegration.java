package com.otectus.runic_races.integration;

import net.minecraft.server.level.ServerPlayer;

/**
 * Interface for optional mod integrations.
 * Implementations live in isolated packages and are loaded via reflection
 * only when their target mod is present.
 */
public interface ModIntegration {

    /**
     * Initialize the integration. Called once during mod setup.
     * Register event handlers, apply modifications, etc.
     */
    void init();

    /**
     * @return Human-readable name of this integration
     */
    String getName();

    /**
     * Re-apply any race-dependent state for a player.
     * Called by the central integration manager on login, respawn,
     * dimension change, and detected race changes.
     */
    default void syncPlayer(ServerPlayer player) {
    }
}
