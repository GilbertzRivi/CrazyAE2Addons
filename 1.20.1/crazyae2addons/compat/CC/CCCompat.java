package net.oktawia.crazyae2addons.compat.CC;

import dan200.computercraft.api.ComputerCraftAPI;

public final class CCCompat {
  private CCCompat() {}
  public static void init() {
    ComputerCraftAPI.registerGenericSource(new PenroseFramePeripheral());
  }
}
