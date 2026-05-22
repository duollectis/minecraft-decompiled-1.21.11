package net.minecraft.client.gl;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.systems.GpuDevice;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

/**
 * Кэшированная информация о GPU-устройстве с эвристиками для обходов известных
 * проблем конкретных видеокарт (Intel Gen11, AMD, D3D12-бэкенд на ARM Windows).
 */
@Environment(EnvType.CLIENT)
public class GpuDeviceInfo {

	private static final List<String> OTHER_INTEL_DEVICES = List.of(
		"i3-1000g1",
		"i3-1000g4",
		"i3-1000ng4",
		"i3-1005g1",
		"i3-l13g4",
		"i5-1030g4",
		"i5-1030g7",
		"i5-1030ng7",
		"i5-1034g1",
		"i5-1035g1",
		"i5-1035g4",
		"i5-1035g7",
		"i5-1038ng7",
		"i5-l16g7",
		"i7-1060g7",
		"i7-1060ng7",
		"i7-1065g7",
		"i7-1068g7",
		"i7-1068ng7"
	);
	private static final List<String> ATOM_DEVICES = List.of(
		"x6211e", "x6212re", "x6214re", "x6413e", "x6414re", "x6416re", "x6425e", "x6425re", "x6427fe"
	);
	private static final List<String> CELERON_DEVICES = List.of(
		"j6412", "j6413", "n4500", "n4505", "n5095", "n5095a", "n5100", "n5105", "n6210", "n6211"
	);
	private static final List<String> PENTIUM_DEVICES = List.of("6805", "j6426", "n6415", "n6000", "n6005");

	private static @Nullable GpuDeviceInfo instance;

	private final WeakReference<GpuDevice> device;
	private final boolean requiresRecreateOnUploadToBuffer;
	private final boolean shouldDisableArbDirectAccess;
	private final boolean isAmdGpu;

	private GpuDeviceInfo(GpuDevice device) {
		this.device = new WeakReference<>(device);
		requiresRecreateOnUploadToBuffer = requiresRecreateOnUploadToBuffer(device);
		shouldDisableArbDirectAccess = shouldDisableArbDirectAccess(device);
		isAmdGpu = detectAmdGpu(device);
	}

	/**
	 * Возвращает кэшированный экземпляр для данного устройства.
	 * Пересоздаёт кэш при смене устройства (например, при переинициализации GL-контекста).
	 */
	public static GpuDeviceInfo get(GpuDevice device) {
		GpuDeviceInfo cached = instance;

		if (cached == null || cached.device.get() != device) {
			instance = cached = new GpuDeviceInfo(device);
		}

		return cached;
	}

	public boolean requiresRecreateOnUploadToBuffer() {
		return requiresRecreateOnUploadToBuffer;
	}

	public boolean shouldDisableArbDirectAccess() {
		return shouldDisableArbDirectAccess;
	}

	public boolean isAmdGpu() {
		return isAmdGpu;
	}

	/**
	 * Определяет, требует ли данный Intel GPU пересоздания буфера при загрузке данных.
	 * Затрагивает только Gen11 iGPU в конкретных процессорах Atom/Celeron/Pentium/Core.
	 */
	private static boolean requiresRecreateOnUploadToBuffer(GpuDevice device) {
		String cpuInfo = GLX._getCpuInfo().toLowerCase(Locale.ROOT);
		String renderer = device.getRenderer().toLowerCase(Locale.ROOT);

		if (!cpuInfo.contains("intel") || !renderer.contains("intel") || renderer.contains("mesa")) {
			return false;
		}

		if (renderer.endsWith("gen11")) {
			return true;
		}

		if (!renderer.contains("uhd graphics") && !renderer.contains("iris")) {
			return false;
		}

		return cpuInfo.contains("atom") && ATOM_DEVICES.stream().anyMatch(cpuInfo::contains)
			|| cpuInfo.contains("celeron") && CELERON_DEVICES.stream().anyMatch(cpuInfo::contains)
			|| cpuInfo.contains("pentium") && PENTIUM_DEVICES.stream().anyMatch(cpuInfo::contains)
			|| OTHER_INTEL_DEVICES.stream().anyMatch(cpuInfo::contains);
	}

	private static boolean shouldDisableArbDirectAccess(GpuDevice device) {
		boolean isWindowsArm = Util.getOperatingSystem() == Util.OperatingSystem.WINDOWS && Util.isOnAarch64();

		return isWindowsArm || device.getRenderer().startsWith("D3D12");
	}

	private static boolean detectAmdGpu(GpuDevice gpuDevice) {
		return gpuDevice.getRenderer().contains("AMD");
	}
}
