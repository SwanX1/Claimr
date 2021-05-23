// This is free and unencumbered software released into the public domain.
package dev.cernavskis.claimr;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

import dev.cernavskis.claimr.util.ChunkDimPos;
import dev.cernavskis.claimr.util.ClaimGroup;
import dev.cernavskis.claimr.util.ClaimrUtil;
import dev.cernavskis.claimr.util.IClaimGroup;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;

public class ClaimrCommands {
  public static CompletableFuture<Suggestions> suggestManagingGroups(CommandContext<CommandSource> context, SuggestionsBuilder suggestions) throws CommandSyntaxException {
    return ISuggestionProvider.suggest(Claimr.DATA.getManagingGroupNames(context.getSource().asPlayer()), suggestions);
  }
  
  public static CompletableFuture<Suggestions> suggestOwningGroups(CommandContext<CommandSource> context, SuggestionsBuilder suggestions) throws CommandSyntaxException {
    return ISuggestionProvider.suggest(Claimr.DATA.getOwningGroupNames(context.getSource().asPlayer()), suggestions);
  }

  public static void registerCommands(CommandDispatcher<CommandSource> dispatcher) {
    LiteralCommandNode<CommandSource> claiminfoCommandNode =
      dispatcher.register(
        Commands.literal("claiminfo")
          .executes(context -> claiminfo(context))
      );

    LiteralCommandNode<CommandSource> claimCommand =
      dispatcher.register(
        Commands.literal("claim")
          .then(
            Commands.argument("group", StringArgumentType.word())
              .suggests(ClaimrCommands::suggestManagingGroups)
              .executes(context -> claim(context, StringArgumentType.getString(context, "group")))
          )
      );

    LiteralCommandNode<CommandSource> unclaimCommand =
      dispatcher.register(
        Commands.literal("unclaim")
          .executes(context -> unclaim(context))
      );

    LiteralCommandNode<CommandSource> unclaimAllCommand =
      dispatcher.register(
        Commands.literal("unclaimall")
          .then(
            Commands.argument("group", StringArgumentType.word())
              .suggests(ClaimrCommands::suggestManagingGroups)
              .executes(context -> unclaimall(context, StringArgumentType.getString(context, "group")))
          )
      );

    LiteralCommandNode<CommandSource> groupCommand =
      dispatcher.register(
        Commands.literal("group")
          .then(
            Commands.literal("create")
              .then(
                Commands.argument("group", StringArgumentType.word())
                  .executes(context -> groupCreate(context, StringArgumentType.getString(context, "group")))
              )
          )
          .then(
            Commands.literal("add")
              .then(
                Commands.argument("group", StringArgumentType.word())
                  .suggests(ClaimrCommands::suggestManagingGroups)
                  .then(
                    Commands.argument("players", EntityArgument.players())
                      .executes(context ->
                        groupAddMember(
                          context,
                          StringArgumentType.getString(context, "group"),
                          EntityArgument.getPlayers(context, "players")
                        )
                      )
                  )
              )
          )
          .then(
            Commands.literal("remove")
              .then(
                Commands.argument("group", StringArgumentType.word())
                  .suggests(ClaimrCommands::suggestManagingGroups)
                  .then(
                    Commands.argument("players", EntityArgument.players())
                      .executes(context ->
                        groupRemoveMember(
                          context,
                          StringArgumentType.getString(context, "group"),
                          EntityArgument.getPlayers(context, "players")
                        )
                      )
                  )
              )
          )
          .then(
            Commands.literal("promote")
              .then(
                Commands.argument("group", StringArgumentType.word())
                  .suggests(ClaimrCommands::suggestOwningGroups)
                  .then(
                    Commands.argument("players", EntityArgument.players())
                      .executes(context ->
                        groupPromoteMember(
                          context,
                          StringArgumentType.getString(context, "group"),
                          EntityArgument.getPlayers(context, "players")
                        )
                      )
                  )
              )
          )
          .then(
            Commands.literal("demote")
              .then(
                Commands.argument("group", StringArgumentType.word())
                  .suggests(ClaimrCommands::suggestOwningGroups)
                  .then(
                    Commands.argument("players", EntityArgument.players())
                      .executes(context ->
                        groupDemoteMember(
                          context,
                          StringArgumentType.getString(context, "group"),
                          EntityArgument.getPlayers(context, "players")
                        )
                      )
                  )
              )
          )
          .then(
            Commands.literal("transferownership")
              .then(
                Commands.argument("group", StringArgumentType.word())
                  .suggests(ClaimrCommands::suggestOwningGroups)
                  .then(
                    Commands.argument("player", EntityArgument.player())
                      .executes(context ->
                        groupTransferOwnership(
                          context,
                          StringArgumentType.getString(context, "group"),
                          EntityArgument.getPlayer(context, "player")
                        )
                      )
                  )
              )
          )
      );

    LiteralCommandNode<CommandSource> baseCommand =
      dispatcher.register(
        Commands.literal("claimr")
          .executes(context -> help(context)) // /claimr -> /claimr help
          .then(
            Commands.literal("help")
              .executes(context -> help(context)) // /claimr help
          )
          .then(
            Commands.literal("info")
              .executes(context -> info(context)) // /claimr info -> /claimr info false
              .then(
                Commands.argument("debug", BoolArgumentType.bool())
                  .requires(context -> context.hasPermissionLevel(3))
                  .executes(context -> info(context, BoolArgumentType.getBool(context, "debug"))) // /claimr info [<debug>]
              )
          )
          .then(claiminfoCommandNode) // /claimr claiminfo   -> /claiminfo
          .then(claimCommand)         // /claimr claim       -> /claim
          .then(unclaimCommand)       // /claimr unclaim     -> /unclaim
          .then(unclaimAllCommand)    // /claimr unclaimall  -> /unclaimall
          .then(groupCommand)         // /claimr group       -> /group
      );
  }

