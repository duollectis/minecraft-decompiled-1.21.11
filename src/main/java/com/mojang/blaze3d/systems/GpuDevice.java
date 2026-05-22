package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.ShaderSourceGetter;
import net.minecraft.util.annotation.DeobfuscateClass;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Supplier;

/**
 * Абстракция GPU-устройства. Предоставляет фабричные методы для создания
 * всех GPU-ресурсов: буферов, текстур, сэмплеров и рендер-конвейеров.
 *
 * <p>Является центральной точкой входа для работы с графическим API.
 * Получить экземпляр можно через {@link RenderSystem#getDevice()}.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public interface GpuDevice {

	/** Создаёт новый кодировщик GPU-команд для записи операций рендеринга. */
	CommandEncoder createCommandEncoder();

	/**
	 * Создаёт сэмплер текстуры с заданными параметрами фильтрации и адресации.
	 *
	 * @param addressModeU   режим адресации по горизонтали
	 * @param addressModeV   режим адресации по вертикали
	 * @param minFilterMode  фильтрация при уменьшении текстуры
	 * @param magFilterMode  фильтрация при увеличении текстуры
	 * @param maxAnisotropy  максимальная степень анизотропной фильтрации (1 = отключена)
	 * @param maxLevelOfDetail максимальный уровень детализации мипмапов
	 */
	GpuSampler createSampler(
		AddressMode addressModeU,
		AddressMode addressModeV,
		FilterMode minFilterMode,
		FilterMode magFilterMode,
		int maxAnisotropy,
		OptionalDouble maxLevelOfDetail
	);

	/**
	 * Создаёт GPU-текстуру с заданными параметрами.
	 *
	 * @param labelGetter    поставщик отладочной метки (может быть {@code null})
	 * @param usage          битовая маска флагов использования {@link GpuTexture.Usage}
	 * @param format         формат пикселей
	 * @param width          ширина в пикселях
	 * @param height         высота в пикселях
	 * @param depthOrLayers  глубина (для 3D-текстур) или количество слоёв (для массивов)
	 * @param mipLevels      количество уровней мипмапов
	 */
	GpuTexture createTexture(
		@Nullable Supplier<String> labelGetter,
		@GpuTexture.Usage int usage,
		TextureFormat format,
		int width,
		int height,
		int depthOrLayers,
		int mipLevels
	);

	GpuTexture createTexture(
		@Nullable String label,
		@GpuTexture.Usage int usage,
		TextureFormat format,
		int width,
		int height,
		int depthOrLayers,
		int mipLevels
	);

	/** Создаёт view, охватывающий все мипмапы текстуры. */
	GpuTextureView createTextureView(GpuTexture texture);

	/**
	 * Создаёт view для заданного диапазона мипмапов текстуры.
	 *
	 * @param texture      исходная текстура
	 * @param baseMipLevel начальный уровень мипмапа
	 * @param mipLevels    количество уровней мипмапов в view
	 */
	GpuTextureView createTextureView(GpuTexture texture, int baseMipLevel, int mipLevels);

	/**
	 * Создаёт GPU-буфер заданного размера без начальных данных.
	 *
	 * @param labelGetter поставщик отладочной метки (может быть {@code null})
	 * @param usage       битовая маска флагов использования {@link GpuBuffer.Usage}
	 * @param size        размер буфера в байтах
	 */
	GpuBuffer createBuffer(@Nullable Supplier<String> labelGetter, @GpuBuffer.Usage int usage, long size);

	/**
	 * Создаёт GPU-буфер и немедленно заполняет его данными из CPU-буфера.
	 *
	 * @param labelGetter поставщик отладочной метки (может быть {@code null})
	 * @param usage       битовая маска флагов использования {@link GpuBuffer.Usage}
	 * @param data        начальные данные буфера
	 */
	GpuBuffer createBuffer(@Nullable Supplier<String> labelGetter, @GpuBuffer.Usage int usage, ByteBuffer data);

	String getImplementationInformation();

	List<String> getLastDebugMessages();

	boolean isDebuggingEnabled();

	String getVendor();

	String getBackendName();

	String getVersion();

	String getRenderer();

	int getMaxTextureSize();

	int getUniformOffsetAlignment();

	/**
	 * Предварительно компилирует рендер-конвейер с использованием стандартного источника шейдеров.
	 * Эквивалентно вызову {@link #precompilePipeline(RenderPipeline, ShaderSourceGetter)} с {@code null}.
	 */
	default CompiledRenderPipeline precompilePipeline(RenderPipeline pipeline) {
		return precompilePipeline(pipeline, null);
	}

	/**
	 * Предварительно компилирует рендер-конвейер, компилируя шейдеры.
	 *
	 * @param pipeline      описание конвейера для компиляции
	 * @param sourceGetter  источник шейдерного кода, или {@code null} для стандартного
	 * @return скомпилированный конвейер; может быть невалидным при ошибке компиляции
	 */
	CompiledRenderPipeline precompilePipeline(RenderPipeline pipeline, @Nullable ShaderSourceGetter sourceGetter);

	void clearPipelineCache();

	List<String> getEnabledExtensions();

	int getMaxSupportedAnisotropy();

	void close();
}
