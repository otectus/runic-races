package com.otectus.runic_races.client.state;

import com.otectus.runic_races.common.state.RaceStateFlags;

/**
 * Client-side mirror of the server {@code RaceStateTracker} for the local player.
 * Single static int — one local player, no keying needed. Updated by
 * {@code S2CRaceStatePacket} handler and read by HUD overlays.
 */
public final class ClientRaceState {

    private static volatile int flags = 0;
    private static volatile int adaptationStacks = 0;

    private ClientRaceState() {}

    public static int get() {
        return flags;
    }

    public static void set(int newFlags) {
        flags = newFlags;
    }

    public static boolean has(RaceStateFlags flag) {
        return flag.isSet(flags);
    }

    /** Human Adaptation stack count (0 when not adapting). Surfaced on the "A" state rune. */
    public static int getAdaptationStacks() {
        return adaptationStacks;
    }

    public static void setAdaptationStacks(int stacks) {
        adaptationStacks = stacks;
    }

    /**
     * Connection boundary: forget the last server's state. The next server hydrates the
     * mirror on login, so nothing from the previous world can light a rune meanwhile.
     */
    public static void reset() {
        flags = 0;
        adaptationStacks = 0;
    }
}
