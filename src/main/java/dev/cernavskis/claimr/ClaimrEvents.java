// This is free and unencumbered software released into the public domain.
package dev.cernavskis.claimr;

import dev.cernavskis.claimr.data.ClaimData;
import dev.cernavskis.claimr.util.ClaimGroup;
import dev.cernavskis.claimr.util.ClaimrUtil;
import dev.cernavskis.claimr.util.IClaimGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

@Mod.EventBusSubscriber(modid = Claimr.ID)
public class ClaimrEvents {
  public static ClaimData DATA;

  @SubscribeEvent
  public void server(FMLServerAboutToStartEvent event) {
    ClaimrUtil.SERVER_INSTANCE = event.getServer();
    Claimr.DATA = new ClaimData(event.getServer());
    ClaimrEvents.DATA = Claimr.DATA;
  }

  @SubscribeEvent
  public void init(FMLServerStartingEvent event) {
    Claimr.ftbranks = ModList.get().isLoaded("ftbranks");
    DATA.init();
  }

  @SubscribeEvent
  public void registerCommands(RegisterCommandsEvent event) {
    ClaimrCommands.registerCommands(event.getDispatcher());
  }

  @SubscribeEvent
  public void saveWorld(WorldEvent.Save event) {
    DATA.save();
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public void leftClick(PlayerInteractEvent.LeftClickBlock event) {
    IClaimGroup group = DATA.getGroup(event.getWorld(), event.getPos());
    if (!group.canInteract(event.getPlayer())) {
      sendClaimAlert(event.getPlayer(), group);
      event.setCanceled(true);
    }
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public void rightClick(PlayerInteractEvent.RightClickBlock event) {
    IClaimGroup group = DATA.getGroup(event.getWorld(), event.getPos());
    if (!group.canInteract(event.getPlayer())) {
      sendClaimAlert(event.getPlayer(), group);
      event.setCanceled(true);
    }
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public void farmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
    if (event.getEntity() instanceof PlayerEntity) {
      IClaimGroup group = DATA.getGroup((World) event.getWorld(), event.getPos());
      if (!group.canInteract((PlayerEntity) event.getEntity())) {
        sendClaimAlert((PlayerEntity) event.getEntity(), group);
        event.setCanceled(true);
      }
    }
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public void itemPickup(EntityItemPickupEvent event) {
    if (event.getPlayer() != null) {
      IClaimGroup group = DATA.getGroup(event.getItem().getEntityWorld(), event.getItem().getPosition());
      if (!group.canInteract(event.getPlayer())) {
        sendClaimAlert(event.getPlayer(), group);
        event.setCanceled(true);
      }
    }
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public void bonemealEvent(BonemealEvent event) {
    IClaimGroup group = DATA.getGroup(event.getWorld(), event.getPos());
    if (!group.canInteract(event.getPlayer())) {
      sendClaimAlert(event.getPlayer(), group);
      event.setResult(Result.DENY);
    }
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public void fillBucket(FillBucketEvent event) {
    IClaimGroup group = DATA.getGroup(event.getWorld(), new BlockPos(event.getTarget().getHitVec()));
    if (!group.canInteract(event.getPlayer())) {
      sendClaimAlert(event.getPlayer(), group);
      event.setResult(Result.DENY);
    }
  }

  // Alert player when entering new claim
  @SubscribeEvent(priority = EventPriority.LOW)
  public void enterChunk(EntityEvent.EnteringChunk event) {
    if (event.getEntity() instanceof PlayerEntity) {
      World world = event.getEntity().getEntityWorld();
      BlockPos newPos = new BlockPos(event.getNewChunkX() << 4, 0, event.getNewChunkZ() << 4);
      BlockPos oldPos = new BlockPos(event.getOldChunkX() << 4, 0, event.getOldChunkZ() << 4);
      IClaimGroup newGroup = DATA.getGroup(world, newPos);
      IClaimGroup oldGroup = DATA.getGroup(world, oldPos);
      if (newGroup != oldGroup) {
        PlayerEntity player = (PlayerEntity) event.getEntity();
        if (newGroup == ClaimGroup.EVERYONE) {
          player.sendStatusMessage(new StringTextComponent("Entering unclaimed land"), true);
        } else {
          player.sendStatusMessage(new StringTextComponent("Entering \u00a76" + newGroup.getId() + "\u00a7r's claim"), true);
        }
      }
    }
  }

  private void sendClaimAlert(PlayerEntity player, IClaimGroup group) {
    player.sendStatusMessage(
        new StringTextComponent("This chunk is claimed by " + group.getId()).mergeStyle(TextFormatting.RED), true);
  }
}
