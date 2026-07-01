package com.otectus.runic_races.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.otectus.runic_races.RunicRacesMod;
import com.otectus.runic_races.common.state.RaceStateFlags;
import com.otectus.runic_races.common.state.RaceStateTracker;
import com.otectus.runic_races.race.RaceDefinition;
import com.otectus.runic_races.race.RaceRegistry;
import com.otectus.runic_races.util.RaceHelper;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.registry.OriginsDynamicRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.StringJoiner;
import java.util.TreeMap;

public class RRCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("runicraces")
                .requires(source -> source.hasPermission(0))
                .then(Commands.literal("info")
                        .executes(RRCommands::infoSelf)
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(source -> source.hasPermission(2))
                                .executes(RRCommands::infoOther)))
                .then(Commands.literal("list")
                        .executes(RRCommands::listRaces))
                .then(Commands.literal("debug")
                        .requires(source -> source.hasPermission(2))
                        .executes(RRCommands::debug))
                .then(Commands.literal("validate")
                        .requires(source -> source.hasPermission(2))
                        .executes(RRCommands::validate))
                .then(Commands.literal("state")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(RRCommands::state)))
        );
    }

    private static int infoSelf(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        return showRaceInfo(context, player);
    }

    private static int infoOther(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            return showRaceInfo(context, player);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Player not found."));
            return 0;
        }
    }

    private static int showRaceInfo(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        String raceId = RaceHelper.getRaceId(player).map(Object::toString).orElse("none selected");
        context.getSource().sendSuccess(() ->
                Component.literal(player.getName().getString() + "'s race: " + raceId), false);
        return 1;
    }

    private static final Map<String, String> FAMILY_COLORS = Map.of(
            "human", "\u00A7e",
            "elven", "\u00A7d",
            "dwarven", "\u00A78",
            "bestial", "\u00A7a",
            "faeborne", "\u00A7b",
            "undead", "\u00A75",
            "draconic", "\u00A7c"
    );

    private static int listRaces(CommandContext<CommandSourceStack> context) {
        StringBuilder sb = new StringBuilder();
        sb.append("\u00A76\u00A7lRunic Races \u00A7r\u00A77\u2014 ")
                .append(RaceRegistry.raceCount())
                .append(" races across ")
                .append(RaceRegistry.allFamilies().size())
                .append(" families\n");

        for (String family : RaceRegistry.allFamilies()) {
            String color = FAMILY_COLORS.getOrDefault(family, "\u00A7f");
            String familyDisplay = family.substring(0, 1).toUpperCase() + family.substring(1);
            sb.append(color).append(familyDisplay).append(":\u00A7r ");

            List<RaceDefinition> races = RaceRegistry.racesInFamily(family);
            StringJoiner joiner = new StringJoiner(", ");
            for (RaceDefinition race : races) {
                joiner.add(race.displayName());
            }
            sb.append(joiner).append("\n");
        }

        String message = sb.toString().stripTrailing();
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int debug(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal(
                "§6[RunicRaces Debug]§r\n" +
                "Health: " + String.format("%.1f", player.getHealth()) + "/" + String.format("%.1f", player.getMaxHealth()) + "\n" +
                "Armor: " + player.getArmorValue() + "\n" +
                "Speed: " + String.format("%.4f", player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)) + "\n" +
                "Attack: " + String.format("%.2f", player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) + "\n" +
                "Attack Speed: " + String.format("%.2f", player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED)) + "\n" +
                "Knockback Resist: " + String.format("%.2f", player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)) + "\n" +
                "Luck: " + String.format("%.2f", player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.LUCK))
        ), false);
        return 1;
    }

    // === /runicraces validate ===

    /**
     * Server-side data validation for pack authors: checks every race in {@link RaceRegistry}
     * against the Origins/Apoli dynamic registries. Runs fine from the console (no player needed).
     */
    private static int validate(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();

        // Origins keeps its origins in a dynamic (datapack) registry; the key is exposed via
        // OriginsDynamicRegistries.ORIGINS_REGISTRY (equivalent to OriginsAPI.getOriginsRegistry(server)).
        Optional<Registry<Origin>> originsRegistry =
                server.registryAccess().registry(OriginsDynamicRegistries.ORIGINS_REGISTRY);
        if (originsRegistry.isEmpty()) {
            context.getSource().sendFailure(Component.literal(
                    "Origins dynamic registry is unavailable — is Origins Forge loaded?"));
            return 0;
        }
        Registry<Origin> origins = originsRegistry.get();
        // Apoli's configured-power dynamic registry, for dangling-power-id checks.
        Registry<ConfiguredPower<?, ?>> powers = ApoliAPI.getPowers(server);

        StringBuilder report = new StringBuilder("§6[RunicRaces Validate]§r\n");
        int passed = 0;
        int failed = 0;
        for (RaceDefinition race : RaceRegistry.allRaces()) {
            List<String> problems = new ArrayList<>();
            ResourceLocation originId = new ResourceLocation(RunicRacesMod.MOD_ID, race.name());

            Origin origin = origins.get(originId);
            if (origin == null) {
                problems.add("origin " + originId + " not registered");
            } else {
                // Preferred source of truth: the power list on the registry entry itself.
                List<ResourceLocation> powerIds = new ArrayList<>();
                int inlinePowers = 0;
                for (HolderSet<ConfiguredPower<?, ?>> set : origin.getPowers()) {
                    for (Holder<ConfiguredPower<?, ?>> holder : set) {
                        Optional<ResourceKey<ConfiguredPower<?, ?>>> key = holder.unwrapKey();
                        if (key.isPresent()) {
                            powerIds.add(key.get().location());
                        } else {
                            inlinePowers++; // direct/inline holder — present by construction
                        }
                    }
                }
                int total = powerIds.size() + inlinePowers;
                if (total != 3) {
                    problems.add("expected 3 powers, origin lists " + total);
                }
                for (ResourceLocation powerId : powerIds) {
                    if (!powers.containsKey(powerId)) {
                        problems.add("dangling power " + powerId + " (not in Apoli power registry)");
                    }
                }
            }

            if (problems.isEmpty()) {
                passed++;
                report.append("§a✓§r ").append(race.name()).append("\n");
            } else {
                failed++;
                report.append("§c✗ ").append(race.name()).append("§r — ")
                        .append(String.join("; ", problems)).append("\n");
            }
        }

        report.append("§7Totals: ").append(passed).append(" passed, ")
                .append(failed).append(" failed, ")
                .append(RaceRegistry.raceCount()).append(" races checked.");

        String message = report.toString();
        if (failed > 0) {
            context.getSource().sendFailure(Component.literal(message));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    // === /runicraces state <player> ===

    /** Mirrors RacialEventHandler.REAPER_REVIVAL_CD_TICKS (30 minutes). */
    private static final int REAPER_REVIVAL_CD_TICKS = 36000;

    private static int state(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            return showState(context, player);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Player not found."));
            return 0;
        }
    }

    private static int showState(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        StringBuilder sb = new StringBuilder();
        sb.append("§6[RunicRaces State] §r").append(player.getName().getString()).append("\n");

        // Race + family
        Optional<ResourceLocation> raceId = RaceHelper.getRaceId(player);
        String raceName = raceId.map(ResourceLocation::getPath).orElse(null);
        sb.append("Race: ").append(raceId.map(Object::toString).orElse("none selected"));
        if (raceName != null) {
            sb.append(" (family: ").append(RaceRegistry.getFamily(raceName)).append(")");
        }
        sb.append("\n");

        // Transient state flags (bitfield)
        int flags = RaceStateTracker.get(player);
        StringJoiner flagNames = new StringJoiner(", ");
        for (RaceStateFlags flag : RaceStateFlags.values()) {
            if (flag.isSet(flags)) {
                flagNames.add(flag.name());
            }
        }
        sb.append("State flags (0x").append(Integer.toHexString(flags)).append("): ")
                .append(flags == 0 ? "none" : flagNames.toString()).append("\n");

        // Persistent data
        CompoundTag data = player.getPersistentData();
        sb.append("Adaptation stacks: ").append(data.getInt("runic_races:human_adapt_stacks")).append("\n");
        if (data.contains("runic_races:revenant_revival_cd")) {
            long stampedAt = data.getLong("runic_races:revenant_revival_cd");
            long elapsed = player.level().getGameTime() - stampedAt;
            long remaining = Math.max(0, REAPER_REVIVAL_CD_TICKS - elapsed);
            sb.append("Reaper revival cooldown: ").append(remaining).append(" ticks remaining (")
                    .append(remaining / 20).append("s)\n");
        }
        if (data.contains("runic_races:last_synced_race")) {
            sb.append("Last synced race: ").append(data.getString("runic_races:last_synced_race")).append("\n");
        }

        // All runic_races cooldown resources currently on the player's Apoli power container.
        Map<String, String> cooldowns = new TreeMap<>();
        IPowerContainer container = IPowerContainer.get(player).orElse(null);
        if (container != null) {
            for (Holder<ConfiguredPower<?, ?>> holder : container.getPowers()) {
                if (holder == null || !holder.isBound()) {
                    continue;
                }
                Optional<ResourceKey<ConfiguredPower<?, ?>>> key = holder.unwrapKey();
                if (key.isEmpty()) {
                    continue;
                }
                ResourceLocation id = key.get().location();
                if (!RunicRacesMod.MOD_ID.equals(id.getNamespace()) || !id.getPath().endsWith("_cooldown_timer")) {
                    continue;
                }
                OptionalInt value = holder.value().getValue(player);
                cooldowns.put(id.getPath(), value.isPresent() ? String.valueOf(value.getAsInt()) : "<no value>");
            }
        }
        sb.append("Cooldown resources:");
        if (cooldowns.isEmpty()) {
            sb.append(" none");
        } else {
            for (Map.Entry<String, String> entry : cooldowns.entrySet()) {
                sb.append("\n  ").append(entry.getKey()).append(": ").append(entry.getValue());
            }
        }

        String message = sb.toString().stripTrailing();
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
