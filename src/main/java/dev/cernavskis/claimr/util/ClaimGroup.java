// This is free and unencumbered software released into the public domain.
package dev.cernavskis.claimr.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import dev.cernavskis.claimr.Claimr;
import dev.cernavskis.claimr.data.ClaimData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;

public class ClaimGroup implements IClaimGroup {
  public static final IClaimGroup EVERYONE = new EveryoneClaimGroup();
  public Map<UUID, Integer> members = new HashMap<UUID, Integer>();
  private final String name;
  private UUID owner;

  protected ClaimGroup(String nameIn, UUID ownerUUID) {
    name = nameIn;
    owner = ownerUUID;
  }

  /**
   * Gets group from created groups
   * @param name Name of the group to look for
   * @return IClaimGroup if group exists, null if group doesn't exist.
   */
  @Nullable
  public static IClaimGroup getGroup(String name) {
    return ClaimData.groups.get(name);
  }

  /**
   * Gets group from created groups, creates group if it doesn't exist
   * @param name Name of the group to look for or to create
   * @param owner Owner of the group, used only for creating a group, doesn't overwrite actual owner
   * @return IClaimGroup
   */
  public static IClaimGroup getOrCreateGroup(String name, PlayerEntity owner) {
    return getOrCreateGroup(name, ClaimrUtil.getUUID(owner));
  }

  /**
   * Gets group from created groups, creates group if it doesn't exist
   * @param name Name of the group to look for or to create
   * @param owner Owner of the group, used only for creating a group, doesn't overwrite actual owner
   * @return IClaimGroup
   */
  public static IClaimGroup getOrCreateGroup(String name, UUID owner) {
    if (!ClaimData.groups.containsKey(name)) {
      Claimr.DATA.shouldSave = true;
      ClaimData.groups.put(name, new ClaimGroup(name, owner));
    }
    return ClaimData.groups.get(name);
  }

  /**
   * Returns name of the group
   */
  public String getName() {
    return name;
  }

  /** 
   * Returns the UUID of the owner of the group
   */
  public UUID getOwner() {
    return owner;
  }

  /**
   * Sets new owner of the group
   * @return The UUID of the new owner
   */
  public UUID setOwner(PlayerEntity player) {
    return setOwner(ClaimrUtil.getUUID(player));
  }

  /**
   * Sets new owner of the group
   * @return The UUID of the new owner
   */
  public UUID setOwner(UUID uuid) {
    this.owner = uuid;
    Claimr.DATA.shouldSave = true;
    return this.owner;
  }

  /**
   * Use this instead of {@code IClaimGroup.getMembers().size()}.
   * {@link getMembers()} converts the member map to an ImmutableMap, this is faster.
   */
  public int getMembersSize() {
    return members.size();
  }

  /**
   * @return True, if the rank of the player is >= 1, or if the player is the owner of the group.
   */
  public boolean canInteract(PlayerEntity player) {
    return getRank(player) >= 1 || isOwner(player);
  }

  /**
   * @return True, if the rank of the player is >= 1, or if the player is the owner of the group.
   */
  public boolean canInteract(UUID uuid) {
    return getRank(uuid) >= 1 || isOwner(uuid);
  }

  /**
   * @return True, if the rank of the player is >= 2, or if the player is the owner of the group.
   */
  public boolean canManage(PlayerEntity player) {
    return getRank(player) >= 2 || isOwner(player);
  }

  /**
   * @return True, if the rank of the player is >= 2, or if the player is the owner of the group.
   */
  public boolean canManage(UUID uuid) {
    return getRank(uuid) >= 2 || isOwner(uuid);
  }

  /**
   * @return True, if the player is the owner of the group.
   */
  public boolean isOwner(PlayerEntity player) {
    return isOwner(ClaimrUtil.getUUID(player));
  }

  /**
   * @return True, if the player is the owner of the group.
   */
  public boolean isOwner(UUID uuid) {
    return uuid.equals(getOwner());
  }

  /**
   * @return The rank of the player, see wiki entry for what each rank means.
   */
  public int getRank(PlayerEntity player) {
    return getRank(ClaimrUtil.getUUID(player));
  }

  /**
   * @return The rank of the player, see wiki entry for what each rank means.
   */
  public int getRank(UUID uuid) {
    return isOwner(uuid) ? 3 : members.getOrDefault(uuid, 0);
  }

  /**
   * Sets a new rank for the player. If the given player is the owner of the group, rank stays unchanged.
   * @return The new rank of the player. If rank is less than 0 or greater than 2,
   * the rank of the player stays unchanged, and the rank of the player is returned.
   */
  public int setRank(PlayerEntity player, int rank) {
    return setRank(ClaimrUtil.getUUID(player), rank);
  }

  /**
   * Sets a new rank for the player. If the given player is the owner of the group, rank stays unchanged.
   * @return The new rank of the player. If rank is less than 0 or greater than 2,
   * the rank of the player stays unchanged, and the rank of the player is returned.
   */
  public int setRank(UUID uuid, int rank) {
    if (isOwner(uuid)) {
      return 3;
    }
    if (rank < 0 || rank > 2) {
      return getRank(uuid);
    } else {
      if (getRank(uuid) > 0) {
        if (rank != 0) {
          members.replace(uuid, rank);
        } else {
          members.remove(uuid);
        }
      } else {
        members.put(uuid, rank);
      }
      Claimr.DATA.shouldSave = true;
      return rank;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ClaimGroup) {
      return this.name == ((ClaimGroup) obj).name;
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return String.format("{%s \"%s\"; %s Members}", this.getClass().getName(), getName(), getMembersSize());
  }

  public ImmutableMap<UUID, Integer> getMembers() {
    return ImmutableMap.copyOf(this.members);
  }

  /**
   * Utility class for a static field that allows everyone to interact with a claim, but no one to edit it.
   */
  private static final class EveryoneClaimGroup implements IClaimGroup {
    private String name;

    public EveryoneClaimGroup() {
      this.name = "everyone";
      ClaimData.groups.put(this.getName(), this);
    }

    @Override
    public int getMembersSize() {
      return 0;
    }

    @Override
    public boolean canInteract(PlayerEntity player) {
      return true;
    }

    @Override
    public boolean canInteract(UUID uuid) {
      return true;
    }

    @Override
    public boolean canManage(PlayerEntity player) {
      return false;
    }

    @Override
    public boolean canManage(UUID uuid) {
      return false;
    }

    @Override
    public boolean isOwner(PlayerEntity player) {
      return false;
    }

    @Override
    public boolean isOwner(UUID uuid) {
      return false;
    }

    @Override
    public int getRank(PlayerEntity player) {
      return 1;
    }

    @Override
    public int getRank(UUID uuid) {
      return 1;
    }

    @Override
    public int setRank(PlayerEntity player, int rank) {
      return 1;
    }

    @Override
    public int setRank(UUID uuid, int rank) {
      return 1;
    }

    @Override
    public String getName() {
      return this.name;
    }

    @Override
    public UUID getOwner() {
      return Util.DUMMY_UUID;
    }

    @Override
    public UUID setOwner(PlayerEntity player) {
      return Util.DUMMY_UUID;
    }

    @Override
    public UUID setOwner(UUID uuid) {
      return Util.DUMMY_UUID;
    }

    @Override
    public ImmutableMap<UUID, Integer> getMembers() {
      return ImmutableMap.of();
    }

    @Override
    public String toString() {
      return String.format("{%s \"%s\"}", this.getClass().getName(), getName());
    }
  }
}
