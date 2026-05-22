package net.minecraft.client.gl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

/**
 * Глобальный UBO с параметрами рендеринга, общими для всех шейдеров в кадре:
 * позиция камеры, размер экрана, время, сила блеска и размытие меню.
 * Обновляется один раз за кадр через {@link #set}.
 */
@Environment(EnvType.CLIENT)
public class GlobalSettings implements AutoCloseable {

	public static final int SIZE =
		new Std140SizeCalculator().putIVec3().putVec3().putVec2().putFloat().putFloat().putInt().putInt().get();

	// usage = 136: USAGE_COPY_DST | USAGE_UNIFORM
	private final GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Global Settings UBO", 136, SIZE);

	/**
	 * Обновляет содержимое UBO данными текущего кадра.
	 * Позиция камеры разбивается на целую часть (ivec3) и дробную (vec3) для точности с плавающей запятой.
	 *
	 * @param renderDebugLabels флаг включения отладочных меток рендеринга
	 */
	public void set(
		int width,
		int height,
		double glintStrength,
		long time,
		RenderTickCounter tickCounter,
		int menuBackgroundBlurriness,
		Camera camera,
		boolean renderDebugLabels
	) {
		Vec3d cameraPos = camera.getCameraPos();
		int camX = MathHelper.floor(cameraPos.x);
		int camY = MathHelper.floor(cameraPos.y);
		int camZ = MathHelper.floor(cameraPos.z);

		// Время нормализуется в диапазон [0, 1] относительно суточного цикла (24000 тиков)
		float normalizedTime = ((float) (time % 24000L) + tickCounter.getTickProgress(false)) / 24000.0F;

		try (MemoryStack stack = MemoryStack.stackPush()) {
			ByteBuffer data = Std140Builder.onStack(stack, SIZE)
				.putIVec3(camX, camY, camZ)
				.putVec3(
					(float) (camX - cameraPos.x),
					(float) (camY - cameraPos.y),
					(float) (camZ - cameraPos.z)
				)
				.putVec2(width, height)
				.putFloat((float) glintStrength)
				.putFloat(normalizedTime)
				.putInt(menuBackgroundBlurriness)
				.putInt(renderDebugLabels ? 1 : 0)
				.get();

			RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), data);
		}

		RenderSystem.setGlobalSettingsUniform(buffer);
	}

	@Override
	public void close() {
		buffer.close();
	}
}
