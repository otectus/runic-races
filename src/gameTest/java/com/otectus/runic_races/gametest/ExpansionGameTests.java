package com.otectus.runic_races.gametest;

import com.mojang.authlib.GameProfile;
import com.otectus.runic_races.ability.*;
import com.otectus.runic_races.race.RaceRegistry;
import com.otectus.runic_races.util.RaceHelper;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.registry.OriginsDynamicRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.*;
import java.util.*;
import java.util.function.Consumer;

/** Runs against real Forge, Origins capabilities and transformed vanilla damage methods. */
@GameTestHolder("runic_races")
@PrefixGameTestTemplate(false)
public final class ExpansionGameTests {
    private static final class TestPlayer extends ServerPlayer {
        String lastMessage = "";
        TestPlayer(ServerLevel level, String race) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "RR_" + race.substring(0, Math.min(12, race.length()))));
            connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(server,
                    new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), this) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { }
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.network.PacketSendListener listener) { }
            };
        }
        @Override public boolean isInvulnerableTo(DamageSource source) { return false; }
        @Override public float getAttackStrengthScale(float partialTick) { return 1; }
        @Override public void displayClientMessage(net.minecraft.network.chat.Component message, boolean actionBar) { lastMessage = message.getString(); }
    }
    private static TestPlayer player(GameTestHelper h, String race) {
        TestPlayer p = new TestPlayer(h.getLevel(), race);
        // Fresh ServerPlayers have vanilla login protection. Disable only that test fixture
        // field so hurt() traverses the actual outer attack, armor and absorption pipeline.
        try {
            var spawnProtection = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            spawnProtection.setAccessible(true); spawnProtection.setInt(p, 0);
        } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
        BlockPos pos = h.absolutePos(new BlockPos(2, 2, 2));
        // Explicit support belongs to this fixture; the template is placed one block above the structure marker.
        h.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        p.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5); p.setOnGround(true);
        IOriginContainer origins = IOriginContainer.get(p).orElseThrow(() -> new AssertionError("Origins capability absent"));
        origins.setOrigin(RaceHelper.FAMILY_LAYER, ResourceKey.create(OriginsDynamicRegistries.ORIGINS_REGISTRY,
                new ResourceLocation("runic_races", "family_" + RaceRegistry.getFamily(race))));
        origins.setOrigin(RaceHelper.RACE_LAYER, ResourceKey.create(OriginsDynamicRegistries.ORIGINS_REGISTRY, new ResourceLocation("runic_races", race)));
        origins.tick(); RaceHelper.invalidate(p.getUUID()); AbilityService.tick(p);
        return p;
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void expansionPowersLoad(GameTestHelper h) {
        h.assertTrue(RaceRegistry.raceCount() == 54, "Native roster must contain 54 races");
        for (AbilityKind kind : AbilityKind.values()) {
            TestPlayer p = player(h, kind.race());
            h.assertTrue(AbilityService.hasAbility(p, kind), "Missing parsed active configuration: " + kind);
            AbilityService.clear(p, true);
        }
        h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 160)
    public static void allExpansionActivesAcceptValidInput(GameTestHelper h) {
        for (AbilityKind kind : AbilityKind.values()) {
            TestPlayer p = player(h, kind.race());
            if (kind == AbilityKind.SAURIAN) AbilityService.session(p).stationary = 20;
            if (kind == AbilityKind.RETURNED) {
                var aggressor = h.spawn(EntityType.ZOMBIE, new BlockPos(5, 2, 2));
                p.hurt(p.damageSources().mobAttack(aggressor), 2);
            }
            AbilityService.input(p, 1, true);
            var session = AbilityService.session(p);
            h.assertTrue(session.cooldowns.getOrDefault(kind, 0) == session.tuning.cooldownTicks(), "Valid activation failed: " + kind + " reason=" + p.lastMessage
                    + " below=" + p.level().getBlockState(p.blockPosition().below()) + " box=" + p.getBoundingBox()
                    + " bodyFree=" + p.level().noCollision(p, p.getBoundingBox().deflate(.001))
                    + " floorFree=" + p.level().noCollision(p, p.getBoundingBox().deflate(.001).move(0, -.12, 0))
                    + " loaded=" + p.level().hasChunkAt(p.blockPosition()));
            AbilityService.clear(p, true);
        }
        h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void malformedTuningIsRejected(GameTestHelper h) {
        var malformed = com.google.gson.JsonParser.parseString("{\"kind\":\"tide_elf\",\"cooldown_ticks\":500,\"duration_ticks\":10,\"parameters\":{\"range\":16,\"land_range\":2,\"move_ticks\":1}}");
        h.assertTrue(AbilityTuning.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, malformed).error().isPresent(), "Unsafe movement tuning was accepted");
        malformed.getAsJsonObject().getAsJsonObject("parameters").addProperty("range", Double.NaN);
        h.assertTrue(AbilityTuning.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, malformed).error().isPresent(), "NaN tuning was accepted");
        h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void optionalBodyScalePreservesExternalReach(GameTestHelper h) throws Exception {
        if (net.minecraftforge.fml.ModList.get().isLoaded("pehkui")) {
            TestPlayer p = player(h, "colossan");
            Class.forName("com.otectus.runic_races.gametest.ScalingProbe").getMethod("verify", GameTestHelper.class, ServerPlayer.class).invoke(null, h, p);
            AbilityService.clear(p, true);
        }
        h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void removedPehkuiDoesNotKeepRetryingOldResize(GameTestHelper h) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("pehkui")) {
            TestPlayer p = player(h, "colossan");
            com.otectus.runic_races.integration.IntegrationManager.syncPlayer(p);
            p.getPersistentData().putBoolean("runic_races:resize_pending", true);
            var flag = com.otectus.runic_races.common.state.RaceStateFlags.BIOME_HOME;
            com.otectus.runic_races.common.state.RaceStateTracker.setFlag(p, flag, true);
            p.tickCount = 20;
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                    net.minecraftforge.event.TickEvent.Phase.END, p));
            h.assertTrue(!p.getPersistentData().getBoolean("runic_races:resize_pending"), "Removed Pehkui left pending resize");
            h.assertTrue(flag.isSet(com.otectus.runic_races.common.state.RaceStateTracker.get(p)), "Stale retry cleared race state");
            com.otectus.runic_races.common.state.RaceStateTracker.clear(p);
            AbilityService.clear(p, true);
        }
        h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void optionalSlotsAndFeathersPreserveExternalModifiers(GameTestHelper h) throws Exception {
        for (String mod : java.util.List.of("curios", "feathers")) {
            if (!net.minecraftforge.fml.ModList.get().isLoaded(mod)) continue;
            TestPlayer p = player(h, mod.equals("curios") ? "grove_elf" : "colossan");
            String probe = mod.equals("curios") ? "CuriosProbe" : "FeathersProbe";
            Class.forName("com.otectus.runic_races.gametest." + probe).getMethod("verify", GameTestHelper.class, ServerPlayer.class).invoke(null, h, p);
            AbilityService.clear(p, true);
        }
        h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void optionalArsAffinityAndShellUseRealEvents(GameTestHelper h) throws Exception {
        if (net.minecraftforge.fml.ModList.get().isLoaded("ars_nouveau")) {
            var probe = Class.forName("com.otectus.runic_races.gametest.ArsProbe");
            TestPlayer caster = player(h, "astral_elf");
            probe.getMethod("affinity", GameTestHelper.class, ServerPlayer.class).invoke(null, h, caster); AbilityService.clear(caster, true);
            TestPlayer shell = player(h, "chelon");
            probe.getMethod("shell", GameTestHelper.class, ServerPlayer.class).invoke(null, h, shell); AbilityService.clear(shell, true);
        }
        h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void optionalIronsAffinityAndChannelInterruption(GameTestHelper h) throws Exception {
        if (net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks")) {
            var probe = Class.forName("com.otectus.runic_races.gametest.IronsProbe");
            TestPlayer caster = player(h, "auroran");
            probe.getMethod("affinity", GameTestHelper.class, ServerPlayer.class).invoke(null, h, caster); AbilityService.clear(caster, true);
            TestPlayer shell = player(h, "chelon");
            probe.getMethod("shell", GameTestHelper.class, ServerPlayer.class).invoke(null, h, shell); AbilityService.clear(shell, true);
        }
        h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 160)
    public static void everyNativeSubpowerLoads(GameTestHelper h) throws Exception {
        for (String race : RaceRegistry.allRaceNames()) {
            TestPlayer p = player(h, race);
            var resources = h.getLevel().getServer().getResourceManager();
            var originId = new ResourceLocation("runic_races", "origins/" + race + ".json");
            try (var reader = resources.getResource(originId).orElseThrow().openAsReader()) {
                var origin = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                for (var powerId : origin.getAsJsonArray("powers")) {
                    ResourceLocation id = new ResourceLocation(powerId.getAsString());
                    try (var powerReader = resources.getResource(new ResourceLocation(id.getNamespace(), "powers/" + id.getPath() + ".json")).orElseThrow().openAsReader()) {
                        var power = com.google.gson.JsonParser.parseReader(powerReader).getAsJsonObject();
                        var container = io.github.edwinmindcraft.apoli.api.component.IPowerContainer.get(p).orElseThrow(() -> new AssertionError("Missing power container"));
                        for (var entry : power.entrySet()) {
                            if (entry.getKey().equals("condition") || !entry.getValue().isJsonObject() || !entry.getValue().getAsJsonObject().has("type")) continue;
                            ResourceLocation child = new ResourceLocation(id.getNamespace(), id.getPath() + "_" + entry.getKey());
                            var holder = container.getPower(child);
                            h.assertTrue(holder != null && holder.isBound(), "Authored subpower failed to load: " + child);
                        }
                    }
                }
            }
            AbilityService.clear(p, true);
        }
        h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 160)
    public static void everyUngatedLegacyActiveExecutesAndSpendsCooldown(GameTestHelper h) throws Exception {
        boolean magiFallbackTested = false;
        for (String race : RaceRegistry.allRaceNames()) {
            if (AbilityKind.forRace(race).isPresent()
                    || (race.equals("magi") && com.otectus.runic_races.util.ManaHelper.isAvailable())) continue;
            TestPlayer p = player(h, race);
            ResourceLocation parent = firstOriginPower(h, race);
            var active = configuredPower(p, new ResourceLocation(parent.getNamespace(), parent.getPath() + "_active_ability"));
            var key = active.getKey(p).orElseThrow(() -> new AssertionError("Legacy active has no key: " + race));
            h.assertTrue("key.origins.primary_active".equals(key.key()) && !key.continuous(),
                    "Legacy active has the wrong key contract: " + race + " -> " + key);
            h.assertTrue(active.activate(p), "Legacy active is not activatable: " + race);
            var cooldown = configuredPower(p, new ResourceLocation(parent.getNamespace(), parent.getPath() + "_cooldown_timer"));
            h.assertTrue(cooldown.getValue(p).orElse(0) > 0, "Legacy active executed without spending cooldown: " + race);
            if (race.equals("magi")) magiFallbackTested = true;
            AbilityService.clear(p, true);
        }
        h.assertTrue(com.otectus.runic_races.util.ManaHelper.isAvailable() || magiFallbackTested,
                "Magi's no-Iron's resource_available fallback was not exercised");
        h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void demonPrimaryActiveAppliesWrathAndCannotRepeatOnCooldown(GameTestHelper h) {
        TestPlayer p = player(h, "demon");
        var target = h.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 2));
        var outsideRadius = h.spawn(EntityType.ZOMBIE, new BlockPos(7, 2, 7));
        var active = configuredPower(p, new ResourceLocation("runic_races", "demon/infernal_wrath_active_ability"));
        var cooldown = configuredPower(p, new ResourceLocation("runic_races", "demon/infernal_wrath_cooldown_timer"));

        h.assertTrue(active.activate(p), "Demon primary active did not expose Apoli's active-power contract");
        h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST)
                        && p.getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST).getAmplifier() == 1,
                "Infernal Wrath did not grant Strength II");
        h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE),
                "Infernal Wrath did not grant Fire Resistance");
        h.assertTrue(target.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)
                        && target.getRemainingFireTicks() > 0,
                "Infernal Wrath did not weaken and ignite a nearby hostile");
        h.assertTrue(!outsideRadius.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)
                        && outsideRadius.getRemainingFireTicks() <= 0,
                "Infernal Wrath affected a hostile outside its spherical radius");
        h.assertTrue(cooldown.getValue(p).orElse(0) == 1000,
                "Infernal Wrath did not spend its authored cooldown");

        p.removeEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST);
        active.activate(p);
        h.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST),
                "Infernal Wrath bypassed its cooldown condition");

        var container = io.github.edwinmindcraft.apoli.api.component.IPowerContainer.get(p)
                .orElseThrow(() -> new AssertionError("Origins capability absent"));
        for (int tick = 1; tick <= 10; tick++) {
            p.tickCount++;
            container.serverTick();
        }
        h.assertTrue(cooldown.getValue(p).orElse(-1) == 990,
                "Infernal Wrath cooldown did not decay on Apoli's authored 10-tick interval");
        for (int tick = 11; tick <= 1000; tick++) {
            p.tickCount++;
            container.serverTick();
        }
        h.assertTrue(cooldown.getValue(p).orElse(-1) == 0,
                "Infernal Wrath cooldown became permanently stuck");
        AbilityService.clear(p, true);
        h.succeed();
    }

    private static ResourceLocation firstOriginPower(GameTestHelper h, String race) throws Exception {
        var resources = h.getLevel().getServer().getResourceManager();
        var originId = new ResourceLocation("runic_races", "origins/" + race + ".json");
        try (var reader = resources.getResource(originId).orElseThrow().openAsReader()) {
            var origin = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            return new ResourceLocation(origin.getAsJsonArray("powers").get(0).getAsString());
        }
    }

    private static io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower<?, ?> configuredPower(
            ServerPlayer player, ResourceLocation id) {
        var container = io.github.edwinmindcraft.apoli.api.component.IPowerContainer.get(player)
                .orElseThrow(() -> new AssertionError("Origins capability absent"));
        var holder = container.getPower(id);
        if (holder == null || !holder.isBound()) throw new AssertionError("Missing configured power: " + id);
        return (io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower<?, ?>) holder.value();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void lethalPreparedHitIsConsumed(GameTestHelper h) {
        TestPlayer p = player(h, "colossan");
        var cow = h.spawn(EntityType.COW, new BlockPos(4, 2, 4)); cow.setHealth(2);
        AbilityService.input(p, 1, true); p.attack(cow);
        h.assertTrue(cow.getHealth() == 0, "Prepared hit did not kill the low-health target");
        h.assertTrue(!AbilityService.session(p).prepared, "Lethal accepted hit failed to consume preparation");
        AbilityService.clear(p, true); h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void empoweredArrowCannotRepeatOnPiercing(GameTestHelper h) {
        TestPlayer p = player(h, "grove_elf");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW));
        AbilityService.input(p, 1, true);
        var arrow = new net.minecraft.world.entity.projectile.Arrow(h.getLevel(), p); arrow.setCritArrow(true);
        h.getLevel().addFreshEntity(arrow);
        h.assertTrue(!AbilityService.session(p).prepared, "Eligible arrow launch did not spend the cast");
        var first = h.spawn(EntityType.COW, new BlockPos(4, 2, 4));
        var second = h.spawn(EntityType.COW, new BlockPos(6, 2, 4));
        first.hurt(p.damageSources().arrow(arrow, p), 5); second.hurt(p.damageSources().arrow(arrow, p), 5);
        h.assertTrue(first.getHealth() == 4 && second.getHealth() == 5, "Piercing repeated or missed empowerment");
        AbilityService.clear(p, true); h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void canceledDamagePreservesWard(GameTestHelper h) {
        TestPlayer p = player(h, "scaleheir");
        WardLedger ledger = AbilityService.wards(p);
        ledger.offer(new WardLedger.Ward(WardLedger.Kind.DAWN, p.getUUID(), AbilityService.now(p) + 100, 4, 1), AbilityService.now(p));
        Consumer<LivingDamageEvent> cancel = e -> { if (e.getEntity() == p) e.setCanceled(true); };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, cancel);
        try { p.hurt(p.damageSources().magic(), 6); }
        finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
        h.assertTrue(p.getHealth() == p.getMaxHealth(), "Canceled hit changed health");
        h.assertTrue(ledger.remaining(WardLedger.Kind.DAWN) == 4, "Canceled hit consumed ward");
        AbilityService.clear(p, true); h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void absorptionPrecedesFiniteWard(GameTestHelper h) {
        TestPlayer p = player(h, "scaleheir");
        WardLedger ledger = AbilityService.wards(p);
        ledger.offer(new WardLedger.Ward(WardLedger.Kind.DAWN, p.getUUID(), AbilityService.now(p) + 100, 4, 1), AbilityService.now(p));
        p.setAbsorptionAmount(8); p.hurt(p.damageSources().magic(), 4);
        h.assertTrue(ledger.remaining(WardLedger.Kind.DAWN) == 4, "Absorption-only hit consumed ward");
        p.invulnerableTime = 0; p.setAbsorptionAmount(0); p.hurt(p.damageSources().magic(), 3);
        h.assertTrue(p.getHealth() == p.getMaxHealth(), "Ward did not protect health");
        h.assertTrue(ledger.remaining(WardLedger.Kind.DAWN) == 1, "Post-absorption ward debit was incorrect: " + ledger.remaining(WardLedger.Kind.DAWN));
        AbilityService.clear(p, true); h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void amplifiedHealingHasFinalCap(GameTestHelper h) {
        var target = h.spawn(EntityType.ZOMBIE, new BlockPos(5, 2, 5)); target.setHealth(5);
        Consumer<LivingHealEvent> amplify = e -> { if (e.getEntity() == target) e.setAmount(e.getAmount() * 20); };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, amplify);
        float restored;
        try { restored = HealingBudget.heal(target, 1, 6); }
        finally { MinecraftForge.EVENT_BUS.unregister(amplify); }
        h.assertTrue(restored == 6 && target.getHealth() == 11, "Final amplified healing exceeded or missed cap"); h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void petsAndWallsAreProtected(GameTestHelper h) {
        TestPlayer p = player(h, "bovine");
        Wolf wolf = h.spawn(EntityType.WOLF, new BlockPos(5, 2, 5)); wolf.setTame(true); wolf.setOwnerUUID(UUID.randomUUID());
        h.assertTrue(!TargetPolicy.aimed(p, wolf), "Offline owner's pet was targetable");
        var target = h.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 2));
        h.setBlock(new BlockPos(3, 2, 2), Blocks.STONE); h.setBlock(new BlockPos(3, 3, 2), Blocks.STONE);
        h.assertTrue(!TargetPolicy.visible(p, target), "Solid wall did not block target visibility");
        h.assertTrue(SafeMovement.advance(p, new Vec3(.75, 0, 0), true).stopped(), "Charge crossed a wall");
        AbilityService.clear(p, true); h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void cooldownSurvivesSessionReplacement(GameTestHelper h) {
        TestPlayer p = player(h, "colossan"); AbilityService.input(p, 1, true);
        int spent = AbilityService.session(p).cooldowns.getOrDefault(AbilityKind.COLOSSAN, 0);
        h.assertTrue(spent > 0, "Valid activation did not start cooldown");
        AbilityService.clear(p, true); AbilityService.tick(p);
        var restored = AbilityService.session(p);
        h.assertTrue(restored.cooldowns.getOrDefault(AbilityKind.COLOSSAN, 0) >= spent - 1, "Session replacement reset cooldown");
        h.assertTrue(!restored.prepared, "Prepared strike survived session replacement");
        AbilityService.clear(p, true); h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void canceledMeleeCannotFeed(GameTestHelper h) {
        TestPlayer p = player(h, "nightborn"); p.setHealth(5); AbilityService.input(p, 1, true);
        var target = h.spawn(EntityType.COW, new BlockPos(4, 2, 4));
        Consumer<LivingDamageEvent> cancel = e -> { if (e.getEntity() == target) e.setCanceled(true); };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, cancel);
        try { p.attack(target); } finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
        var s = AbilityService.session(p);
        h.assertTrue(p.getHealth() == 5 && s.charges == 3 && s.offered == 0, "Canceled melee granted or consumed feeding");
        AbilityService.clear(p, true); h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void feedingAmplificationCannotExceedCastCap(GameTestHelper h) {
        TestPlayer p = player(h, "nightborn"); p.setHealth(5); AbilityService.input(p, 1, true);
        var target = h.spawn(EntityType.COW, new BlockPos(4, 2, 4));
        Consumer<LivingHealEvent> amplify = e -> { if (e.getEntity() == p) e.setAmount(e.getAmount() * 100); };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, amplify);
        try { p.attack(target); } finally { MinecraftForge.EVENT_BUS.unregister(amplify); }
        var s = AbilityService.session(p);
        h.assertTrue(p.getHealth() == 11 && s.restored == 6 && s.charges == 2, "Actual feeding bypassed or missed its final cap");
        h.assertTrue(s.offered <= s.tuning.amount("healing"), "Single-hit pre-modifier cap was exceeded");
        AbilityService.clear(p, true); h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void raceCyclingKeepsDebtAndClearsPreparation(GameTestHelper h) {
        TestPlayer p = player(h, "colossan"); AbilityService.input(p, 1, true);
        var origins = IOriginContainer.get(p).orElseThrow(() -> new AssertionError("No Origins container"));
        origins.setOrigin(RaceHelper.RACE_LAYER, ResourceKey.create(OriginsDynamicRegistries.ORIGINS_REGISTRY, new ResourceLocation("runic_races", "auroran")));
        origins.tick(); RaceHelper.invalidate(p.getUUID()); AbilityService.tick(p);
        h.assertTrue(!AbilityService.session(p).prepared, "Race change retained a prepared attack");
        origins.setOrigin(RaceHelper.RACE_LAYER, ResourceKey.create(OriginsDynamicRegistries.ORIGINS_REGISTRY, new ResourceLocation("runic_races", "colossan")));
        origins.tick(); RaceHelper.invalidate(p.getUUID()); AbilityService.tick(p);
        h.assertTrue(AbilityService.session(p).cooldowns.getOrDefault(AbilityKind.COLOSSAN, 0) >= 698, "Race cycling reset cooldown debt");
        AbilityService.clear(p, true); h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void heldInputAndWallCannotRecall(GameTestHelper h) {
        TestPlayer p = player(h, "astral_elf"); AbilityService.input(p, 1, true);
        var s = AbilityService.session(p); h.assertTrue(s.anchor != null, "Supported anchor was refused");
        Vec3 anchor = s.anchor; p.setPos(p.getX(), p.getY(), p.getZ() + 3);
        h.setBlock(new BlockPos(2, 2, 3), Blocks.STONE); h.setBlock(new BlockPos(2, 3, 3), Blocks.STONE);
        h.runAfterDelay(5, () -> {
            AbilityService.input(p, 2, true);
            h.assertTrue(s.anchor != null && p.position().distanceToSqr(anchor) > 4, "Held input recalled an anchor");
            AbilityService.input(p, 3, false); AbilityService.input(p, 4, true);
            h.assertTrue(s.anchor != null && p.position().distanceToSqr(anchor) > 4, "Recall crossed solid cover");
            h.assertTrue(s.cooldowns.getOrDefault(AbilityKind.ASTRAL_ELF, 0) > 0, "Denied recall erased cooldown debt");
            AbilityService.clear(p, true); h.succeed();
        });
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void permissionVetoAndBossResistanceStopControl(GameTestHelper h) {
        TestPlayer p = player(h, "scaleheir");
        var target = h.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 4));
        Consumer<RacialTargetEvent> cancel = e -> { if (e.getTarget() == target) e.setCanceled(true); };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, cancel);
        Vec3 before = target.getDeltaMovement();
        try { AbilityService.control(p, target, net.minecraft.world.effect.MobEffects.WEAKNESS, 80, 0, true); }
        finally { MinecraftForge.EVENT_BUS.unregister(cancel); }
        h.assertTrue(!target.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS) && target.getDeltaMovement().equals(before), "Vetoed control applied a rider");
        var boss = EntityType.WITHER.create(h.getLevel());
        h.assertTrue(boss != null && !TargetPolicy.control(p, boss), "Boss accepted racial control");
        AbilityService.clear(p, true); h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 100)
    public static void nativeAttributesHealingAndFiniteBreathWork(GameTestHelper h) {
        Map<String, Double> health = Map.ofEntries(
                Map.entry("colossan", 24d), Map.entry("auroran", 22d), Map.entry("grove_elf", 17d),
                Map.entry("tide_elf", 18d), Map.entry("astral_elf", 17d), Map.entry("mountain_one", 20d),
                Map.entry("moss_one", 20d), Map.entry("crystal_one", 18d), Map.entry("bovine", 22d),
                Map.entry("saurian", 20d), Map.entry("chelon", 20d), Map.entry("zephyr", 16d),
                Map.entry("nightborn", 20d), Map.entry("returned", 22d), Map.entry("wailer", 16d),
                Map.entry("scaleheir", 20d), Map.entry("wyvernkin", 18d));
        for (var entry : health.entrySet()) {
            TestPlayer p = player(h, entry.getKey());
            h.assertTrue(Math.abs(p.getMaxHealth() - entry.getValue()) < .001, "Native maximum-health power failed for " + entry.getKey() + ": " + p.getMaxHealth());
            if (entry.getKey().equals("saurian") || entry.getKey().equals("chelon"))
                h.assertTrue(p.getMaxAirSupply() == (entry.getKey().equals("saurian") ? 600 : 500), "Finite extra breath did not apply");
            if (entry.getKey().equals("nightborn") || entry.getKey().equals("returned")) {
                p.setHealth(5); p.heal(10);
                h.assertTrue(Math.abs(p.getHealth() - (entry.getKey().equals("nightborn") ? 13 : 12.5)) < .001, "Native healing drawback was ineffective");
            }
            if (entry.getKey().equals("colossan")) h.assertTrue(Math.abs(p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED) - 3.52) < .001, "Heavy Limbs attack-speed drawback was ineffective");
            AbilityService.clear(p, true);
        }
        h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void damageAffinityCategoriesRemainIndependent(GameTestHelper h) {
        String[] races = {"auroran", "auroran", "crystal_one", "moss_one", "wailer", "wyvernkin"};
        double[] expected = {8.5, 11.5, 9, 12.5, 11.5, 11.5};
        for (int i = 0; i < races.length; i++) {
            TestPlayer p = player(h, races[i]);
            DamageSource source = switch (i) {
                case 0, 2 -> p.damageSources().magic();
                case 3 -> p.damageSources().inFire();
                case 5 -> p.damageSources().arrow(new net.minecraft.world.entity.projectile.Arrow(h.getLevel(), p), p);
                default -> p.damageSources().playerAttack(p);
            };
            var event = new LivingHurtEvent(p, source, 10); MinecraftForge.EVENT_BUS.post(event);
            h.assertTrue(Math.abs(event.getAmount() - expected[i]) < .001, "Damage affinity failed for " + races[i] + ": " + event.getAmount());
            AbilityService.clear(p, true);
        }
        h.succeed();
    }
    @GameTest(templateNamespace = "runic_races", template = "empty", timeoutTicks = 80)
    public static void dimensionAndCloneHooksClearEveryCastShape(GameTestHelper h) {
        for (String race : List.of("colossan", "astral_elf", "moss_one", "chelon")) {
            TestPlayer p = player(h, race); AbilityService.input(p, 1, true);
            var before = AbilityService.session(p); int debt = before.cooldowns.getOrDefault(before.tuning.kind(), 0);
            var oldGeneration = before.generation;
            AbilityEvents.dimension(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(p,
                    net.minecraft.world.level.Level.OVERWORLD, net.minecraft.world.level.Level.NETHER));
            h.assertTrue(before.anchor == null && before.field == null && !before.prepared && !before.live(AbilityService.now(p))
                    && !oldGeneration.equals(before.generation) && AbilityService.existingWards(p) == null, "Dimension hook retained transient state: " + race);
            TestPlayer next = player(h, race);
            AbilityService.clear(next, true); next.setUUID(p.getUUID());
            AbilityEvents.clone(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(next, p, true));
            AbilityService.tick(next);
            h.assertTrue(AbilityService.session(next).cooldowns.getOrDefault(before.tuning.kind(), 0) >= debt - 1, "Clone lost cooldown debt: " + race);
            h.assertTrue(!AbilityService.session(next).live(AbilityService.now(next)), "Clone revived a cast: " + race);
            AbilityService.clear(next, true);
        }
        h.succeed();
    }
}
