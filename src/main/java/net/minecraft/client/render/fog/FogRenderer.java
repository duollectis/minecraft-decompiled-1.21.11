package net.minecraft.client.render.fog;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Рендерер тумана: вычисляет параметры тумана и записывает их в GPU-буфер (UBO).
 * <p>
 * Поддерживает два типа буферов: пустой (туман отключён или тип {@code NONE})
 * и рабочий кольцевой буфер для тумана мира. Цепочка {@link FogModifier}
 * определяет цвет и дальность тумана в зависимости от типа погружения камеры
 * и эффектов на сущности.
 */
@Environment(EnvType.CLIENT)
public class FogRenderer implements AutoCloseable {

	public static final int FOG_UBO_SIZE = new Std140SizeCalculator()
			.putVec4()
			.putFloat()
			.putFloat()
			.putFloat()
			.putFloat()
			.putFloat()
			.putFloat()
			.get();

	private static final List<FogModifier> FOG_MODIFIERS = Lists.newArrayList(
			new FogModifier[]{
					new LavaFogModifier(),
					new PowderSnowFogModifier(),
					new BlindnessEffectFogModifier(),
					new DarknessEffectFogModifier(),
					new WaterFogModifier(),
					new AtmosphericFogModifier()
			}
	);

	private static boolean fogEnabled = true;

	private final GpuBuffer emptyBuffer;
	private final MappableRingBuffer fogBuffer;

	public FogRenderer() {
		GpuDevice gpuDevice = RenderSystem.getDevice();
		fogBuffer = new MappableRingBuffer(() -> "Fog UBO", 130, FOG_UBO_SIZE);

		MemoryStack memoryStack = MemoryStack.stackPush();

		try {
			ByteBuffer byteBuffer = memoryStack.malloc(FOG_UBO_SIZE);
			applyFog(
					byteBuffer,
					0,
					new Vector4f(0.0F),
					Float.MAX_VALUE,
					Float.MAX_VALUE,
					Float.MAX_VALUE,
					Float.MAX_VALUE,
					Float.MAX_VALUE,
					Float.MAX_VALUE
			);
			emptyBuffer = gpuDevice.createBuffer(() -> "Empty fog", 128, byteBuffer.flip());
		}
		catch (Throwable throwable) {
			if (memoryStack != null) {
				try {
					memoryStack.close();
				}
				catch (Throwable suppressed) {
					throwable.addSuppressed(suppressed);
				}
			}

			throw throwable;
		}

		if (memoryStack != null) {
			memoryStack.close();
		}

		RenderSystem.setShaderFog(getFogBuffer(FogRenderer.FogType.NONE));
	}

	@Override
	public void close() {
		emptyBuffer.close();
		fogBuffer.close();
	}

	/** Переключает кольцевой буфер на следующий слот (вызывается в начале кадра). */
	public void rotate() {
		fogBuffer.rotate();
	}

	/**
	 * Возвращает срез GPU-буфера для заданного типа тумана.
	 * Если туман глобально отключён, всегда возвращает пустой буфер.
	 */
	public GpuBufferSlice getFogBuffer(FogRenderer.FogType fogType) {
		if (fogEnabled) {
			return switch (fogType) {
				case NONE -> emptyBuffer.slice(0L, FOG_UBO_SIZE);
				case WORLD -> fogBuffer.getBlocking().slice(0L, FOG_UBO_SIZE);
			};
		}

		return emptyBuffer.slice(0L, FOG_UBO_SIZE);
	}

	/**
	 * Вычисляет и записывает параметры тумана в кольцевой буфер.
	 * Возвращает итоговый цвет тумана для использования при очистке фона.
	 *
	 * @param camera            камера (определяет тип погружения и сущность)
	 * @param viewDistance      дальность прорисовки в чанках
	 * @param renderTickCounter счётчик тиков рендера (для интерполяции)
	 * @param skyDarkness       затемнение неба [0..1]
	 * @param clientWorld       мир клиента
	 * @return цвет тумана в формате RGBA
	 */
	public Vector4f applyFog(
			Camera camera,
			int viewDistance,
			RenderTickCounter renderTickCounter,
			float skyDarkness,
			ClientWorld clientWorld
	) {
		float tickProgress = renderTickCounter.getTickProgress(false);
		Vector4f fogColor = getFogColor(camera, tickProgress, clientWorld, viewDistance, skyDarkness);
		float renderDistanceBlocks = viewDistance * 16;
		CameraSubmersionType submersionType = getCameraSubmersionType(camera);
		Entity entity = camera.getFocusedEntity();
		FogData fogData = new FogData();

		for (FogModifier modifier : FOG_MODIFIERS) {
			if (modifier.shouldApply(submersionType, entity)) {
				modifier.applyStartEndModifier(fogData, camera, clientWorld, renderDistanceBlocks, renderTickCounter);
				break;
			}
		}

		float fogTransitionWidth = MathHelper.clamp(renderDistanceBlocks / 10.0F, 4.0F, 64.0F);
		fogData.renderDistanceStart = renderDistanceBlocks - fogTransitionWidth;
		fogData.renderDistanceEnd = renderDistanceBlocks;

		try (GpuBuffer.MappedView mappedView = RenderSystem
				.getDevice()
				.createCommandEncoder()
				.mapBuffer(fogBuffer.getBlocking(), false, true)
		) {
			applyFog(
					mappedView.data(),
					0,
					fogColor,
					fogData.environmentalStart,
					fogData.environmentalEnd,
					fogData.renderDistanceStart,
					fogData.renderDistanceEnd,
					fogData.skyEnd,
					fogData.cloudEnd
			);
		}

		return fogColor;
	}