  private static int groupTransferOwnership(CommandContext<CommandSource> context, String name, ServerPlayerEntity player) throws CommandSyntaxException {
    CommandSource source = context.getSource();
    IClaimGroup group = ClaimGroup.getGroup(name);
    if (group != null) {
      PlayerEntity executingPlayer = source.asPlayer();
      if (group.isOwner(executingPlayer)) {
        group.setOwner(player);
        group.setRank(executingPlayer, 2);
        source.sendFeedback(new StringTextComponent("Player " + ClaimrUtil.getPlayerName(player, true) + " is now the owner of \u00a76" + group.getName() + "\u00a7r"), false);
        return 1;
      } else {
        source.sendErrorMessage(new StringTextComponent("You are not the group owner!"));
        return 0;
      }
    } else {
      source.sendErrorMessage(new StringTextComponent("The group " + name + " doesn't exist!"));
      return 0;
    }
  }

  private static int groupAddMember(CommandContext<CommandSource> context, String name, Collection<ServerPlayerEntity> players) throws CommandSyntaxException {
    CommandSource source = context.getSource();
    IClaimGroup group = ClaimGroup.getGroup(name);
    if (group != null) {
      PlayerEntity executingPlayer = source.asPlayer();
      if (group.canManage(executingPlayer)) {
        if (players.size() == 1) {
          PlayerEntity player = players.iterator().next();
          if (group.getRank(player) < 1) {
            group.setRank(player, 1);
          }
          source.sendFeedback(new StringTextComponent("Added " + ClaimrUtil.getPlayerName(player, true) + " as a member."), false);
        } else {
          int addedPlayers = 0;
          for (PlayerEntity player : players) {
            if (group.getRank(player) < 1) {
              group.setRank(player, 1);
              addedPlayers++;
            }
          }
          source.sendFeedback(new StringTextComponent("Added " + addedPlayers + " players as members."), false);
        }
        return 1;
      } else {
        source.sendErrorMessage(new StringTextComponent("You are not a group manager!"));
        return 0;
      }
    } else {
      source.sendErrorMessage(new StringTextComponent("The group " + name + " doesn't exist!"));
      return 0;
    }
  }
  
  private static int groupRemoveMember(CommandContext<CommandSource> context, String name, Collection<ServerPlayerEntity> players) throws CommandSyntaxException {
    CommandSource source = context.getSource();
    IClaimGroup group = ClaimGroup.getGroup(name);
    if (group != null) {
      PlayerEntity executingPlayer = source.asPlayer();
      if (group.canManage(executingPlayer)) {
        if (players.size() == 1) {
          PlayerEntity player = players.iterator().next();
          if (group.getRank(player) == 1) {
            group.setRank(player, 0);
          }
          source.sendFeedback(new StringTextComponent("Removed member " + ClaimrUtil.getPlayerName(player, true) + " from group."), false);
        } else {
          int demotedPlayers = 0;
          for (PlayerEntity player : players) {
            if (group.getRank(player) == 1) {
              group.setRank(player, 0);
              demotedPlayers++;
            }
          }
          source.sendFeedback(new StringTextComponent("Removed " + demotedPlayers + " players from group."), false);
        }
        return 1;
      } else {
        source.sendErrorMessage(new StringTextComponent("You are not a group manager!"));
        return 0;
      }
    } else {
      source.sendErrorMessage(new StringTextComponent("The group " + name + " doesn't exist!"));
      return 0;
    }
  }
  
