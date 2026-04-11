package com.otectus.runic_races.integration.pehkui;

import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.event.RacialEventHandler;
import com.otectus.runic_races.integration.ModIntegration;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.Optional;

/**
 * Pehkui integration: assigns racial height scaling to each of the 24 races.
 * Humans are baseline (1.0). Range: 0.25 (Sprite) to 1.40 (Giant-Blooded).
 */
public class PehkuiIntegration implements ModIntegration {
    private static final float SCALE_EPSILON = 0.001f;
    private static final int RESIZE_PROTECTION_TICKS = 40;

    @Override
    public void init() {
        RunicRacesMod.LOGGER.info("[RunicRaces] Pehkui integration initialized with {} race scales", RaceHelper.RACE_SCALES.size());
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
        float scale = RaceHelper.getRaceScale(race);

        try {
            ScaleData scaleData = ScaleTypes.BASE.getScaleData(player);
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
                    race,
                    currentScale,
                    scale,
                    previousDimensions.width,
                    previousDimensions.height,
                    currentDimensions.width,
                    currentDimensions.height,
                    relocated
            );
        } catch (Exception e) {
            RunicRacesMod.LOGGER.error("[RunicRaces] Failed to set Pehkui scale for {}: {}", race, e.getMessage());
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
