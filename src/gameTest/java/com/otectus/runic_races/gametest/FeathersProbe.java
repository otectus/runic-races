package com.otectus.runic_races.gametest;

import com.otectus.runic_races.integration.IntegrationManager;
import com.elenai.feathers.attributes.FeathersAttributes;
import com.elenai.feathers.api.FeathersHelper;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public final class FeathersProbe {
    public static void verify(GameTestHelper h, ServerPlayer p) {
        var attribute = p.getAttribute(FeathersAttributes.MAX_FEATHERS.get());
        h.assertTrue(attribute != null, "Missing maximum-feathers attribute");
        var external = java.util.UUID.randomUUID();
        attribute.addTransientModifier(new AttributeModifier(external, "Test external feathers", 4, AttributeModifier.Operation.ADDITION));
        IntegrationManager.syncPlayer(p); IntegrationManager.syncPlayer(p);
        h.assertTrue(FeathersHelper.getMaxFeathers(p) == 30, "Colossan's 26 capacity did not compose with external +4");
        h.assertTrue(attribute.getBaseValue() == 20 && attribute.getModifier(external) != null, "Racial update overwrote external feather state");
    }
}
