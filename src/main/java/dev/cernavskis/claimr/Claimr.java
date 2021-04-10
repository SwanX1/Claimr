package dev.cernavskis.claimr;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.cernavskis.claimr.data.ClaimData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(Claimr.ID)
public class Claimr {
  public static final String ID = "claimr";
  public static final String VERSION = "1.0.5";
  public static final Logger LOGGER = LogManager.getLogger("Claimr");
  public static ClaimData claimdata;
  public static boolean ftbranks = false;

  public Claimr() {
    MinecraftForge.EVENT_BUS.register(new ClaimrEvents());
  }
}