  private static int groupPromoteMember(CommandContext<CommandSource> context, String name, Collection<ServerPlayerEntity> players) throws CommandSyntaxException {
    CommandSource source = context.getSource();
    IClaimGroup group = ClaimGroup.getGroup(name);
    if (group != null) {
      PlayerEntity executingPlayer = source.asPlayer();
      if (group.isOwner(executingPlayer)) {
        if (players.size() == 1) {
          PlayerEntity player = players.iterator().next();
          if (group.getRank(player) < 2) {
            group.setRank(player, 2);
          }
          source.sendFeedback(new StringTextComponent("Promoted player " + ClaimrUtil.getPlayerName(player, true) + " to manager."), false);
        } else {
          int addedPlayers = 0;
          for (PlayerEntity player : players) {
            if (group.getRank(player) < 2) {
              group.setRank(player, 2);
              addedPlayers++;
            }
          }
          source.sendFeedback(new StringTextComponent("Promoted " + addedPlayers + " players to managers."), false);
        }
        return 1;
      } else {
        source.sendErrorMessage(new StringTextComponent("You are not the group owner!"));
        return 0;
      }
    } else {
      source.sendErrorMessage(new StringTextComponent("The group " + name + " doesn't exist!"));
      return 0;
    }
  }

  private static int groupDemoteMember(CommandContext<CommandSource> context, String name, Collection<ServerPlayerEntity> players) throws CommandSyntaxException {
    CommandSource source = context.getSource();
    IClaimGroup group = ClaimGroup.getGroup(name);
    if (group != null) {
      PlayerEntity executingPlayer = source.asPlayer();
      if (group.isOwner(executingPlayer)) {
        if (players.size() == 1) {
          PlayerEntity player = players.iterator().next();
          if (group.getRank(player) > 1) {
            group.setRank(player, 1);
          }
          source.sendFeedback(new StringTextComponent("Demoted manager " + ClaimrUtil.getPlayerName(player, true) + " to member."), false);
        } else {
          int demotedPlayers = 0;
          for (PlayerEntity player : players) {
            if (group.getRank(player) > 1) {
              group.setRank(player, 1);
              demotedPlayers++;
            }
          }
          source.sendFeedback(new StringTextComponent("Demoted " + demotedPlayers + " players to member."), false);
        }
        return 1;
      } else {
        source.sendErrorMessage(new StringTextComponent("You are not the group owner!"));
        return 0;
      }
    } else {
      source.sendErrorMessage(new StringTextComponent("The group " + name + " doesn't exist!"));
      return 0;
    }
  }

  private static int groupCreate(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
    CommandSource source = context.getSource();
    IClaimGroup group = ClaimGroup.getGroup(name);
    if (group == null) {
      group = ClaimGroup.getOrCreateGroup(name, source.asPlayer());
      source.sendFeedback(new StringTextComponent("Created a new group with the name " + group.getName()), false);
      return 1;
    } else {
      source.sendErrorMessage(new StringTextComponent("The name you provided is already in use."));
      return 0;
    }
  }

  private static int unclaimall(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
    CommandSource source = context.getSource();
    IClaimGroup group = ClaimGroup.getGroup(name);
    if (group == null) {
      source.sendErrorMessage(new StringTextComponent("The group " + name + " does not exist!"));
      return 0;
    }
    return unclaimall(context, group);
  }

  private static int unclaimall(CommandContext<CommandSource> context, IClaimGroup group) throws CommandSyntaxException {
    CommandSource source = context.getSource();
    if (group.canManage(source.asPlayer())) {
      final int startSize = Claimr.DATA.data.size();
      Claimr.DATA.data.forEach((pos, iteratedgroup) -> {
        if (iteratedgroup == group) {
          Claimr.DATA.setGroup(pos, ClaimGroup.EVERYONE);
        }
      });
      final int endSize = Claimr.DATA.data.size();
      source.sendFeedback(new StringTextComponent("Unclaimed " + (startSize - endSize) + " chunks!"), false);
      return 1;
    } else {
      source.sendErrorMessage(new StringTextComponent("You cannot manage this group!"));
      return 0;
    }
  }

