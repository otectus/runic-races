package com.otectus.runic_races.client.state;

import com.otectus.runic_races.common.state.RaceStateFlags;

/**
 * Client-side mirror of the server {@code RaceStateTracker} for the local player.
 * Single static int — one local player, no keying needed. Updated by
 * {@code S2CRaceStatePacket} handler and read by HUD overlays.
 */
public final class ClientRaceState {

    private static volatile int flags = 0;

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
}
