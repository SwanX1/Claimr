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

  @Nullable
  public static IClaimGroup getGroup(String name) {
    return ClaimData.groups.get(name);
  }

  public static IClaimGroup getOrCreateGroup(String name, PlayerEntity owner) {
    return getOrCreateGroup(name, ClaimrUtil.getUUID(owner));
  }

  public static IClaimGroup getOrCreateGroup(String name, UUID owner) {
    if (!ClaimData.groups.containsKey(name)) {
      Claimr.DATA.shouldSave = true;
      ClaimData.groups.put(name, new ClaimGroup(name, owner));
    }
    return ClaimData.groups.get(name);
  }

  public String getName() {
    return name;
  }

  public UUID getOwner() {
    return owner;
  }

  public UUID setOwner(PlayerEntity player) {
    return setOwner(ClaimrUtil.getUUID(player));
  }

  public UUID setOwner(UUID uuid) {
    this.owner = uuid;
    return this.owner;
  }

  public int getMembersSize() {
    return members.size();
  }

  public boolean canInteract(PlayerEntity player) {
    return getRank(player) >= 1 || isOwner(player);
  }

  public boolean canInteract(UUID uuid) {
    return getRank(uuid) >= 1 || isOwner(uuid);
  }

  public boolean canManage(PlayerEntity player) {
    return getRank(player) >= 2 || isOwner(player);
  }

  public boolean canManage(UUID uuid) {
    return getRank(uuid) >= 2 || isOwner(uuid);
  }

  public boolean isOwner(PlayerEntity player) {
    return isOwner(ClaimrUtil.getUUID(player));
  }

  public boolean isOwner(UUID uuid) {
    return uuid.equals(getOwner());
  }

  public int getRank(PlayerEntity player) {
    return getRank(ClaimrUtil.getUUID(player));
  }

  public int getRank(UUID uuid) {
    return isOwner(uuid) ? 3 : members.getOrDefault(uuid, 0);
  }

  public int setRank(PlayerEntity player, int rank) {
    return setRank(ClaimrUtil.getUUID(player), rank);
  }

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
    return String.format("{ClaimGroup \"%s\"; %s Members}", getName(), getMembersSize());
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
  }
}
