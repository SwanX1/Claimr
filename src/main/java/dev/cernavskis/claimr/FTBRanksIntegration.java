// This is free and unencumbered software released into the public domain.
package dev.cernavskis.claimr;

import javax.annotation.Nullable;

import com.feed_the_beast.mods.ftbranks.api.FTBRanksAPI;

import dev.cernavskis.claimr.util.ClaimrUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;

public class FTBRanksIntegration {
  
  @Nullable
  public static boolean canEnableAdminMode(PlayerEntity player) {
    if (Claimr.ftbranks) {
      return FTBRanksAPI.getPermissionValue((ServerPlayerEntity)player, "claimr.admin_mode").asBoolean().orElse(ClaimrUtil.isPlayerOpped(player));
    } else {
      return ClaimrUtil.isPlayerOpped(player);
    }
  }

  @Nullable
  public static boolean canEditAnyClaim(PlayerEntity player) {
    if (Claimr.ftbranks) {
      return FTBRanksAPI.getPermissionValue((ServerPlayerEntity)player, "claimr.ignore_claims").asBoolean().orElse(ClaimrUtil.isPlayerOpped(player));
    } else {
      return ClaimrUtil.isPlayerOpped(player);
    }
  }
}