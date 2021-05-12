// This is free and unencumbered software released into the public domain.
package dev.cernavskis.claimr.util;

import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.OpEntry;

public class ClaimrUtil {
  public static MinecraftServer SERVER_INSTANCE;

  public static boolean isPlayerOpped(PlayerEntity player) {
    return getOpEntry(player) == null ? false : true;
  }

  @Nullable
  public static OpEntry getOpEntry(PlayerEntity player) {
    return SERVER_INSTANCE.getPlayerList().getOppedPlayers().getEntry(player.getGameProfile());
  }

  /**
   * Shorthand for getPlayerName(uuid, false);
   */
  @Nullable
  public static String getPlayerName(UUID uuid) {
    return getPlayerName(uuid, false);
  }


  /**
   * Gets player name from profile cache.
   * @return
   *  null if there is no name and returnUUID is false,
   *  uuid string if there is no name and returnUUID is true,
   *  player name if there is a name.
   */
  @Nullable
  public static String getPlayerName(UUID uuid, boolean returnUUID) {
    GameProfile profile = SERVER_INSTANCE.getPlayerProfileCache().getProfileByUUID(uuid);
    if (profile == null) {
      if (returnUUID) {
        return uuid.toString();
      } else {
        return null;
      }
    } else {
      return profile.getName();
    }
  }

  @Nullable
  public static GameProfile getPlayerProfile(UUID uuid) {
    return SERVER_INSTANCE.getPlayerProfileCache().getProfileByUUID(uuid);
  }

  public static UUID getUUID(PlayerEntity player) {
    return PlayerEntity.getUUID(player.getGameProfile());
  }
}
