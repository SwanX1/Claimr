package dev.cernavskis.claimr;

import com.feed_the_beast.mods.ftbranks.api.FTBRanksAPI;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;

public class FTBRanksIntegration {
  public static boolean canEnableAdminMode(PlayerEntity player) {
    return FTBRanksAPI.getPermissionValue((ServerPlayerEntity)player, "claimr.admin_mode").asBoolean().orElse(false);
  }

  public static boolean canEditAnyClaim(PlayerEntity player) {
    return FTBRanksAPI.getPermissionValue((ServerPlayerEntity)player, "claimr.ignore_claims").asBoolean().orElse(false);
  }
}