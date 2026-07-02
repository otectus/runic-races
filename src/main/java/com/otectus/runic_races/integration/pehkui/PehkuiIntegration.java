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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleOperations;
import virtuoel.pehkui.api.ScaleRegistries;
import virtuoel.pehkui.api.ScaleType;
import virtuoel.pehkui.api.ScaleTypes;
import virtuoel.pehkui.api.TypedScaleModifier;

import java.util.Optional;

/**
 * Pehkui integration: assigns racial height scaling to each of the 37 races, read
 * from the central {@link RaceRegistry}. Human is baseline (~1.0).
 * Range: 0.45 (Sprite) / 0.50 (Faerie) to 1.30 (Terra Drake).
 */
public class PehkuiIntegration implements ModIntegration {
    private static final float SCALE_EPSILON = 0.001f;
    private static final int RESIZE_PROTECTION_TICKS = 40;
    private static ScaleType RACE_SCALE_TYPE;

    @Override
    public void init() {
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
            float currentScale = scaleData.getScale();
            if (Math.abs(currentScale - scale) <= SCALE_EPSILON) {
                return;
            }

            EntityDimensions previousDimensions = player.getDimensions(player.getPose());
            boolean growing = scale > currentScale + SCALE_EPSILON;

            scaleData.setScale(scale);
            player.refreshDimensions();
            player.fallDistance = 0.0f;

            boolean relocated = false;
            if (growing) {
                relocated = relocateForGrowth(player, previousDimensions);
            }
            RacialEventHandler.markResizeProtection(player, RESIZE_PROTECTION_TICKS);

            EntityDimensions currentDimensions = player.getDimensions(player.getPose());
            RunicRacesMod.debug(
                    "[RunicRaces] Set Pehkui scale for {} ({}) from {} to {} ({}x{} -> {}x{}, relocated={})",
                    player.getName().getString(),
                    race == null ? "<no race>" : race,
                    currentScale,
                    scale,
                    previousDimensions.width,
                    previousDimensions.height,
                    currentDimensions.width,
                    currentDimensions.height,
                    relocated
            );
        } catch (Exception e) {
            RunicRacesMod.LOGGER.error("[RunicRaces] Failed to set Pehkui scale for {}: {}",
                    player.getName().getString(), e.getMessage());
        }
    }

    private boolean relocateForGrowth(ServerPlayer player, EntityDimensions previousDimensions) {
        EntityDimensions currentDimensions = player.getDimensions(player.getPose());
        Vec3 center = player.position().add(0.0, (double) previousDimensions.height / 2.0, 0.0);
        double horizontalGrowth = (double) Math.max(0.0F, currentDimensions.width - previousDimensions.width) + 1.0E-6;
        double verticalGrowth = (double) Math.max(0.0F, currentDimensions.height - previousDimensions.height) + 1.0E-6;

        VoxelShape growthShape = Shapes.create(AABB.ofSize(center, horizontalGrowth, verticalGrowth, horizontalGrowth));
        Optional<Vec3> safePosition = player.level().findFreePosition(
                player,
                growthShape,
                center,
                (double) currentDimensions.width,
                (double) currentDimensions.height,
                (double) currentDimensions.width
        );
        if (safePosition.isPresent()) {
            Vec3 target = safePosition.get().add(0.0, (double) (-currentDimensions.height) / 2.0, 0.0);
            player.teleportTo(target.x, target.y, target.z);
            return true;
        }

        if (currentDimensions.width > previousDimensions.width && currentDimensions.height > previousDimensions.height) {
            VoxelShape horizontalOnlyShape = Shapes.create(AABB.ofSize(center, horizontalGrowth, 1.0E-6, horizontalGrowth));
            Optional<Vec3> horizontalFallback = player.level().findFreePosition(
                    player,
                    horizontalOnlyShape,
                    center,
                    (double) currentDimensions.width,
                    (double) previousDimensions.height,
                    (double) currentDimensions.width
            );
            if (horizontalFallback.isPresent()) {
                Vec3 target = horizontalFallback.get().add(0.0, (double) (-previousDimensions.height) / 2.0 + 1.0E-6, 0.0);
                player.teleportTo(target.x, target.y, target.z);
                return true;
            }
        }

        return false;
    }
}
