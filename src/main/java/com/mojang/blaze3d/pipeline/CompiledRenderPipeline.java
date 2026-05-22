package com.mojang.blaze3d.pipeline;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

/**
 * Скомпилированный рендер-конвейер, готовый к использованию на GPU.
 * Создаётся из {@link RenderPipeline} через {@code GpuDevice.precompilePipeline()}.
 * Может быть невалидным, если компиляция шейдеров завершилась с ошибкой.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public interface CompiledRenderPipeline {

	/** Возвращает {@code true}, если конвейер успешно скомпилирован и готов к использованию. */
	boolean isValid();
}
