package dev.cernavskis.claimr.data;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;

import dev.cernavskis.claimr.Claimr;
import dev.cernavskis.claimr.util.ClaimGroup;
import dev.cernavskis.claimr.util.ChunkDimPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.FolderName;

public class ClaimData {
  public static final FolderName DATA_DIR_NAME = new FolderName("data");
  public static final Map<String, ClaimGroup> groups = new ConcurrentHashMap<String, ClaimGroup>();
  public Map<ChunkDimPos, ClaimGroup> data = new ConcurrentHashMap<ChunkDimPos, ClaimGroup>();
  private static Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().serializeNulls().disableHtmlEscaping().create();
  public boolean shouldSave = false;
  private boolean initialized = false;
  public Path dataDirectory;
  public final MinecraftServer server;

  public ClaimData(MinecraftServer s) {
    server = s;
  }

  public void init() {
    if (initialized) return;
    initialized = false;
    dataDirectory = server.func_240776_a_(DATA_DIR_NAME);

    try {
      Files.createDirectories(dataDirectory);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    Path dataFile = dataDirectory.resolve("claimr.json");

    try {
      if (Files.notExists(dataFile)) {
        System.out.println("Claimr data file not found, creating one.");
        Files.createFile(dataFile);
        Files.writeString(dataFile, "{\n  \"chunks\": {},\n  \"groups\": {}\n}");
      }

      JsonObject jsondata;

      try (Reader reader = Files.newBufferedReader(dataFile)) {
				jsondata = GSON.fromJson(reader, JsonObject.class);
        JsonObject groupdata = jsondata.getAsJsonObject("groups");

        for (Map.Entry<String, JsonElement> entry : groupdata.entrySet()) {
          String id = entry.getKey();
          JsonObject group = entry.getValue().getAsJsonObject();

          boolean personal = group.get("personal").getAsBoolean();
          UUID owner = UUID.fromString(group.get("owner").getAsString());

          ClaimGroup claimgroup = ClaimGroup.getOrCreateGroup(id, owner, personal);

          for (Map.Entry<String, JsonElement> memberentry : group.getAsJsonObject("members").entrySet()) {
            UUID member = UUID.fromString(memberentry.getKey());
            int rank = memberentry.getValue().getAsInt();
            claimgroup.setRank(member, rank);
          }
        }

        JsonObject chunkdata = jsondata.getAsJsonObject("chunks");

        for (Map.Entry<String, JsonElement> entry : chunkdata.entrySet()) {
          ChunkDimPos pos = ChunkDimPos.parseChunkDimPos(entry.getKey());
          String groupid = entry.getValue().getAsString();
          ClaimGroup group = ClaimGroup.getGroup(groupid);

          if (group != null) {
            data.put(pos, group);
          } else {
            Claimr.LOGGER.error("Undefined group given: \"" + groupid + "\" for chunk [" + pos.toString() + "]");
          }
        }
        initialized = true;
      } catch (JsonIOException | IOException ex) {
        ex.printStackTrace();
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public void save() {
    if (!initialized) {
      Claimr.LOGGER.error("Cannot save Claimr data because it has not been initialized yet!");
      return;
    }
    if (!shouldSave) {
      return;
    }
    shouldSave = false;

    Claimr.LOGGER.info("Saving Claimr data...");
    JsonObject jsondata = new JsonObject();

    JsonObject chunkdata = new JsonObject();
    data.forEach((pos, group) -> {
      if (group != ClaimGroup.EVERYONE) {
        chunkdata.addProperty(pos.toString(), group.getId());
      }
    });
    jsondata.add("chunks", chunkdata);

    JsonObject groupdata = new JsonObject();
    groups.forEach((id, group) -> {
      JsonObject jsongroup = new JsonObject();
      JsonObject members = new JsonObject();
      group.members.forEach((uuid, rank) -> {
        if (rank == 0) return;
        members.addProperty(uuid.toString(), rank);
      });

      jsongroup.addProperty("personal", group.isPersonal());
      jsongroup.addProperty("owner", group.getOwner().toString());
      jsongroup.add("members", members);

      groupdata.add(id, jsongroup);
    });
    jsondata.add("groups", groupdata);

    Path dataFile = dataDirectory.resolve("claimr.json");

    try (Writer writer = Files.newBufferedWriter(dataFile)) {
      GSON.toJson(jsondata, writer);
      Claimr.LOGGER.info("Saved Claimr data!");
    } catch (Exception ex) {
      Claimr.LOGGER.error("Failed to save claimr.json! Error: " + ex);
    }
  }

  public int getSize() {
    return data.size() + groups.size();
  }

  public ClaimGroup getGroup(World world, BlockPos pos) {
    return getGroup(new ChunkDimPos(world, pos));
  }

  public ClaimGroup getGroup(ChunkDimPos pos) {
    return data.getOrDefault(pos, ClaimGroup.EVERYONE);
  }

  public ClaimGroup setGroup(World world, BlockPos pos, ClaimGroup group) {
    return setGroup(new ChunkDimPos(world, pos), group);
  }

  public ClaimGroup setGroup(ChunkDimPos pos, ClaimGroup group) {
    if (group == ClaimGroup.EVERYONE) {
      data.remove(pos);
      return ClaimGroup.EVERYONE;
    }
    shouldSave = true;
    if (data.containsKey(pos)) {
      data.replace(pos, group);
    } else {
      data.put(pos, group);
    }
    return data.get(pos);
  }
}