  private static int unclaim(CommandContext<CommandSource> context) throws CommandSyntaxException {
    CommandSource source = context.getSource();
    return unclaim(context, new ChunkDimPos(source.getWorld(), new BlockPos(source.getPos())));
  }

  private static int unclaim(CommandContext<CommandSource> context, ChunkDimPos pos) throws CommandSyntaxException {
    CommandSource source = context.getSource();
    IClaimGroup oldGroup = Claimr.DATA.getGroup(pos);
    if (oldGroup != ClaimGroup.EVERYONE) {
      if (oldGroup.canManage(source.asPlayer())) {
        Claimr.DATA.setGroup(pos, ClaimGroup.EVERYONE);
        source.sendFeedback(new StringTextComponent("Unclaimed chunk [" + pos.toString() + "]"), false);
        return 1;
      } else {
        source.sendErrorMessage(new StringTextComponent("You cannot manage this group!"));
        return 0;
      }
    } else {
      source.sendErrorMessage(new StringTextComponent("This chunk is not claimed!"));
      return 0;
    }
  }
  private static int claim(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
    IClaimGroup group = ClaimGroup.getGroup(name);
    if (group == null) {
      context.getSource().sendErrorMessage(new StringTextComponent("The group " + name + " does not exist!"));
      return 0;
    } else {
      return claim(context, group);
    }
  }

  private static int claim(CommandContext<CommandSource> context, IClaimGroup group) throws CommandSyntaxException {
    return claim(context, new ChunkDimPos(context.getSource().getWorld(), new BlockPos(context.getSource().getPos())),
        group);
  }

  private static int claim(CommandContext<CommandSource> context, ChunkDimPos pos, IClaimGroup group)
      throws CommandSyntaxException {
    CommandSource source = context.getSource();
    IClaimGroup oldGroup = Claimr.DATA.getGroup(pos);
    if (oldGroup == ClaimGroup.EVERYONE) {
      if (group.canManage(source.asPlayer())) {
        Claimr.DATA.setGroup(pos, group);
        source.sendFeedback(new StringTextComponent("Claimed chunk [" + pos.toString() + "] for " + group.getName()),
            false);
        return 1;
      } else {
        context.getSource().sendErrorMessage(new StringTextComponent("You cannot manage this group!"));
        return 0;
      }
    } else {
      context.getSource().sendErrorMessage(new StringTextComponent("This chunk is already claimed!"));
      return 0;
    }
  }

  private static int claiminfo(CommandContext<CommandSource> context) {
    CommandSource source = context.getSource();
    ChunkDimPos pos = new ChunkDimPos(source.getWorld(), new BlockPos(source.getPos()));
    source.sendFeedback(new StringTextComponent("Chunk Location: " + pos.toString()), false);
    IClaimGroup group = Claimr.DATA.getGroup(pos);
    if (group == ClaimGroup.EVERYONE) {
      source.sendFeedback(new StringTextComponent("This chunk is unclaimed"), false);
    } else {
      source.sendFeedback(new StringTextComponent("Claimed by " + group.getName()), false);
    }
    return 1;
  }

  private static int help(CommandContext<CommandSource> context) {
    CommandSource source = context.getSource();
    source.sendFeedback(
      new StringTextComponent("Documentation for this mod is available on the GitHub Wiki: https://github.com/SwanX1/Claimr/wiki"),
      false);
    return 1;
  }

  private static int info(CommandContext<CommandSource> context) {
    return info(context, false);
  }

  private static int info(CommandContext<CommandSource> context, boolean debug) {
    CommandSource source = context.getSource();
    source.sendFeedback(new StringTextComponent("Claimr Version: " + Claimr.VERSION), false);
    source.sendFeedback(new StringTextComponent("Mod made by SwanX1, logo made by Nekomaster1000"), false);
    source.sendFeedback(new StringTextComponent("GitHub: https://github.com/SwanX1/Claimr"), false);
    if (Claimr.ftbranks) {
      source.sendFeedback(new StringTextComponent("FTB Ranks integration is enabled."), false);
    }
    if (debug) {
      source.sendFeedback(new StringTextComponent("Size of ClaimData: " + Claimr.DATA.getSize() + " entries"), false);
    }
    return 0;
  }
}
