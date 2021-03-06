package dev.cernavskis.claimr;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Claimr.MODID)
public class Claimr {
  public static final String MODID = "claimr";
  public static final String version = "0.0.1-SNAPSHOT";
  protected static final Logger LOGGER = LogManager.getLogger("Claimr");
  
  public static boolean ftbranks = false;

  public Claimr() {
    MinecraftForge.EVENT_BUS.register(this);
  }
  
  @SubscribeEvent
  public void init(FMLCommonSetupEvent event) {
    ftbranks = ModList.get().isLoaded("ftbranks");
  }
}
