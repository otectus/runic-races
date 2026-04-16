package com.otectus.runic_races.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/**
 * Dedicated Runic Races keybinds. Both flap and cancel default to unbound
 * ({@code InputConstants.UNKNOWN}) so the existing jump-override behavior
 * in {@link FlightInputHandler} still works for players who don't rebind.
 * Users can bind these in the vanilla Controls screen under the "Runic Races" category.
 */
public final class RRKeyBindings {

    public static final String CATEGORY = "key.categories.runic_races";

    public static final KeyMapping FLAP = new KeyMapping(
            "key.runic_races.flap",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    public static final KeyMapping CANCEL_GLIDE = new KeyMapping(
            "key.runic_races.cancel_glide",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    public static boolean flapIsBound() {
        return !FLAP.isUnbound();
    }

    public static boolean cancelIsBound() {
        return !CANCEL_GLIDE.isUnbound();
    }

    private RRKeyBindings() {}
}
