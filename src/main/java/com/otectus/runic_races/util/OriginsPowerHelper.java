package com.otectus.runic_races.util;

import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.OptionalInt;

/**
 * Shared utilities for reading/writing Origins power resource values via Apoli API.
 */
public final class OriginsPowerHelper {

    private OriginsPowerHelper() {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean isResourceReady(ServerPlayer player, ResourceLocation powerId) {
        IPowerContainer container = IPowerContainer.get(player).orElse(null);
        if (container == null || !container.hasPower(powerId)) {
            return true;
        }

        Holder holder = container.getPower(powerId);
        if (holder == null || !holder.isBound()) {
            return true;
        }

        ConfiguredPower configuredPower = (ConfiguredPower) holder.value();
        OptionalInt value = configuredPower.getValue(player);
        return value.isEmpty() || value.getAsInt() <= 0;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void setResourceValue(ServerPlayer player, ResourceLocation powerId, int value) {
        IPowerContainer container = IPowerContainer.get(player).orElse(null);
        if (container == null || !container.hasPower(powerId)) {
            return;
        }

        Holder holder = container.getPower(powerId);
        if (holder == null || !holder.isBound()) {
            return;
        }

        ConfiguredPower configuredPower = (ConfiguredPower) holder.value();
        configuredPower.assign(player, value);
        container.sync();
    }
}
