package com.otectus.runic_races.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.otectus.runic_races.util.RaceHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

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

    private static int listRaces(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal(
                "§6§lRunic Races §r§7— 24 races across 6 families\n" +
                "§eMortal:§r Human, Halfling, Nomad, Giant-Blooded\n" +
                "§dFae:§r High Elf, Wood Elf, Sprite, Changeling, Dryad\n" +
                "§aBeast:§r Wolfkin, Dragonborn, Catfolk, Minotaur, Serpentfolk\n" +
                "§8Underfolk:§r Mountain Dwarf, Deep Dwarf, Goblin, Troll, Kobold\n" +
                "§cDragon:§r Wyvern-Blooded, Elder Drake\n" +
                "§5Cursed:§r Vampire, Lycanthrope, Revenant"
        ), false);
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
}
