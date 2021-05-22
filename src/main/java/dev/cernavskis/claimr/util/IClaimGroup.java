// This is free and unencumbered software released into the public domain.
package dev.cernavskis.claimr.util;

import java.util.UUID;

import com.google.common.collect.ImmutableMap;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Utility interface for me to not fuck up implementations
 * @author SwanX1
 */
public interface IClaimGroup {
  public String getId();

  public UUID getOwner();
  public int getMembersSize();
  public ImmutableMap<UUID, Integer> getMembers();

  public boolean canInteract(PlayerEntity player);
  public boolean canInteract(UUID uuid);
  public boolean canManage(PlayerEntity player);
  public boolean canManage(UUID uuid);
  public boolean isOwner(PlayerEntity player);
  public boolean isOwner(UUID uuid);

  public int getRank(PlayerEntity player);
  public int getRank(UUID uuid);
  public int setRank(PlayerEntity player, int rank);
  public int setRank(UUID uuid, int rank);
}
