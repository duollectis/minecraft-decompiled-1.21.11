package net.minecraft.client.gl;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.OptionalDouble;

/**
 * Кэш всех возможных комбинаций параметров сэмплера.
 * Индекс вычисляется через {@link #toIndex} как битовая маска из ordinal-значений перечислений.
 * Размер массива — 32 (2 AddressMode × 2 AddressMode × 2 FilterMode × 2 FilterMode × 2 LOD = 32).
 */
@Environment(EnvType.CLIENT)
public class SamplerCache {

	private final GpuSampler[] samplers = new GpuSampler[32];

	/**
	 * Инициализирует все 32 комбинации сэмплеров.
	 * Требует ровно 2 значения в {@link AddressMode} и {@link FilterMode}.
	 */
	public void init() {
		if (AddressMode.values().length != 2 || FilterMode.values().length != 2) {
			throw new IllegalStateException(
				"AddressMode and FilterMode enum sizes must be 2 - if you expanded them, please update SamplerCache"
			);
		}

		GpuDevice gpuDevice = RenderSystem.getDevice();

		for (AddressMode addressModeU : AddressMode.values()) {
			for (AddressMode addressModeV : AddressMode.values()) {
				for (FilterMode minFilter : FilterMode.values()) {
					for (FilterMode magFilter : FilterMode.values()) {
						for (boolean defaultLod : new boolean[]{true, false}) {
							samplers[toIndex(addressModeU, addressModeV, minFilter, magFilter, defaultLod)] =
								gpuDevice.createSampler(
									addressModeU,
									addressModeV,
									minFilter,
									magFilter,
									1,
									defaultLod ? OptionalDouble.empty() : OptionalDouble.of(0.0)
								);
						}
					}
				}
			}
		}
	}

	public GpuSampler get(
		AddressMode addressModeU,
		AddressMode addressModeV,
		FilterMode minFilterMode,
		FilterMode magFilterMode,
		boolean defaultLineOfDetail
	) {
		return samplers[toIndex(addressModeU, addressModeV, minFilterMode, magFilterMode, defaultLineOfDetail)];
	}

	public GpuSampler get(FilterMode filterMode) {
		return get(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, filterMode, filterMode, false);
	}

	public GpuSampler get(FilterMode filterMode, boolean defaultLineOfDetail) {
		return get(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, filterMode, filterMode, defaultLineOfDetail);
	}

	public GpuSampler getRepeated(FilterMode filterMode) {
		return get(AddressMode.REPEAT, AddressMode.REPEAT, filterMode, filterMode, false);
	}

	public GpuSampler getRepeated(FilterMode filterMode, boolean defaultLineOfDetail) {
		return get(AddressMode.REPEAT, AddressMode.REPEAT, filterMode, filterMode, defaultLineOfDetail);
	}

	public void close() {
		for (GpuSampler sampler : samplers) {
			sampler.close();
		}
	}

	/**
	 * Вычисляет индекс в массиве сэмплеров через битовую упаковку ordinal-значений.
	 * Бит 0 — addressModeU, бит 1 — addressModeV, бит 2 — minFilter, бит 3 — magFilter, бит 4 — defaultLod.
	 */
	@VisibleForTesting
	static int toIndex(
		AddressMode addressModeU,
		AddressMode addressModeV,
		FilterMode minFilterMode,
		FilterMode magFilterMode,
		boolean defaultLineOfDetail
	) {
		int index = 0;
		index |= addressModeU.ordinal() & 1;
		index |= (addressModeV.ordinal() & 1) << 1;
		index |= (minFilterMode.ordinal() & 1) << 2;
		index |= (magFilterMode.ordinal() & 1) << 3;

		if (defaultLineOfDetail) {
			index |= 16;
		}

		return index;
	}
}
