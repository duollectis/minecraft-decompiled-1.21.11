package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.util.annotation.DeobfuscateClass;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Проход рендеринга (render pass) — единица работы GPU, выполняющая отрисовку
 * в заданные цветовые и/или глубинные вложения.
 *
 * <p>Создаётся через {@link CommandEncoder#createRenderPass} и обязательно
 * закрывается после завершения всех команд отрисовки.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public interface RenderPass extends AutoCloseable {

	void pushDebugGroup(Supplier<String> labelGetter);

	void popDebugGroup();

	/**
	 * Устанавливает активный рендер-конвейер для последующих команд отрисовки.
	 * Должен быть вызван перед любой командой draw.
	 */
	void setPipeline(RenderPipeline pipeline);

	/**
	 * Привязывает текстуру к именованному сэмплеру шейдера.
	 *
	 * @param name           имя сэмплера в шейдере
	 * @param gpuTextureView view текстуры, или {@code null} для отвязки
	 * @param sampler        сэмплер с параметрами фильтрации, или {@code null} для отвязки
	 */
	void bindTexture(String name, @Nullable GpuTextureView gpuTextureView, @Nullable GpuSampler sampler);

	void setUniform(String name, GpuBuffer buffer);

	void setUniform(String name, GpuBufferSlice slice);

	void enableScissor(int x, int y, int width, int height);

	void disableScissor();

	void setVertexBuffer(int index, GpuBuffer buffer);

	void setIndexBuffer(GpuBuffer indexBuffer, VertexFormat.IndexType indexType);

	/**
	 * Выполняет индексированную отрисовку.
	 *
	 * @param baseVertex    смещение базовой вершины
	 * @param firstIndex    первый индекс в индексном буфере
	 * @param count         количество индексов для отрисовки
	 * @param instanceCount количество экземпляров
	 */
	void drawIndexed(int baseVertex, int firstIndex, int count, int instanceCount);

	/**
	 * Выполняет множественную индексированную отрисовку для набора объектов.
	 * Позволяет эффективно отрисовать несколько объектов с разными uniform-данными
	 * за один вызов без смены конвейера.
	 *
	 * @param objects                    коллекция объектов для отрисовки
	 * @param buffer                     общий вершинный буфер, или {@code null}
	 * @param indexType                  тип индексов, или {@code null}
	 * @param validationSkippedUniforms  имена uniform-переменных, для которых пропускается валидация
	 * @param object                     пользовательский объект, передаваемый в колбэк загрузки uniform
	 */
	<T> void drawMultipleIndexed(
		Collection<RenderObject<T>> objects,
		@Nullable GpuBuffer buffer,
		VertexFormat.@Nullable IndexType indexType,
		Collection<String> validationSkippedUniforms,
		T object
	);

	void draw(int offset, int count);

	@Override
	void close();

	/**
	 * Описание одного объекта для отрисовки в {@link #drawMultipleIndexed}.
	 *
	 * @param slot                      индекс слота вершинного буфера
	 * @param vertexBuffer              вершинный буфер объекта
	 * @param indexBuffer               индексный буфер, или {@code null} для использования общего
	 * @param indexType                 тип индексов, или {@code null}
	 * @param firstIndex                первый индекс в индексном буфере
	 * @param indexCount                количество индексов
	 * @param uniformUploaderConsumer   колбэк для загрузки uniform-данных объекта, или {@code null}
	 */
	@Environment(EnvType.CLIENT)
	record RenderObject<T>(
		int slot,
		GpuBuffer vertexBuffer,
		@Nullable GpuBuffer indexBuffer,
		VertexFormat.@Nullable IndexType indexType,
		int firstIndex,
		int indexCount,
		@Nullable BiConsumer<T, UniformUploader> uniformUploaderConsumer
	) {

		public RenderObject(
			int slot,
			GpuBuffer vertexBuffer,
			GpuBuffer indexBuffer,
			VertexFormat.IndexType indexType,
			int firstIndex,
			int indexCount
		) {
			this(slot, vertexBuffer, indexBuffer, indexType, firstIndex, indexCount, null);
		}
	}

	/** Интерфейс для загрузки uniform-данных в рамках {@link #drawMultipleIndexed}. */
	@Environment(EnvType.CLIENT)
	interface UniformUploader {

		void upload(String name, GpuBufferSlice slice);
	}
}
