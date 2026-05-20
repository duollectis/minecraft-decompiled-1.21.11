package net.minecraft.client.network;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.logging.UncaughtExceptionLogger;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class LanServerPinger extends Thread {
   private static final AtomicInteger THREAD_ID = new AtomicInteger(0);
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String PING_ADDRESS = "224.0.2.60";
   public static final int PING_PORT = 4445;
   private static final long PING_INTERVAL = 1500L;
   private final String motd;
   private final DatagramSocket socket;
   private boolean running = true;
   private final String addressPort;

   public LanServerPinger(String motd, String addressPort) throws IOException {
      super("LanServerPinger #" + THREAD_ID.incrementAndGet());
      this.motd = motd;
      this.addressPort = addressPort;
      this.setDaemon(true);
      this.setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER));
      this.socket = new DatagramSocket();
   }

   @Override
   public void run() {
      String string = createAnnouncement(this.motd, this.addressPort);
      byte[] bs = string.getBytes(StandardCharsets.UTF_8);

      while (!this.isInterrupted() && this.running) {
         try {
            InetAddress inetAddress = InetAddress.getByName("224.0.2.60");
            DatagramPacket datagramPacket = new DatagramPacket(bs, bs.length, inetAddress, 4445);
            this.socket.send(datagramPacket);
         } catch (IOException var6) {
            LOGGER.warn("LanServerPinger: {}", var6.getMessage());
            break;
         }

         try {
            sleep(1500L);
         } catch (InterruptedException var5) {
         }
      }
   }

   @Override
   public void interrupt() {
      super.interrupt();
      this.running = false;
   }

   public static String createAnnouncement(String motd, String addressPort) {
      return "[MOTD]" + motd + "[/MOTD][AD]" + addressPort + "[/AD]";
   }

   public static String parseAnnouncementMotd(String announcement) {
      int i = announcement.indexOf("[MOTD]");
      if (i < 0) {
         return "missing no";
      } else {
         int j = announcement.indexOf("[/MOTD]", i + "[MOTD]".length());
         return j < i ? "missing no" : announcement.substring(i + "[MOTD]".length(), j);
      }
   }

   public static @Nullable String parseAnnouncementAddressPort(String announcement) {
      int i = announcement.indexOf("[/MOTD]");
      if (i < 0) {
         return null;
      } else {
         int j = announcement.indexOf("[/MOTD]", i + "[/MOTD]".length());
         if (j >= 0) {
            return null;
         } else {
            int k = announcement.indexOf("[AD]", i + "[/MOTD]".length());
            if (k < 0) {
               return null;
            } else {
               int l = announcement.indexOf("[/AD]", k + "[AD]".length());
               return l < k ? null : announcement.substring(k + "[AD]".length(), l);
            }
         }
      }
   }
}
