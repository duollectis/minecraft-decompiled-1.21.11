package net.minecraft.client.render.command;

import com.mojang.blaze3d.systems.RenderPass;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.BillboardParticleSubmittable;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import org.jspecify.annotations.Nullable;

/**
 * Расширение {@link RenderCommandQueue} с поддержкой упорядоченных пакетных команд рендеринга.
 * Позволяет получить очередь для конкретного порядка отрисовки и регистрировать
 * пользовательские команды двух типов: {@link Custom} (произвольная геометрия) и
 * {@link LayeredCustom} (слоистые частицы с предварительной отправкой вершин).
 */
@Environment(EnvType.CLIENT)
public interface OrderedRenderCommandQueue extends RenderCommandQueue {

	RenderCommandQueue getBatchingQueue(int order);

	/** Произвольная команда рисования, получающая матрицу трансформации и потребитель вершин. */
	@Environment(EnvType.CLIENT)
	interface Custom {

		void render(MatrixStack.Entry matricesEntry, VertexConsumer vertexConsumer);
	}

	/**
	 * Слоистая пользовательская команда рисования для частиц-билбордов.
	 * Сначала вызывается {@link #submit} для подготовки вершин, затем {@link #render} для отрисовки.
	 */
	@Environment(EnvType.CLIENT)
	interface LayeredCustom {

		BillboardParticleSubmittable.@Nullable Buffers submit(LayeredCustomCommandRenderer.VerticesCache cache);

		void render(
				BillboardParticleSubmittable.Buffers buffers,
				LayeredCustomCommandRenderer.VerticesCache cache,
				RenderPass renderPass,
				TextureManager manager,
				boolean translucent
		);
	}
}
