package dev.cernavskis.claimr;

import java.util.Collection;
import java.util.Iterator;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import dev.cernavskis.claimr.data.ClaimData;
import dev.cernavskis.claimr.util.ChunkDimPos;
import dev.cernavskis.claimr.util.ClaimGroup;
import dev.cernavskis.claimr.util.ClaimrUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class ClaimrCommands {
  public static void registerCommands(CommandDispatcher<CommandSource> dispatcher) {
    LiteralCommandNode<CommandSource> claiminfoCommandNode =
      dispatcher.register(
        Commands.literal("claiminfo")
          .executes(context -> claiminfo(context))
      );

    dispatcher.register(
      Commands.literal("ci")
        .then(claiminfoCommandNode)
    );

    LiteralCommandNode<CommandSource> claimCommand =
      dispatcher.register(
        Commands.literal("claim")
          .executes(context -> claim(context))
          .then(
            Commands.argument("group", StringArgumentType.word())
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
          .executes(context -> unclaimall(context))
          .then(
            Commands.argument("group", StringArgumentType.word())
              .executes(context -> unclaimall(context, StringArgumentType.getString(context, "group")))
          )
      );

    LiteralCommandNode<CommandSource> trustCommand =
      dispatcher.register(
        Commands.literal("trust")
          .then(
            Commands.argument("players", GameProfileArgument.gameProfile())
              .suggests((context, suggestions) ->
                ISuggestionProvider.suggest(context.getSource().getServer().getPlayerList().getOnlinePlayerNames(), suggestions)
              )
              .executes(context -> trust(context, GameProfileArgument.getGameProfiles(context, "players")))
          )
      );

    LiteralCommandNode<CommandSource> untrustCommand =
      dispatcher.register(
        Commands.literal("untrust")
          .then(
            Commands.argument("players", GameProfileArgument.gameProfile())
              .suggests((context, suggestions) ->
                ISuggestionProvider.suggest(context.getSource().getServer().getPlayerList().getOnlinePlayerNames(), suggestions)
              )
              .executes(context -> untrust(context, GameProfileArgument.getGameProfiles(context, "players")))
          )
      );

    LiteralCommandNode<CommandSource> listtrustedCommand =
      dispatcher.register(
        Commands.literal("listtrusted")
          .executes(context -> listtrusted(context))
      );

    LiteralCommandNode<CommandSource> groupCommand =
      dispatcher.register(
        Commands.literal("group")
          .then(
            Commands.literal("create")
              .then(
                Commands.argument("group", StringArgumentType.word())
                  .executes(context -> createGroup(context, StringArgumentType.getString(context, "group")))
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
          .then(Commands.literal("ci")        .then(claiminfoCommandNode)) // /claimr ci          -> /claiminfo
          .then(Commands.literal("claiminfo") .then(claiminfoCommandNode)) // /claimr claiminfo   -> /claiminfo
          .then(Commands.literal("claim")     .then(claimCommand))         // /claimr claim       -> /claim
          .then(Commands.literal("unclaim")   .then(unclaimCommand))       // /claimr unclaim     -> /unclaim
          .then(Commands.literal("unclaimall").then(unclaimAllCommand))    // /claimr unclaimall  -> /unclaimall
          .then(Commands.literal("trust")     .then(trustCommand))         // /claimr trust       -> /trust
          .then(Commands.literal("untrust")   .then(untrustCommand))       // /claimr untrust     -> /untrust
          .then(Commands.literal("listtrused").then(listtrustedCommand))   // /claimr listtrusted -> /listtrusted
          .then(Commands.literal("group")     .then(groupCommand))         // /claimr group       -> /group
      );
  }

  private static int createGroup(CommandContext<CommandSource> context, String id) throws CommandSyntaxException {
    CommandSource source = context.getSource();
    ClaimGroup group = ClaimGroup.getGroup(id);
    if (group == null) {
      group = ClaimGroup.getOrCreateGroup(id, source.asPlayer(), false);
      source.sendFeedback(new StringTextComponent("Created a new group with the name " + group.getName()), false);
      return 1;
    } else {
      source.sendErrorMessage(new StringTextComponent("The name you provided is already in use."));
      return 0;
    }
  }

  private static int trust(CommandContext<CommandSource> context, Collection<GameProfile> players)
      throws CommandSyntaxException {
    CommandSource source = context.getSource();
    ClaimGroup group = ClaimGroup.getGroup(ClaimrUtil.getUUID(source.asPlayer()).toString());
    Iterator<GameProfile> playerIter = players.iterator();

    while (playerIter.hasNext()) {
      GameProfile playerProfile = (GameProfile) playerIter.next();
      group.setRank(PlayerEntity.getUUID(playerProfile), 1);
    }

    if (players.size() == 1) {
      source.sendFeedback(
          new StringTextComponent("Added " + ((GameProfile) players.toArray()[0]).getName() + " to trusted players!"),
          false);
    } else {
      source.sendFeedback(new StringTextComponent("Added " + players.size() + " players to trusted players!"), false);
    }
    return 0;
  }

  private static int untrust(CommandContext<CommandSource> context, Collection<GameProfile> players)
      throws CommandSyntaxException {
    CommandSource source = context.getSource();
    ClaimGroup group = ClaimGroup.getGroup(ClaimrUtil.getUUID(source.asPlayer()).toString());
    Iterator<GameProfile> playerIter = players.iterator();

    while (playerIter.hasNext()) {
      GameProfile playerProfile = (GameProfile) playerIter.next();
      group.setRank(PlayerEntity.getUUID(playerProfile), 0);
    }

    if (players.size() == 1) {
      source.sendFeedback(new StringTextComponent(
          "Removed " + ((GameProfile) players.toArray()[0]).getName() + " from trusted players!"), false);
    } else {
      source.sendFeedback(new StringTextComponent("Removed " + players.size() + " players from trusted players!"),
          false);
    }
    return 0;
  }

  private static int listtrusted(CommandContext<CommandSource> context) throws CommandSyntaxException {
    CommandSource source = context.getSource();

    ClaimGroup group = ClaimGroup.getGroup(ClaimrUtil.getUUID(source.asPlayer()).toString());
    source.sendFeedback(new StringTextComponent("Trusted Members, Total: " + group.members.size()), false);
    group.members.forEach((memberuuid, rank) -> {
      source.sendFeedback(new StringTextComponent(ClaimrUtil.getPlayerName(memberuuid, true)), false);
    });

    return 0;
  }

  private static int unclaimall(CommandContext<CommandSource> context) throws CommandSyntaxException {
    CommandSource source = context.getSource();
    return unclaimall(context, ClaimGroup.getGroup(ClaimrUtil.getUUID(source.asPlayer()).toString()));
  }

  private static int unclaimall(CommandContext<CommandSource> context, String id) throws CommandSyntaxException {
    CommandSource source = context.getSource();
    ClaimGroup group = ClaimGroup.getGroup(id);
    if (group == null) {
      source.sendErrorMessage(new StringTextComponent("The group " + id + " does not exist!"));
      return 0;
    }
    return unclaimall(context, group);
  }

  private static int unclaimall(CommandContext<CommandSource> context, ClaimGroup group) throws CommandSyntaxException {
    CommandSource source = context.getSource();
    if (group.canManage(source.asPlayer())) {
      final int startSize = Claimr.claimdata.data.size();
      Claimr.claimdata.data.forEach((pos, iteratedgroup) -> {
        if (iteratedgroup == group) {
          Claimr.claimdata.setGroup(pos, ClaimGroup.EVERYONE);
        }
      });
      final int endSize = Claimr.claimdata.data.size();
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
    ClaimGroup oldGroup = Claimr.claimdata.getGroup(pos);
    if (oldGroup != ClaimGroup.EVERYONE) {
      if (oldGroup.canManage(source.asPlayer())) {
        Claimr.claimdata.setGroup(pos, ClaimGroup.EVERYONE);
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

  private static int claim(CommandContext<CommandSource> context) throws CommandSyntaxException {
    CommandSource source = context.getSource();
    return claim(context,
        ClaimGroup.getOrCreateGroup(ClaimrUtil.getUUID(source.asPlayer()).toString(), source.asPlayer(), true));
  }

  private static int claim(CommandContext<CommandSource> context, String id) throws CommandSyntaxException {
    ClaimGroup group = ClaimGroup.getGroup(id);
    if (group == null) {
      context.getSource().sendErrorMessage(new StringTextComponent("The group " + id + " does not exist!"));
      return 0;
    } else {
      return claim(context, group);
    }
  }

  private static int claim(CommandContext<CommandSource> context, ClaimGroup group) throws CommandSyntaxException {
    return claim(context, new ChunkDimPos(context.getSource().getWorld(), new BlockPos(context.getSource().getPos())),
        group);
  }

  private static int claim(CommandContext<CommandSource> context, ChunkDimPos pos, ClaimGroup group)
      throws CommandSyntaxException {
    CommandSource source = context.getSource();
    ClaimGroup oldGroup = Claimr.claimdata.getGroup(pos);
    if (oldGroup == ClaimGroup.EVERYONE) {
      if (group.canManage(source.asPlayer())) {
        Claimr.claimdata.setGroup(pos, group);
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
    ClaimGroup group = Claimr.claimdata.getGroup(pos);
    source.sendFeedback(new StringTextComponent(
        group == ClaimGroup.EVERYONE ? "This chunk is unclaimed" : "Claimed by: " + group.getName()), false);
    return 1;
  }

  private static int help(CommandContext<CommandSource> context) {
    CommandSource source = context.getSource();
    source.sendFeedback(
        new StringTextComponent(
            "Documentation for this mod is available on the GitHub Wiki: https://github.com/SwanX1/Claimr/wiki"),
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
      source.sendFeedback(
          new StringTextComponent(
              "Claimr Directory: " + Claimr.claimdata.dataDirectory.resolve(ClaimData.DATA_DIR_NAME.toString())),
          false);
      source.sendFeedback(new StringTextComponent("Size of ChunkData: " + Claimr.claimdata.getSize() + " entries"),
          false);
    }
    return 0;
  }
}
