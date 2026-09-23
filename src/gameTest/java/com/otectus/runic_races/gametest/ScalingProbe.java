package com.otectus.runic_races.gametest;

import com.otectus.runic_races.integration.IntegrationManager;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import virtuoel.pehkui.api.ScaleTypes;

/** Loaded only for the Pehkui runtime matrix, keeping the required-only runtime independent. */
public final class ScalingProbe {
    public static void verify(GameTestHelper h, ServerPlayer p) {
        // Warm every derived cache before changing the owned scales: fresh-only probes
        // miss the stale collision dimensions seen after login or changing race.
        for (var type : virtuoel.pehkui.api.ScaleRegistries.SCALE_TYPES.values()) type.getScaleData(p).getScale();
        IntegrationManager.syncPlayer(p);
        h.assertTrue(Math.abs(p.getBbHeight() - 1.8 * 1.2) < .02, "Colossan body scale was not applied: " + p.getBbHeight());
        h.assertTrue(Math.abs(ScaleTypes.REACH.getScaleData(p).getScale() - 1) < .001, "Race size granted reach");
        ScaleTypes.BASE.getScaleData(p).setScale(1.1f); p.refreshDimensions();
        IntegrationManager.syncPlayer(p);
        h.assertTrue(Math.abs(p.getBbHeight() - 1.8 * 1.2 * 1.1) < .02, "External body scale was overwritten");
        h.assertTrue(Math.abs(ScaleTypes.REACH.getScaleData(p).getScale() - 1.1) < .001, "External reach contribution was overwritten");
        var types = virtuoel.pehkui.api.ScaleRegistries.SCALE_TYPES;
        var body = types.get(new net.minecraft.resources.ResourceLocation("runic_races", "race_body_size"));
        var legacy = types.get(new net.minecraft.resources.ResourceLocation("runic_races", "race_scale"));
        // No manual refresh: receiving Pehkui scale updates must refresh the body itself.
        body.getScaleData(p).setScale(.6f);
        h.assertTrue(Math.abs(p.getBbHeight() - 1.8 * .6 * 1.1) < .02, "Body update left cached dimensions");
        body.getScaleData(p).setScale(1);
        legacy.getScaleData(p).setScale(.7f);
        h.assertTrue(Math.abs(p.getBbWidth() - .6 * .7 * 1.1) < .01, "Legacy update left cached width");
        h.assertTrue(Math.abs(ScaleTypes.REACH.getScaleData(p).getScale() - .7 * 1.1) < .001, "Legacy reach cache stale");
        legacy.getScaleData(p).setScale(1);
        ScaleTypes.BASE.getScaleData(p).setScale(1);
        body.getScaleData(p).setScale(1.2f);
        var wall = h.absolutePos(new net.minecraft.core.BlockPos(4, 2, 2));
        for (int z = 1; z <= 5; z++) {
            for (int y = 2; y <= 4; y++) h.setBlock(new net.minecraft.core.BlockPos(4, y, z), net.minecraft.world.level.block.Blocks.STONE);
        }
        p.setPos(wall.getX() - p.getBbWidth() / 2.0 - .01, wall.getY(), wall.getZ() + .5);
        double before = p.getZ();
        p.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(.2, 0, .2));
        h.assertTrue(p.getZ() > before + .19, "Scaled player stuck sliding along wall");
        h.assertTrue(p.getBoundingBox().maxX <= wall.getX() + .001, "Scaled player clipped wall");
        h.assertTrue(p.level().noCollision(p, p.getBoundingBox().deflate(.001)), "Scaled movement ended inside wall");
        body.getScaleData(p).setScale(.6f);
        p.setPos(wall.getX() - p.getBbWidth() / 2.0 - .01, wall.getY(), wall.getZ() + .5);
        var waitingPosition = p.position();
        var velocity = new net.minecraft.world.phys.Vec3(0, 0, .1);
        p.setDeltaMovement(velocity);
        IntegrationManager.syncPlayer(p); // Establish a blocked resize before exercising automatic retries.
        com.otectus.runic_races.common.state.RaceStateTracker.setFlag(p,
                com.otectus.runic_races.common.state.RaceStateFlags.BIOME_HOME, true);
        for (int i = 0; i < 3; i++) {
            p.tickCount += 20;
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                    net.minecraftforge.event.TickEvent.Phase.END, p));
        }
        h.assertTrue(com.otectus.runic_races.common.state.RaceStateFlags.BIOME_HOME.isSet(
                com.otectus.runic_races.common.state.RaceStateTracker.get(p)), "Resize retry cleared unrelated race state");
        com.otectus.runic_races.common.state.RaceStateTracker.clear(p);
        h.assertTrue(p.getPersistentData().getBoolean("runic_races:resize_pending"), "Unsafe growth was not deferred");
        h.assertTrue(p.position().equals(waitingPosition), "Growth retry teleported player near wall");
        h.assertTrue(p.getDeltaMovement().equals(velocity), "Growth retry interrupted movement");
        h.assertTrue(Math.abs(p.getBbWidth() - .36) < .01, "Deferred growth changed collision width");
        p.setPos(waitingPosition.x - 1, waitingPosition.y, waitingPosition.z);
        var freePosition = p.position();
        IntegrationManager.syncPlayer(p);
        h.assertTrue(!p.getPersistentData().getBoolean("runic_races:resize_pending"), "Growth did not resume in open space");
        h.assertTrue(Math.abs(p.getBbWidth() - .72) < .01, "Deferred size was not applied");
        h.assertTrue(p.position().equals(freePosition), "Growth moved the player's feet");
        var originalType = body;
        IntegrationManager.registerCommonTypes();
        h.assertTrue(types.get(new net.minecraft.resources.ResourceLocation("runic_races", "race_body_size")) == originalType,
                "Repeated setup replaced registered scale types");
    }
}
