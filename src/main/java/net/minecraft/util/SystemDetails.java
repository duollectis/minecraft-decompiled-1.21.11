package net.minecraft.util;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;

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

/**
 * Сборщик системной информации для отчётов об ошибках.
 * Собирает данные об ОС, JVM, процессоре, памяти, видеокарте и дисковом пространстве.
 */
public class SystemDetails {

	/** Количество байт в мебибайте (2^20). */
	public static final long MEBI = 1048576L;

	/** Количество байт в гигабайте (10^9). */
	private static final long GIGA = 1000000000L;

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final String OPERATING_SYSTEM = System.getProperty("os.name")
		+ " (" + System.getProperty("os.arch") + ") version " + System.getProperty("os.version");

	private static final String JAVA_VERSION = System.getProperty("java.version")
		+ ", " + System.getProperty("java.vendor");

	private static final String JVM_VERSION = System.getProperty("java.vm.name")
		+ " (" + System.getProperty("java.vm.info") + "), " + System.getProperty("java.vm.vendor");

	private final Map<String, String> sections = Maps.newLinkedHashMap();

	public SystemDetails() {
		addSection("Minecraft Version", SharedConstants.getGameVersion().name());
		addSection("Minecraft Version ID", SharedConstants.getGameVersion().id());
		addSection("Operating System", OPERATING_SYSTEM);
		addSection("Java Version", JAVA_VERSION);
		addSection("Java VM Version", JVM_VERSION);
		addSection("Memory", () -> {
			Runtime runtime = Runtime.getRuntime();
			long maxMemory = runtime.maxMemory();
			long totalMemory = runtime.totalMemory();
			long freeMemory = runtime.freeMemory();
			long maxMiB = maxMemory / MEBI;
			long totalMiB = totalMemory / MEBI;
			long freeMiB = freeMemory / MEBI;
			return freeMemory + " bytes (" + freeMiB + " MiB) / "
				+ totalMemory + " bytes (" + totalMiB + " MiB) up to "
				+ maxMemory + " bytes (" + maxMiB + " MiB)";
		});
		addSection("CPUs", () -> String.valueOf(Runtime.getRuntime().availableProcessors()));
		tryAddGroup("hardware", () -> addHardwareGroup(new SystemInfo()));
		addSection("JVM Flags", () -> collectJvmArguments(flag -> flag.startsWith("-X")));
		addSection("Debug Flags", () -> collectJvmArguments(flag -> flag.startsWith("-DMC_DEBUG_")));
	}

	private static String collectJvmArguments(Predicate<String> predicate) {
		List<String> allArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
		List<String> filtered = allArgs.stream().filter(predicate).toList();
		return String.format(Locale.ROOT, "%d total; %s", filtered.size(), String.join(" ", filtered));
	}

	public void addSection(String name, String value) {
		sections.put(name, value);
	}

	/**
	 * Добавляет секцию с ленивым вычислением значения.
	 * При ошибке вычисления записывает «ERR» и логирует предупреждение.
	 *
	 * @param name название секции
	 * @param valueSupplier поставщик значения
	 */
	public void addSection(String name, Supplier<String> valueSupplier) {
		try {
			addSection(name, valueSupplier.get());
		} catch (Exception exception) {
			LOGGER.warn("Failed to get system info for {}", name, exception);
			addSection(name, "ERR");
		}
	}

	private void addHardwareGroup(SystemInfo systemInfo) {
		HardwareAbstractionLayer hardware = systemInfo.getHardware();
		tryAddGroup("processor", () -> addProcessorGroup(hardware.getProcessor()));
		tryAddGroup("graphics", () -> addGraphicsCardGroup(hardware.getGraphicsCards()));
		tryAddGroup("memory", () -> addGlobalMemoryGroup(hardware.getMemory()));
		tryAddGroup("storage", this::addStorageGroup);
	}

	private void tryAddGroup(String name, Runnable adder) {
		try {
			adder.run();
		} catch (Throwable throwable) {
			LOGGER.warn("Failed retrieving info for group {}", name, throwable);
		}
	}

	/**
	 * Конвертирует байты в мебибайты.
	 *
	 * @param bytes количество байт
	 * @return значение в мебибайтах
	 */
	public static float toMebibytes(long bytes) {
		return (float) bytes / MEBI;
	}

	private void addPhysicalMemoryGroup(List<PhysicalMemory> memories) {
		int slotIndex = 0;

		for (PhysicalMemory memory : memories) {
			String slotPrefix = String.format(Locale.ROOT, "Memory slot #%d ", slotIndex++);
			addSection(slotPrefix + "capacity (MiB)", () -> String.format(Locale.ROOT, "%.2f", toMebibytes(memory.getCapacity())));
			addSection(slotPrefix + "clockSpeed (GHz)", () -> String.format(Locale.ROOT, "%.2f", (float) memory.getClockSpeed() / 1.0E9F));
			addSection(slotPrefix + "type", memory::getMemoryType);
		}
	}

