package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.annotation.DeobfuscateClass;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

/**
 * Кодировщик GPU-команд. Позволяет записывать операции рендеринга, копирования
 * и передачи данных для последующего выполнения на видеокарте.
 *
 * <p>Все операции записываются в очередь и выполняются GPU асинхронно.
 * Для синхронизации используйте {@link #createFence()}.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public interface CommandEncoder {

	/**
	 * Создаёт проход рендеринга только с цветовым вложением.
	 *
	 * @param labelGetter     поставщик отладочной метки прохода
	 * @param colorAttachment цветовое вложение (render target)
	 * @param clearColor      цвет очистки в формате ARGB, или пустой если очистка не нужна
	 */
	RenderPass createRenderPass(
		Supplier<String> labelGetter,
		GpuTextureView colorAttachment,
		OptionalInt clearColor
	);

	/**
	 * Создаёт проход рендеринга с цветовым и опциональным вложением глубины.
	 *
	 * @param labelGetter      поставщик отладочной метки прохода
	 * @param colorAttachment  цветовое вложение (render target)
	 * @param clearColor       цвет очистки в формате ARGB, или пустой если очистка не нужна
	 * @param depthAttachment  вложение глубины, или {@code null} если не используется
	 * @param clearDepth       значение очистки буфера глубины [0.0, 1.0], или пустой если не нужна
	 */
	RenderPass createRenderPass(
		Supplier<String> labelGetter,
		GpuTextureView colorAttachment,
		OptionalInt clearColor,
		@Nullable GpuTextureView depthAttachment,
		OptionalDouble clearDepth
	);

	void clearColorTexture(GpuTexture texture, int color);

	void clearColorAndDepthTextures(GpuTexture colorAttachment, int color, GpuTexture depthAttachment, double depth);

	void clearColorAndDepthTextures(
		GpuTexture colorAttachment,
		int color,
		GpuTexture depthAttachment,
		double depth,
		int scissorX,
		int scissorY,
		int scissorWidth,
		int scissorHeight
	);

	void clearDepthTexture(GpuTexture texture, double depth);

	/**
	 * Записывает данные из CPU-буфера в GPU-буфер.
	 *
	 * @param slice  целевой срез GPU-буфера
	 * @param source данные для записи
	 */
	void writeToBuffer(GpuBufferSlice slice, ByteBuffer source);

	/**
	 * Отображает GPU-буфер в CPU-память для чтения и/или записи.
	 *
	 * @param buffer буфер для маппинга
	 * @param read   разрешить чтение из буфера
	 * @param write  разрешить запись в буфер
	 * @return представление отображённого буфера; обязательно закрыть после использования
	 */
	GpuBuffer.MappedView mapBuffer(GpuBuffer buffer, boolean read, boolean write);

	GpuBuffer.MappedView mapBuffer(GpuBufferSlice slice, boolean read, boolean write);

	void copyToBuffer(GpuBufferSlice from, GpuBufferSlice to);

	void writeToTexture(GpuTexture target, NativeImage source);

	void writeToTexture(
		GpuTexture target,
		NativeImage source,
		int mipLevel,
		int depth,
		int offsetX,
		int offsetY,
		int width,
		int height,
		int skipPixels,
		int skipRows
	);

	void writeToTexture(
		GpuTexture target,
		ByteBuffer buf,
		NativeImage.Format format,
		int mipLevel,
		int depth,
		int offsetX,
		int offsetY,
		int width,
		int height
	);

	/**
	 * Копирует содержимое текстуры в GPU-буфер асинхронно.
	 * Колбэк вызывается после завершения копирования.
	 *
	 * @param source                источник — GPU-текстура
	 * @param target                цель — GPU-буфер
	 * @param offset                смещение в целевом буфере в байтах
	 * @param dataUploadedCallback  колбэк, вызываемый после завершения операции
	 * @param mipLevel              уровень мипмапа источника
	 */
	void copyTextureToBuffer(
		GpuTexture source,
		GpuBuffer target,
		long offset,
		Runnable dataUploadedCallback,
		int mipLevel
	);

	void copyTextureToBuffer(
		GpuTexture source,
		GpuBuffer target,
		long offset,
		Runnable dataUploadedCallback,
		int mipLevel,
		int intoX,
		int intoY,
		int width,
		int height
	);

	void copyTextureToTexture(
		GpuTexture source,
		GpuTexture target,
		int mipLevel,
		int intoX,
		int intoY,
		int sourceX,
		int sourceY,
		int width,
		int height
	);

	void presentTexture(GpuTextureView texture);

	/** Создаёт барьер синхронизации для ожидания завершения текущих GPU-команд. */
	GpuFence createFence();

	/** Начинает измерение времени GPU-операций. */
	GpuQuery timerQueryBegin();

	/** Завершает измерение времени GPU-операций, начатое {@link #timerQueryBegin()}. */
	void timerQueryEnd(GpuQuery gpuQuery);
}
