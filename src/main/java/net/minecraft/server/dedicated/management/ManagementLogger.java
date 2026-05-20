package net.minecraft.server.dedicated.management;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.server.dedicated.management.network.ManagementConnectionId;
import org.slf4j.Logger;

public class ManagementLogger {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String LOG_PREFIX = "RPC Connection #{}: ";

   public void logAction(ManagementConnectionId remote, String action, Object... arguments) {
      if (arguments.length == 0) {
         LOGGER.info("RPC Connection #{}: " + action, remote.connectionId());
      } else {
         List<Object> list = new ArrayList<>(Arrays.asList(arguments));
         list.addFirst(remote.connectionId());
         LOGGER.info("RPC Connection #{}: " + action, list.toArray());
      }
   }
}