	/** Переключает глобальное состояние тумана и возвращает новое значение. */
	public static boolean toggleFog() {
		return fogEnabled = !fogEnabled;
	}

	private Vector4f getFogColor(
			Camera camera,
			float tickProgress,
			ClientWorld world,
			int viewDistance,
			float skyDarkness
	) {
		CameraSubmersionType submersionType = getCameraSubmersionType(camera);
		Entity entity = camera.getFocusedEntity();
		FogModifier colorSource = null;
		FogModifier darknessModifier = null;

		for (FogModifier modifier : FOG_MODIFIERS) {
			if (modifier.shouldApply(submersionType, entity)) {
				if (colorSource == null && modifier.isColorSource()) {
					colorSource = modifier;
				}

				if (darknessModifier == null && modifier.isDarknessModifier()) {
					darknessModifier = modifier;
				}
			}
		}

		if (colorSource == null) {
			throw new IllegalStateException("No color source environment found");
		}

		int rawColor = colorSource.getFogColor(world, camera, viewDistance, tickProgress);
		float voidDarknessRange = world.getLevelProperties().getVoidDarknessRange();
		float voidDarkness = MathHelper.clamp(
				(voidDarknessRange + world.getBottomY() - (float) camera.getCameraPos().y) / voidDarknessRange,
				0.0F,
				1.0F
		);

		if (darknessModifier != null) {
			voidDarkness = darknessModifier.applyDarknessModifier((LivingEntity) entity, voidDarkness, tickProgress);
		}

		float red = ColorHelper.getRedFloat(rawColor);
		float green = ColorHelper.getGreenFloat(rawColor);
		float blue = ColorHelper.getBlueFloat(rawColor);

		if (voidDarkness > 0.0F
				&& submersionType != CameraSubmersionType.LAVA
				&& submersionType != CameraSubmersionType.POWDER_SNOW) {
			float darknessSquared = MathHelper.square(1.0F - voidDarkness);
			red *= darknessSquared;
			green *= darknessSquared;
			blue *= darknessSquared;
		}

		if (skyDarkness > 0.0F) {
			red = MathHelper.lerp(skyDarkness, red, red * 0.7F);
			green = MathHelper.lerp(skyDarkness, green, green * 0.6F);
			blue = MathHelper.lerp(skyDarkness, blue, blue * 0.6F);
		}

		float nightVisionStrength;
		if (submersionType == CameraSubmersionType.WATER) {
			nightVisionStrength = entity instanceof ClientPlayerEntity player
					? player.getUnderwaterVisibility()
					: 1.0F;
		}
		else if (entity instanceof LivingEntity living
				&& living.hasStatusEffect(StatusEffects.NIGHT_VISION)
				&& !living.hasStatusEffect(StatusEffects.DARKNESS)) {
			nightVisionStrength = GameRenderer.getNightVisionStrength(living, tickProgress);
		}
		else {
			nightVisionStrength = 0.0F;
		}

		if (red != 0.0F && green != 0.0F && blue != 0.0F) {
			float maxChannel = 1.0F / Math.max(red, Math.max(green, blue));
			red = MathHelper.lerp(nightVisionStrength, red, red * maxChannel);
			green = MathHelper.lerp(nightVisionStrength, green, green * maxChannel);
			blue = MathHelper.lerp(nightVisionStrength, blue, blue * maxChannel);
		}

		return new Vector4f(red, green, blue, 1.0F);
	}

	/** Возвращает тип погружения камеры, заменяя {@code NONE} на {@code ATMOSPHERIC}. */
	private CameraSubmersionType getCameraSubmersionType(Camera camera) {
		CameraSubmersionType type = camera.getSubmersionType();
		return type == CameraSubmersionType.NONE ? CameraSubmersionType.ATMOSPHERIC : type;
	}

	private void applyFog(
			ByteBuffer buffer,
			int bufPos,
			Vector4f fogColor,
			float environmentalStart,
			float environmentalEnd,
			float renderDistanceStart,
			float renderDistanceEnd,
			float skyEnd,
			float cloudEnd
	) {
		buffer.position(bufPos);
		Std140Builder.intoBuffer(buffer)
		             .putVec4(fogColor)
		             .putFloat(environmentalStart)
		             .putFloat(environmentalEnd)
		             .putFloat(renderDistanceStart)
		             .putFloat(renderDistanceEnd)
		             .putFloat(skyEnd)
		             .putFloat(cloudEnd);
	}

	/** Тип запрашиваемого буфера тумана. */
	@Environment(EnvType.CLIENT)
	public enum FogType {
		NONE,
		WORLD;
	}
}
