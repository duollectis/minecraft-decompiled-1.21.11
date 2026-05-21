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

@Environment(EnvType.CLIENT)
/**
 * {@code GpuDeviceInfo}.
 */
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
	private static final List<String>
			ATOM_DEVICES =
			List.of("x6211e", "x6212re", "x6214re", "x6413e", "x6414re", "x6416re", "x6425e", "x6425re", "x6427fe");
	private static final List<String>
			CELERON_DEVICES =
			List.of("j6412", "j6413", "n4500", "n4505", "n5095", "n5095a", "n5100", "n5105", "n6210", "n6211");
	private static final List<String> PENTIUM_DEVICES = List.of("6805", "j6426", "n6415", "n6000", "n6005");
	private static @Nullable GpuDeviceInfo instance;
	private final WeakReference<GpuDevice> device;
	private final boolean requiresRecreateOnUploadToBuffer;
	private final boolean shouldDisableArbDirectAccess;
	private final boolean isAmdGpu;

	private GpuDeviceInfo(GpuDevice device) {
		this.device = new WeakReference<>(device);
		this.requiresRecreateOnUploadToBuffer = requiresRecreateOnUploadToBuffer(device);
		this.shouldDisableArbDirectAccess = shouldDisableArbDirectAccess(device);
		this.isAmdGpu = detectAmdGpu(device);
	}

	public static GpuDeviceInfo get(GpuDevice device) {
		GpuDeviceInfo gpuDeviceInfo = instance;
		if (gpuDeviceInfo == null || gpuDeviceInfo.device.get() != device) {
			instance = gpuDeviceInfo = new GpuDeviceInfo(device);
		}

		return gpuDeviceInfo;
	}

	public boolean requiresRecreateOnUploadToBuffer() {
		return this.requiresRecreateOnUploadToBuffer;
	}

	public boolean shouldDisableArbDirectAccess() {
		return this.shouldDisableArbDirectAccess;
	}

	public boolean isAmdGpu() {
		return this.isAmdGpu;
	}

	private static boolean requiresRecreateOnUploadToBuffer(GpuDevice device) {
		String string = GLX._getCpuInfo().toLowerCase(Locale.ROOT);
		String string2 = device.getRenderer().toLowerCase(Locale.ROOT);
		if (!string.contains("intel") || !string2.contains("intel") || string2.contains("mesa")) {
			return false;
		}
		else if (string2.endsWith("gen11")) {
			return true;
		}
		else {
			return !string2.contains("uhd graphics") && !string2.contains("iris")
			       ? false
			       : string.contains("atom") && ATOM_DEVICES.stream().anyMatch(string::contains)
			         || string.contains("celeron") && CELERON_DEVICES.stream().anyMatch(string::contains)
			         || string.contains("pentium") && PENTIUM_DEVICES.stream().anyMatch(string::contains)
			         || OTHER_INTEL_DEVICES.stream().anyMatch(string::contains);
		}
	}

	private static boolean shouldDisableArbDirectAccess(GpuDevice device) {
		boolean bl = Util.getOperatingSystem() == Util.OperatingSystem.WINDOWS && Util.isOnAarch64();
		return bl || device.getRenderer().startsWith("D3D12");
	}

	private static boolean detectAmdGpu(GpuDevice gpuDevice) {
		return gpuDevice.getRenderer().contains("AMD");
	}
}
