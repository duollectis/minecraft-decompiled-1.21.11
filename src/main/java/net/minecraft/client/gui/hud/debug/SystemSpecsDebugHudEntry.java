package net.minecraft.client.gui.hud.debug;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Запись отладочного HUD: характеристики системы (Java, CPU, GPU, разрешение).
 */
@Environment(EnvType.CLIENT)
public class SystemSpecsDebugHudEntry implements DebugHudEntry {

	private static final Identifier SECTION_ID = Identifier.ofVanilla("system");

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		GpuDevice gpuDevice = RenderSystem.getDevice();
		MinecraftClient client = MinecraftClient.getInstance();

		lines.addLinesToSection(
			SECTION_ID,
			List.of(
				String.format(Locale.ROOT, "Java: %s", System.getProperty("java.version")),
				String.format(Locale.ROOT, "CPU: %s", GLX._getCpuInfo()),
				String.format(
					Locale.ROOT,
					"Display: %dx%d (%s)",
					client.getWindow().getFramebufferWidth(),
					client.getWindow().getFramebufferHeight(),
					gpuDevice.getVendor()
				),
				gpuDevice.getRenderer(),
				String.format(Locale.ROOT, "%s %s", gpuDevice.getBackendName(), gpuDevice.getVersion())
			)
		);
	}

	@Override
	public boolean canShow(boolean reducedDebugInfo) {
		return true;
	}
}