	private void addVirtualMemoryGroup(VirtualMemory virtualMemory) {
		addSection("Virtual memory max (MiB)", () -> String.format(Locale.ROOT, "%.2f", toMebibytes(virtualMemory.getVirtualMax())));
		addSection("Virtual memory used (MiB)", () -> String.format(Locale.ROOT, "%.2f", toMebibytes(virtualMemory.getVirtualInUse())));
		addSection("Swap memory total (MiB)", () -> String.format(Locale.ROOT, "%.2f", toMebibytes(virtualMemory.getSwapTotal())));
		addSection("Swap memory used (MiB)", () -> String.format(Locale.ROOT, "%.2f", toMebibytes(virtualMemory.getSwapUsed())));
	}

	private void addGlobalMemoryGroup(GlobalMemory globalMemory) {
		tryAddGroup("physical memory", () -> addPhysicalMemoryGroup(globalMemory.getPhysicalMemory()));
		tryAddGroup("virtual memory", () -> addVirtualMemoryGroup(globalMemory.getVirtualMemory()));
	}

	private void addGraphicsCardGroup(List<GraphicsCard> graphicsCards) {
		int cardIndex = 0;

		for (GraphicsCard card : graphicsCards) {
			String cardPrefix = String.format(Locale.ROOT, "Graphics card #%d ", cardIndex++);
			addSection(cardPrefix + "name", card::getName);
			addSection(cardPrefix + "vendor", card::getVendor);
			addSection(cardPrefix + "VRAM (MiB)", () -> String.format(Locale.ROOT, "%.2f", toMebibytes(card.getVRam())));
			addSection(cardPrefix + "deviceId", card::getDeviceId);
			addSection(cardPrefix + "versionInfo", card::getVersionInfo);
		}
	}

	private void addProcessorGroup(CentralProcessor processor) {
		ProcessorIdentifier identifier = processor.getProcessorIdentifier();
		addSection("Processor Vendor", identifier::getVendor);
		addSection("Processor Name", identifier::getName);
		addSection("Identifier", identifier::getIdentifier);
		addSection("Microarchitecture", identifier::getMicroarchitecture);
		addSection("Frequency (GHz)", () -> String.format(Locale.ROOT, "%.2f", (float) identifier.getVendorFreq() / 1.0E9F));
		addSection("Number of physical packages", () -> String.valueOf(processor.getPhysicalPackageCount()));
		addSection("Number of physical CPUs", () -> String.valueOf(processor.getPhysicalProcessorCount()));
		addSection("Number of logical CPUs", () -> String.valueOf(processor.getLogicalProcessorCount()));
	}

	private void addStorageGroup() {
		addStorageSection("jna.tmpdir");
		addStorageSection("org.lwjgl.system.SharedLibraryExtractPath");
		addStorageSection("io.netty.native.workdir");
		addStorageSection("java.io.tmpdir");
		addStorageSection("workdir", () -> "");
	}

	private void addStorageSection(String property) {
		addStorageSection(property, () -> System.getProperty(property));
	}

	private void addStorageSection(String name, Supplier<@Nullable String> pathSupplier) {
		String sectionName = "Space in storage for " + name + " (MiB)";

		try {
			String pathValue = pathSupplier.get();

			if (pathValue == null) {
				addSection(sectionName, "<path not set>");
				return;
			}

			FileStore fileStore = Files.getFileStore(Path.of(pathValue));
			addSection(sectionName, String.format(
				Locale.ROOT,
				"available: %.2f, total: %.2f",
				toMebibytes(fileStore.getUsableSpace()),
				toMebibytes(fileStore.getTotalSpace())
			));
		} catch (InvalidPathException exception) {
			LOGGER.warn("{} is not a path", name, exception);
			addSection(sectionName, "<invalid path>");
		} catch (Exception exception) {
			LOGGER.warn("Failed retrieving storage space for {}", name, exception);
			addSection(sectionName, "ERR");
		}
	}

	/**
	 * Записывает все секции в {@link StringBuilder} в формате отчёта об ошибке.
	 *
	 * @param stringBuilder целевой строковый буфер
	 */
	public void writeTo(StringBuilder stringBuilder) {
		stringBuilder.append("-- ").append("System Details").append(" --\n");
		stringBuilder.append("Details:");
		sections.forEach((name, value) -> {
			stringBuilder.append("\n\t");
			stringBuilder.append(name);
			stringBuilder.append(": ");
			stringBuilder.append(value);
		});
	}

	/**
	 * Собирает все секции в одну строку, разделённую системными переносами строк.
	 *
	 * @return строка со всеми секциями
	 */
	public String collect() {
		return sections.entrySet()
			.stream()
			.map(entry -> entry.getKey() + ": " + entry.getValue())
			.collect(Collectors.joining(System.lineSeparator()));
	}
}
