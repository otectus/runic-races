package com.otectus.runic_races.client.state;

import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Client-side read of an Apoli cooldown resource's (value, max). Shared by the
 * cooldown HUD overlay and the ability-deny handler so both agree on readiness.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientCooldownReader {

    public record CooldownState(int value, int max) {
        public boolean isReady() { return value <= 0; }
    }

    private ClientCooldownReader() {}

    /**
     * Empty when Origins isn't ready, the player lacks the power, or the power
     * isn't a readable resource — callers should treat that as "unknown", not "ready".
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Optional<CooldownState> read(Player player, ResourceLocation resourceId) {
        try {
            IPowerContainer container = IPowerContainer.get(player).orElse(null);
            if (container == null || !container.hasPower(resourceId)) return Optional.empty();

            Holder holder = container.getPower(resourceId);
            if (holder == null || !holder.isBound()) return Optional.empty();

            ConfiguredPower cp = (ConfiguredPower) holder.value();
            OptionalInt val = cp.getValue(player);
            OptionalInt max = cp.getMaximum(player);
            if (val.isPresent() && max.isPresent()) {
                return Optional.of(new CooldownState(val.getAsInt(), max.getAsInt()));
            }
        } catch (Exception ignored) {
            // Origins absent or power incompatible — report unknown.
        }
        return Optional.empty();
    }
}
