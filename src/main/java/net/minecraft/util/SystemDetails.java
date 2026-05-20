package net.minecraft.util;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.lang.management.ManagementFactory;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;

public class SystemDetails {
   public static final long MEBI = 1048576L;
   private static final long GIGA = 1000000000L;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String OPERATING_SYSTEM = System.getProperty("os.name")
      + " ("
      + System.getProperty("os.arch")
      + ") version "
      + System.getProperty("os.version");
   private static final String JAVA_VERSION = System.getProperty("java.version") + ", " + System.getProperty("java.vendor");
   private static final String JVM_VERSION = System.getProperty("java.vm.name")
      + " ("
      + System.getProperty("java.vm.info")
      + "), "
      + System.getProperty("java.vm.vendor");
   private final Map<String, String> sections = Maps.newLinkedHashMap();

   public SystemDetails() {
      this.addSection("Minecraft Version", SharedConstants.getGameVersion().name());
      this.addSection("Minecraft Version ID", SharedConstants.getGameVersion().id());
      this.addSection("Operating System", OPERATING_SYSTEM);
      this.addSection("Java Version", JAVA_VERSION);
      this.addSection("Java VM Version", JVM_VERSION);
      this.addSection("Memory", () -> {
         Runtime runtime = Runtime.getRuntime();
         long l = runtime.maxMemory();
         long m = runtime.totalMemory();
         long n = runtime.freeMemory();
         long o = l / 1048576L;
         long p = m / 1048576L;
         long q = n / 1048576L;
         return n + " bytes (" + q + " MiB) / " + m + " bytes (" + p + " MiB) up to " + l + " bytes (" + o + " MiB)";
      });
      this.addSection("CPUs", () -> String.valueOf(Runtime.getRuntime().availableProcessors()));
      this.tryAddGroup("hardware", () -> this.addHardwareGroup(new SystemInfo()));
      this.addSection("JVM Flags", () -> collectJvmArguments(flag -> flag.startsWith("-X")));
      this.addSection("Debug Flags", () -> collectJvmArguments(flag -> flag.startsWith("-DMC_DEBUG_")));
   }

   private static String collectJvmArguments(Predicate<String> predicate) {
      List<String> list = ManagementFactory.getRuntimeMXBean().getInputArguments();
      List<String> list2 = list.stream().filter(predicate).toList();
      return String.format(Locale.ROOT, "%d total; %s", list2.size(), String.join(" ", list2));
   }

   public void addSection(String name, String value) {
      this.sections.put(name, value);
   }

   public void addSection(String name, Supplier<String> valueSupplier) {
      try {
         this.addSection(name, valueSupplier.get());
      } catch (Exception var4) {
         LOGGER.warn("Failed to get system info for {}", name, var4);
         this.addSection(name, "ERR");
      }
   }

   private void addHardwareGroup(SystemInfo systemInfo) {
      HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();
      this.tryAddGroup("processor", () -> this.addProcessorGroup(hardwareAbstractionLayer.getProcessor()));
      this.tryAddGroup("graphics", () -> this.addGraphicsCardGroup(hardwareAbstractionLayer.getGraphicsCards()));
      this.tryAddGroup("memory", () -> this.addGlobalMemoryGroup(hardwareAbstractionLayer.getMemory()));
      this.tryAddGroup("storage", this::addStorageGroup);
   }

   private void tryAddGroup(String name, Runnable adder) {
      try {
         adder.run();
      } catch (Throwable var4) {
         LOGGER.warn("Failed retrieving info for group {}", name, var4);
      }
   }

   public static float toMebibytes(long bytes) {
      return (float)bytes / 1048576.0F;
   }

   private void addPhysicalMemoryGroup(List<PhysicalMemory> memories) {
      int i = 0;

      for (PhysicalMemory physicalMemory : memories) {
         String string = String.format(Locale.ROOT, "Memory slot #%d ", i++);
         this.addSection(string + "capacity (MiB)", () -> String.format(Locale.ROOT, "%.2f", toMebibytes(physicalMemory.getCapacity())));
         this.addSection(string + "clockSpeed (GHz)", () -> String.format(Locale.ROOT, "%.2f", (float)physicalMemory.getClockSpeed() / 1.0E9F));
         this.addSection(string + "type", physicalMemory::getMemoryType);
      }
   }

