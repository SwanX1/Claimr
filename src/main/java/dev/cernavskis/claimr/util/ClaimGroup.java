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
  private final String id;
  private final UUID owner;

  protected ClaimGroup(String idIn, UUID uuid) {
    id = idIn;
    owner = uuid;
  }

  @Nullable
  public static IClaimGroup getGroup(String id) {
    return ClaimData.groups.get(id);
  }

  public static IClaimGroup getOrCreateGroup(String id, PlayerEntity owner) {
    return getOrCreateGroup(id, ClaimrUtil.getUUID(owner));
  }

  public static IClaimGroup getOrCreateGroup(String id, UUID owner) {
    if (!ClaimData.groups.containsKey(id)) {
      Claimr.DATA.shouldSave = true;
      ClaimData.groups.put(id, new ClaimGroup(id, owner));
    }
    return ClaimData.groups.get(id);
  }

  public String getId() {
    return id;
  }

  public UUID getOwner() {
    return owner;
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
      return this.id == ((ClaimGroup) obj).id;
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return String.format("{ClaimGroup \"%s\"; %s Members}", getId(), getMembersSize());
  }

  public ImmutableMap<UUID, Integer> getMembers() {
    return ImmutableMap.copyOf(this.members);
  }

  /**
   * Utility class for a static field that allows everyone to interact with a claim, but no one to edit it.
   */
  private static final class EveryoneClaimGroup implements IClaimGroup {
    private String id;

    public EveryoneClaimGroup() {
      this.id = "everyone";
      ClaimData.groups.put(this.getId(), this);
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
    public String getId() {
      return this.id;
    }

    @Override
    public UUID getOwner() {
      return Util.DUMMY_UUID;
    }

    @Override
    public ImmutableMap<UUID, Integer> getMembers() {
      return ImmutableMap.of();
    }
  }
}
