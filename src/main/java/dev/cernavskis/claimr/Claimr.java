// This is free and unencumbered software released into the public domain.
package dev.cernavskis.claimr;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.cernavskis.claimr.data.ClaimData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(Claimr.ID)
public final class Claimr {
  public static final String ID = "claimr";
  public static final String VERSION = "1.1.4";
  public static final Logger LOGGER = LogManager.getLogger();
  public static ClaimData DATA;
  public static boolean ftbranks = false;

  public Claimr() {
    MinecraftForge.EVENT_BUS.register(new ClaimrEvents());
  }
}
