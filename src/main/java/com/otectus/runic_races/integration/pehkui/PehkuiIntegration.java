package com.otectus.runic_races.integration.pehkui;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.event.RacialEventHandler;
import com.otectus.runic_races.integration.ModIntegration;
import com.otectus.runic_races.race.RaceRegistry;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleOperations;
import virtuoel.pehkui.api.ScaleRegistries;
import virtuoel.pehkui.api.ScaleType;
import virtuoel.pehkui.api.ScaleTypes;
import virtuoel.pehkui.api.TypedScaleModifier;

/** Optional body scaling; existing races retain BASE behavior, additions change geometry without racial reach. */
public class PehkuiIntegration implements ModIntegration {
    private static final float SCALE_EPSILON = 0.001f;
    private static final int RESIZE_PROTECTION_TICKS = 40;
    private static ScaleType RACE_SCALE_TYPE;
    private static ScaleType EXPANSION_SIZE_TYPE;

    @Override
    public void init() {
        registerScaleTypes();
    }

    /** Registry and dimension callbacks must exist on remote clients before scale packets arrive. */
    public static synchronized void registerScaleTypes() {
        if (RACE_SCALE_TYPE != null) return;
        // Register a custom ScaleType so racial scaling doesn't clobber BASE
        ResourceLocation typeId = new ResourceLocation(RunicRacesMod.MOD_ID, "race_scale");
        RACE_SCALE_TYPE = ScaleType.Builder.create()
                .defaultBaseScale(1.0f)
                .build();
        ScaleRegistries.register(ScaleRegistries.SCALE_TYPES, typeId, RACE_SCALE_TYPE);

        // Link our type into BASE via a multiplicative modifier
        TypedScaleModifier raceModifier = new TypedScaleModifier(() -> RACE_SCALE_TYPE, ScaleOperations.MULTIPLY);
        ResourceLocation modifierId = new ResourceLocation(RunicRacesMod.MOD_ID, "race_scale_modifier");
        ScaleRegistries.register(ScaleRegistries.SCALE_MODIFIERS, modifierId, raceModifier);
        ScaleTypes.BASE.getDefaultBaseValueModifiers().add(raceModifier);
        RACE_SCALE_TYPE.getScaleChangedEvent().add(data -> {
            if (data.getEntity() != null) ScaleTypes.BASE.getScaleData(data.getEntity()).onUpdate();
        });

        // Expansion size changes geometry only. BASE also affects reach, motion and other
        // Pehkui mechanics; preserve the old races' established BASE behavior separately.
        EXPANSION_SIZE_TYPE = ScaleType.Builder.create().defaultBaseScale(1.0f).build();
        ScaleRegistries.register(ScaleRegistries.SCALE_TYPES, new ResourceLocation(RunicRacesMod.MOD_ID, "race_body_size"), EXPANSION_SIZE_TYPE);
        TypedScaleModifier body = new TypedScaleModifier(() -> EXPANSION_SIZE_TYPE, ScaleOperations.MULTIPLY);
        ScaleRegistries.register(ScaleRegistries.SCALE_MODIFIERS, new ResourceLocation(RunicRacesMod.MOD_ID, "race_body_modifier"), body);
        ScaleTypes.WIDTH.getDefaultBaseValueModifiers().add(body);
        ScaleTypes.HEIGHT.getDefaultBaseValueModifiers().add(body);
        EXPANSION_SIZE_TYPE.getScaleChangedEvent().add(data -> {
            if (data.getEntity() == null) return;
            ScaleTypes.WIDTH.getScaleData(data.getEntity()).onUpdate();
            ScaleTypes.HEIGHT.getScaleData(data.getEntity()).onUpdate();
        });

        RunicRacesMod.LOGGER.info("[RunicRaces] Pehkui integration initialized — custom race_scale type registered with {} race scales",
                RaceRegistry.raceCount());
    }

    @Override
    public String getName() {
        return "Pehkui";
    }

    @Override
    public void syncPlayer(ServerPlayer player) {
        applyRaceScale(player);
    }

    private void applyRaceScale(ServerPlayer player) {
        String race = RaceHelper.getRaceName(player).orElse(null);
        // No race (origin cleared / non-runic origin picked) or integration toggled off
        // mid-session → return the player to baseline instead of freezing the old scale.
        boolean enabled = com.otectus.runic_races.config.RRServerConfig.PEHKUI_INTEGRATION.get();
        float scale = (race == null || !enabled) ? 1.0f : RaceHelper.getRaceScale(race);

        try {
            ScaleData scaleData = RACE_SCALE_TYPE.getScaleData(player);
            ScaleData bodyData = EXPANSION_SIZE_TYPE.getScaleData(player);
            boolean expansion = com.otectus.runic_races.ability.AbilityKind.forRace(race == null ? "" : race).isPresent();
            float baseTarget = expansion ? 1 : scale;
            float bodyTarget = expansion ? scale : 1;
            float oldBase = scaleData.getBaseScale(), oldBody = bodyData.getBaseScale();
            float currentScale = oldBase * oldBody;
            if (Math.abs(oldBase - baseTarget) <= SCALE_EPSILON && Math.abs(oldBody - bodyTarget) <= SCALE_EPSILON) {
                player.getPersistentData().remove("runic_races:resize_pending");
                return;
            }

            EntityDimensions previousDimensions = player.getDimensions(player.getPose());
            boolean growing = scale > currentScale + SCALE_EPSILON;

            Vec3 previousPosition = player.position();
            if (growing) {
                // Check before mutating scale. Refreshing dimensions can move the entity;
                // repeated grow/rollback/teleport attempts used to pin players near walls.
                float ratio = scale / currentScale;
                AABB destination = EntityDimensions.scalable(previousDimensions.width * ratio,
                        previousDimensions.height * ratio).makeBoundingBox(previousPosition);
                if (!player.level().getWorldBorder().isWithinBounds(destination)
                        || destination.minY < player.level().getMinBuildHeight()
                        || destination.maxY > player.level().getMaxBuildHeight()
                        || !player.level().noCollision(player, destination.deflate(1.0e-7))) {
                    if (!player.getPersistentData().getBoolean("runic_races:resize_pending"))
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.runic_races.resize_waiting"), true);
                    player.getPersistentData().putBoolean("runic_races:resize_pending", true);
                    return;
                }
            }
            scaleData.setScale(baseTarget);
            bodyData.setScale(bodyTarget);
            // Keep the feet fixed after the automatic dimension callbacks. The full
            // destination was checked above; no relocation search may cross a wall.
            player.setPos(previousPosition.x, previousPosition.y, previousPosition.z);
            player.fallDistance = 0.0f;
            player.getPersistentData().remove("runic_races:resize_pending");
            RacialEventHandler.markResizeProtection(player, RESIZE_PROTECTION_TICKS);

            EntityDimensions currentDimensions = player.getDimensions(player.getPose());
            RunicRacesMod.debug(
                    "[RunicRaces] Set Pehkui scale for {} ({}) from {} to {} ({}x{} -> {}x{})",
                    player.getName().getString(),
                    race == null ? "<no race>" : race,
                    currentScale,
                    scale,
                    previousDimensions.width,
                    previousDimensions.height,
                    currentDimensions.width,
                    currentDimensions.height
            );
        } catch (Exception e) {
            RunicRacesMod.LOGGER.error("[RunicRaces] Failed to set Pehkui scale for {}: {}",
                    player.getName().getString(), e.getMessage());
        }
    }

}
