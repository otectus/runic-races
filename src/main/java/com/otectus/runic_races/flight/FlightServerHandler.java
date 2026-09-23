package com.otectus.runic_races.flight;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.config.RRServerConfig;
import com.otectus.runic_races.presentation.ProcDebounce;
import com.otectus.runic_races.presentation.RunicPresentation;
import com.otectus.runic_races.presentation.SignatureKey;
import com.otectus.runic_races.registry.ModSounds;
import com.otectus.runic_races.util.OriginsPowerHelper;
import com.otectus.runic_races.util.RaceHelper;
import com.otectus.runic_races.util.StaminaHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-side handler for flight flap and glide cancel packets.
 */
public final class FlightServerHandler {

    private static final Map<UUID, Long> lastFlapTick = new HashMap<>();
    private static final int MIN_FLAP_INTERVAL_TICKS = 2;

    private FlightServerHandler() {}

    public static void handleFlap(ServerPlayer player) {
        if (com.otectus.runic_races.ability.AbilityService.withdrawn(player) || !player.isAlive() || player.isPassenger() || player.isInWater()) return;
        String race = RaceHelper.getRaceName(player).orElse(null);
        if (race == null) return;

        FlightConfig config = FlightConfig.forRace(race).orElse(null);
        if (config == null) return;

        // The legit client only sends flap packets mid-glide (FlightInputHandler);
        // anything else is a forged packet angling for free vertical thrust.
        if (!player.isFallFlying()) return;

        // Rate-limit: reject packets that arrive too quickly
        long now = player.level().getGameTime();
        Long last = lastFlapTick.get(player.getUUID());
        if (last != null && now - last < MIN_FLAP_INTERVAL_TICKS) return;
        // Stamp before the gating checks so denied flaps are rate-limited too.
        lastFlapTick.put(player.getUUID(), now);

        // Check Origins cooldown resource
        if (!OriginsPowerHelper.isResourceReady(player, config.getCooldownResource())) return;

        // Feathers cost — only when Feather's is actually present, so standalone
        // behavior is unchanged and the fail-closed policy is never consulted here.
        int featherCost = config.getFlapFeatherCost();
        if (featherCost > 0 && StaminaHelper.isAvailable() && RRServerConfig.FLAP_STAMINA_COST.get()) {
            if (!StaminaHelper.hasEnoughStamina(player, featherCost)
                    || !StaminaHelper.consumePlayerStamina(player, featherCost)) {
                // Exhausted wings: refuse, and don't burn the cooldown. The banner shares the
                // sound's one-second debounce — a held flap key is one message, not ten.
                if (ProcDebounce.tryAcquire(player, "flap_deny", 20)) {
                    player.displayClientMessage(Component.translatable("message.runic_races.ability.no_stamina")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModSounds.ABILITY_DENY.get(), SoundSource.PLAYERS, 0.5f, 1.0f);
                }
                return;
            }
        }

        // Apply flap
        player.setDeltaMovement(player.getDeltaMovement().add(0, config.getFlapVelocityY(), 0));
        player.hurtMarked = true; // Force velocity sync to client

        // Set cooldown
        OriginsPowerHelper.setResourceValue(player, config.getCooldownResource(), config.getCooldownTicks());

        // Fire unified presentation (sfx + vfx + actionbar banner)
        signatureKeyFor(config).ifPresent(key -> RunicPresentation.fire(player, key));

        RunicRacesMod.debug("[RunicRaces] {} flapped (race: {}, vel: +{})",
                player.getName().getString(), race, config.getFlapVelocityY());
    }

    public static void onLogout(UUID id) {
        lastFlapTick.remove(id);
    }

    /** Server stopping: nothing carries into the next world of an integrated session. */
    public static void clearAll() {
        lastFlapTick.clear();
    }

    public static int trackedCount() {
        return lastFlapTick.size();
    }

    public static void handleCancel(ServerPlayer player) {
        if (!player.isFallFlying()) return;

        String race = RaceHelper.getRaceName(player).orElse(null);
        if (race == null) return;

        boolean innate = io.github.edwinmindcraft.apoli.api.component.IPowerContainer.get(player)
                .map(c -> c.getPowers().stream().anyMatch(h -> h.isBound() && h.value().getFactory()
                        instanceof io.github.edwinmindcraft.apoli.common.power.ElytraFlightPower)).orElse(false);
        if (!innate) return;

        // Glide-only races use the same fold controls; a powered flap profile is not required.
        player.stopFallFlying();

        RunicPresentation.fire(player, SignatureKey.FLIGHT_CANCEL);

        RunicRacesMod.debug("[RunicRaces] {} canceled glide (race: {})",
                player.getName().getString(), race);
    }

    private static Optional<SignatureKey> signatureKeyFor(FlightConfig config) {
        return switch (config) {
            case ZEPHYR -> Optional.of(SignatureKey.ZEPHYR_WING_FLAP);
            case SPRITE -> Optional.of(SignatureKey.SPRITE_WING_FLAP);
            case FAERIE -> Optional.of(SignatureKey.FAERIE_WING_FLAP);
            case AVIAN -> Optional.of(SignatureKey.AVIAN_WING_FLAP);
            case WIND_WYRM -> Optional.of(SignatureKey.WIND_WYRM_WING_FLAP);
        };
    }
}
