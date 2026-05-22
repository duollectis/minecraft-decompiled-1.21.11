package net.minecraft.client.gui.render;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.TexturedQuadGuiElementRenderState;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.math.MatrixStack;
import org.jspecify.annotations.Nullable;

/**
 * Базовый класс для рендереров специальных GUI-элементов, требующих отрисовки
 * в отдельную off-screen текстуру (PIP — Picture-In-Picture).
 *
 * <p>Алгоритм работы:
 * <ol>
 *   <li>Создаёт или переиспользует пару текстур (цвет + глубина) нужного размера.</li>
 *   <li>Перенаправляет вывод рендера в эти текстуры через {@code outputColorTextureOverride}.</li>
 *   <li>Вызывает {@link #render(SpecialGuiElementRenderState, MatrixStack)} у подкласса.</li>
 *   <li>Добавляет текстурированный квад в основной GUI-слой для отображения результата.</li>
 * </ol>
 *
 * <p>Если {@link #shouldBypassScaling} возвращает {@code true}, повторная отрисовка
 * в текстуру пропускается и используется уже готовый результат предыдущего кадра.
 *
 * @param <T> тип состояния специального GUI-элемента
 */
@Environment(EnvType.CLIENT)
public abstract class SpecialGuiElementRenderer<T extends SpecialGuiElementRenderState> implements AutoCloseable {

	private static final int TEXTURE_USAGE_FLAGS = 12;
	private static final int DEPTH_TEXTURE_USAGE_FLAGS = 8;
	private static final float PROJECTION_NEAR = -1000.0F;
	private static final float PROJECTION_FAR = 1000.0F;

	protected final VertexConsumerProvider.Immediate vertexConsumers;

	private @Nullable GpuTexture texture;
	private @Nullable GpuTextureView textureView;
	private @Nullable GpuTexture depthTexture;
	private @Nullable GpuTextureView depthTextureView;
	private final ProjectionMatrix2 projectionMatrix =
			new ProjectionMatrix2("PIP - " + getClass().getSimpleName(), PROJECTION_NEAR, PROJECTION_FAR, true);

	protected SpecialGuiElementRenderer(VertexConsumerProvider.Immediate vertexConsumers) {
		this.vertexConsumers = vertexConsumers;
	}

	/**
	 * Выполняет полный цикл рендеринга специального элемента.
	 * Если размер текстуры изменился или {@link #shouldBypassScaling} вернул {@code false},
	 * перерисовывает содержимое в off-screen текстуру, иначе переиспользует кэш.
	 */
	public void render(T elementState, GuiRenderState state, int windowScaleFactor) {
		int width = (elementState.x2() - elementState.x1()) * windowScaleFactor;
		int height = (elementState.y2() - elementState.y1()) * windowScaleFactor;
		boolean needsResize = texture == null
				|| texture.getWidth(0) != width
				|| texture.getHeight(0) != height;

		if (!needsResize && shouldBypassScaling(elementState)) {
			renderElement(elementState, state);
			return;
		}

		prepareTextures(needsResize, width, height);
		RenderSystem.outputColorTextureOverride = textureView;
		RenderSystem.outputDepthTextureOverride = depthTextureView;

		MatrixStack matrices = new MatrixStack();
		matrices.translate(width / 2.0F, getYOffset(height, windowScaleFactor), 0.0F);
		float scale = windowScaleFactor * elementState.scale();
		matrices.scale(scale, scale, -scale);

		render(elementState, matrices);
		vertexConsumers.draw();

		RenderSystem.outputColorTextureOverride = null;
		RenderSystem.outputDepthTextureOverride = null;

		renderElement(elementState, state);
	}

	/**
	 * Добавляет текстурированный квад с результатом off-screen рендера в текущий GUI-слой.
	 */
	protected void renderElement(T element, GuiRenderState state) {
		state.addSimpleElementToCurrentLayer(
				new TexturedQuadGuiElementRenderState(
						RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
						TextureSetup.of(
								textureView,
								RenderSystem.getSamplerCache().getRepeated(FilterMode.NEAREST)
						),
						element.pose(),
						element.x1(),
						element.y1(),
						element.x2(),
						element.y2(),
						0.0F,
						1.0F,
						1.0F,
						0.0F,
						-1,
						element.scissorArea(),
						null
				)
		);
	}

	/**
	 * Создаёт или пересоздаёт off-screen текстуры нужного размера.
	 * Если {@code closePrevious} равен {@code true}, старые текстуры освобождаются.
	 */
	private void prepareTextures(boolean closePrevious, int width, int height) {
		if (texture != null && closePrevious) {
			texture.close();
			texture = null;
			textureView.close();
			textureView = null;
			depthTexture.close();
			depthTexture = null;
			depthTextureView.close();
			depthTextureView = null;
		}

		GpuDevice gpuDevice = RenderSystem.getDevice();

		if (texture == null) {
			texture = gpuDevice.createTexture(
					() -> "UI " + getName() + " texture",
					TEXTURE_USAGE_FLAGS,
					TextureFormat.RGBA8,
					width,
					height,
					1,
					1
			);
			textureView = gpuDevice.createTextureView(texture);
			depthTexture = gpuDevice.createTexture(
					() -> "UI " + getName() + " depth texture",
					DEPTH_TEXTURE_USAGE_FLAGS,
					TextureFormat.DEPTH32,
					width,
					height,
					1,
					1
			);
			depthTextureView = gpuDevice.createTextureView(depthTexture);
		}

		gpuDevice.createCommandEncoder().clearColorAndDepthTextures(texture, 0, depthTexture, 1.0);
		RenderSystem.setProjectionMatrix(projectionMatrix.set(width, height), ProjectionType.ORTHOGRAPHIC);
	}

	/**
	 * Определяет, можно ли пропустить повторную отрисовку в текстуру.
	 * По умолчанию всегда возвращает {@code false} — перерисовка обязательна.
	 * Подклассы могут переопределить для кэширования статичных элементов.
	 */
	protected boolean shouldBypassScaling(T elementState) {
		return false;
	}

	/**
	 * Возвращает смещение по Y для центрирования содержимого в off-screen текстуре.
	 * По умолчанию равно полной высоте текстуры (нижний край).
	 */
	protected float getYOffset(int height, int windowScaleFactor) {
		return height;
	}

	@Override
	public void close() {
		if (texture != null) {
			texture.close();
		}

		if (textureView != null) {
			textureView.close();
		}

		if (depthTexture != null) {
			depthTexture.close();
		}

		if (depthTextureView != null) {
			depthTextureView.close();
		}

		projectionMatrix.close();
	}

	public abstract Class<T> getElementClass();

	/**
	 * Выполняет непосредственную отрисовку содержимого элемента в off-screen текстуру.
	 *
	 * @param state   состояние элемента с данными для рендера
	 * @param matrices стек матриц, уже настроенный на нужный масштаб и позицию
	 */
	protected abstract void render(T state, MatrixStack matrices);

	protected abstract String getName();
}