   private void addVirtualMemoryGroup(VirtualMemory virtualMemory) {
      this.addSection("Virtual memory max (MiB)", () -> String.format(Locale.ROOT, "%.2f", toMebibytes(virtualMemory.getVirtualMax())));
      this.addSection("Virtual memory used (MiB)", () -> String.format(Locale.ROOT, "%.2f", toMebibytes(virtualMemory.getVirtualInUse())));
      this.addSection("Swap memory total (MiB)", () -> String.format(Locale.ROOT, "%.2f", toMebibytes(virtualMemory.getSwapTotal())));
      this.addSection("Swap memory used (MiB)", () -> String.format(Locale.ROOT, "%.2f", toMebibytes(virtualMemory.getSwapUsed())));
   }

   private void addGlobalMemoryGroup(GlobalMemory globalMemory) {
      this.tryAddGroup("physical memory", () -> this.addPhysicalMemoryGroup(globalMemory.getPhysicalMemory()));
      this.tryAddGroup("virtual memory", () -> this.addVirtualMemoryGroup(globalMemory.getVirtualMemory()));
   }

   private void addGraphicsCardGroup(List<GraphicsCard> graphicsCards) {
      int i = 0;

      for (GraphicsCard graphicsCard : graphicsCards) {
         String string = String.format(Locale.ROOT, "Graphics card #%d ", i++);
         this.addSection(string + "name", graphicsCard::getName);
         this.addSection(string + "vendor", graphicsCard::getVendor);
         this.addSection(string + "VRAM (MiB)", () -> String.format(Locale.ROOT, "%.2f", toMebibytes(graphicsCard.getVRam())));
         this.addSection(string + "deviceId", graphicsCard::getDeviceId);
         this.addSection(string + "versionInfo", graphicsCard::getVersionInfo);
      }
   }

   private void addProcessorGroup(CentralProcessor centralProcessor) {
      ProcessorIdentifier processorIdentifier = centralProcessor.getProcessorIdentifier();
      this.addSection("Processor Vendor", processorIdentifier::getVendor);
      this.addSection("Processor Name", processorIdentifier::getName);
      this.addSection("Identifier", processorIdentifier::getIdentifier);
      this.addSection("Microarchitecture", processorIdentifier::getMicroarchitecture);
      this.addSection("Frequency (GHz)", () -> String.format(Locale.ROOT, "%.2f", (float)processorIdentifier.getVendorFreq() / 1.0E9F));
      this.addSection("Number of physical packages", () -> String.valueOf(centralProcessor.getPhysicalPackageCount()));
      this.addSection("Number of physical CPUs", () -> String.valueOf(centralProcessor.getPhysicalProcessorCount()));
      this.addSection("Number of logical CPUs", () -> String.valueOf(centralProcessor.getLogicalProcessorCount()));
   }

   private void addStorageGroup() {
      this.addStorageSection("jna.tmpdir");
      this.addStorageSection("org.lwjgl.system.SharedLibraryExtractPath");
      this.addStorageSection("io.netty.native.workdir");
      this.addStorageSection("java.io.tmpdir");
      this.addStorageSection("workdir", () -> "");
   }

   private void addStorageSection(String property) {
      this.addStorageSection(property, () -> System.getProperty(property));
   }

   private void addStorageSection(String name, Supplier<@Nullable String> pathSupplier) {
      String string = "Space in storage for " + name + " (MiB)";

      try {
         String string2 = pathSupplier.get();
         if (string2 == null) {
            this.addSection(string, "<path not set>");
            return;
         }

         FileStore fileStore = Files.getFileStore(Path.of(string2));
         this.addSection(
            string, String.format(Locale.ROOT, "available: %.2f, total: %.2f", toMebibytes(fileStore.getUsableSpace()), toMebibytes(fileStore.getTotalSpace()))
         );
      } catch (InvalidPathException var6) {
         LOGGER.warn("{} is not a path", name, var6);
         this.addSection(string, "<invalid path>");
      } catch (Exception var7) {
         LOGGER.warn("Failed retrieving storage space for {}", name, var7);
         this.addSection(string, "ERR");
      }
   }

   public void writeTo(StringBuilder stringBuilder) {
      stringBuilder.append("-- ").append("System Details").append(" --\n");
      stringBuilder.append("Details:");
      this.sections.forEach((name, value) -> {
         stringBuilder.append("\n\t");
         stringBuilder.append(name);
         stringBuilder.append(": ");
         stringBuilder.append(value);
      });
   }

   public String collect() {
      return this.sections.entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.joining(System.lineSeparator()));
   }
}
