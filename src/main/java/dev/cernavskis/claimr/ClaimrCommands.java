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

import dev.cernavskis.claimr.util.ChunkDimPos;
import dev.cernavskis.claimr.util.ClaimGroup;
import dev.cernavskis.claimr.util.ClaimrUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.player.PlayerEntity;
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
          .then(claiminfoCommandNode) // /claimr claiminfo   -> /claiminfo
          .then(claimCommand)         // /claimr claim       -> /claim
          .then(unclaimCommand)       // /claimr unclaim     -> /unclaim
          .then(unclaimAllCommand)    // /claimr unclaimall  -> /unclaimall
          .then(trustCommand)         // /claimr trust       -> /trust
          .then(untrustCommand)       // /claimr untrust     -> /untrust
          .then(listtrustedCommand)   // /claimr listtrusted -> /listtrusted
          .then(groupCommand)         // /claimr group       -> /group
      );
  }

  private static int createGroup(CommandContext<CommandSource> context, String id) throws CommandSyntaxException {
    CommandSource source = context.getSource();
    ClaimGroup group = ClaimGroup.getGroup(id);
    if (group == null) {
      group = ClaimGroup.getOrCreateGroup(id, source.asPlayer(), false);
      source.sendFeedback(new TranslationTextComponent("claimr.commands.group.create.success", group.getName()), false);
      return 1;
    } else {
      source.sendErrorMessage(new TranslationTextComponent("claimr.commands.group.create.exists"));
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
          new TranslationTextComponent("claimr.commands.trust.single", ((GameProfile) players.toArray()[0]).getName()),
          false);
    } else {
      source.sendFeedback(new TranslationTextComponent("claimr.commands.trust.multiple", players.size()), false);
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
      source.sendFeedback(new TranslationTextComponent("claimr.commands.untrust.single",
          ((GameProfile) players.toArray()[0]).getName()), false);
    } else {
      source.sendFeedback(new TranslationTextComponent("claimr.commands.untrust.multiple", players.size()), false);
    }
    return 0;
  }

  private static int listtrusted(CommandContext<CommandSource> context) throws CommandSyntaxException {
    CommandSource source = context.getSource();

    ClaimGroup group = ClaimGroup.getGroup(ClaimrUtil.getUUID(source.asPlayer()).toString());
    source.sendFeedback(new TranslationTextComponent("claimr.commands.listtrusted.info", group.members.size()), false);
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
      source.sendErrorMessage(new TranslationTextComponent("claimr.commands.unclaimall.groupnotexist", id));
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
      source.sendFeedback(new TranslationTextComponent("claimr.commands.unclaimall.success", startSize - endSize),
          false);
      return 1;
    } else {
      source.sendErrorMessage(new TranslationTextComponent("claimr.commands.unclaimall.nopermission"));
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
        source.sendFeedback(new TranslationTextComponent("claimr.commands.unclaim.success", "[" + pos.toString() + "]"),
            false);
        return 1;
      } else {
        source
            .sendErrorMessage(new TranslationTextComponent("claimr.commands.unclaim.nopermission", oldGroup.getName()));
        return 0;
      }
    } else {
      source.sendErrorMessage(new TranslationTextComponent("claimr.commands.unclaim.notclaimed"));
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
      context.getSource().sendErrorMessage(new TranslationTextComponent("command.claimr.claim.groupnotexist", id));
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
        source.sendFeedback(
            new TranslationTextComponent("command.claimr.claim.success", "[" + pos.toString() + "]", group.getName()),
            false);
        return 1;
      } else {
        context.getSource()
            .sendErrorMessage(new TranslationTextComponent("command.claimr.claim.nopermission", group.getName()));
        return 0;
      }
    } else {
      context.getSource().sendErrorMessage(new TranslationTextComponent("command.claimr.claim.claimed"));
      return 0;
    }
  }

  private static int claiminfo(CommandContext<CommandSource> context) {
    CommandSource source = context.getSource();
    ChunkDimPos pos = new ChunkDimPos(source.getWorld(), new BlockPos(source.getPos()));
    source.sendFeedback(new TranslationTextComponent("claimr.commands.claiminfo.location", "[" + pos.toString() + "]"),
        false);
    ClaimGroup group = Claimr.claimdata.getGroup(pos);
    if (group == ClaimGroup.EVERYONE) {
      source.sendFeedback(new TranslationTextComponent("claimr.commands.claiminfo.unclaimed"), false);
    } else {
      source.sendFeedback(new TranslationTextComponent("claimr.commands.claiminfo.claimed", group.getName()), false);
    }
    return 1;
  }

  private static int help(CommandContext<CommandSource> context) {
    CommandSource source = context.getSource();
    source.sendFeedback(new TranslationTextComponent("claimr.commands.help"), false);
    return 1;
  }

  private static int info(CommandContext<CommandSource> context) {
    return info(context, false);
  }

  private static int info(CommandContext<CommandSource> context, boolean debug) {
    CommandSource source = context.getSource();
    source.sendFeedback(new TranslationTextComponent("claimr.commands.info.version", Claimr.VERSION), false);
    source.sendFeedback(new TranslationTextComponent("claimr.commands.info.credits"), false);
    source.sendFeedback(new TranslationTextComponent("claimr.commands.info.github"), false);
    if (Claimr.ftbranks) {
      source.sendFeedback(new TranslationTextComponent("claimr.commands.info.ftbranksintegration"), false);
    }
    if (debug) {
      source.sendFeedback(
          new TranslationTextComponent("claimr.commands.info.debug.entries", Claimr.claimdata.getSize()), false);
    }
    return 0;
  }
}
