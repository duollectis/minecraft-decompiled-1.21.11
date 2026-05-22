package net.minecraft.client.gui.render;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextDrawable;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GlyphGuiElementRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.gui.render.state.TextGuiElementRenderState;
import net.minecraft.client.gui.render.state.TexturedQuadGuiElementRenderState;
import net.minecraft.client.gui.render.state.special.OversizedItemGuiElementRenderState;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.item.KeyedItemRenderState;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Центральный рендерер GUI. Управляет полным циклом отрисовки одного кадра GUI:
 * <ol>
 *   <li>Подготовка специальных, предметных и текстовых элементов в off-screen текстуры.</li>
 *   <li>Сортировка и упаковка простых элементов в вершинные буферы.</li>
 *   <li>Отрисовка всех буферов в основной фреймбуфер с поддержкой blur-разделения.</li>
 * </ol>
 *
 * <p>Предметы запекаются в общий атлас текстур ({@code itemAtlasTexture}) для минимизации
 * draw calls. Oversized-предметы рендерятся отдельно через {@link OversizedItemGuiElementRenderer}.
 */
@Environment(EnvType.CLIENT)
public class GuiRenderer implements AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();

	// Параметры проекции GUI
	private static final float GUI_PROJECTION_NEAR = 1000.0F;
	private static final float GUI_PROJECTION_FAR = 11000.0F;
	private static final float ITEMS_PROJECTION_NEAR = -1000.0F;
	private static final float ITEMS_PROJECTION_FAR = 1000.0F;
	private static final float GUI_Z_TRANSLATION = -11000.0F;

	// Параметры атласа предметов
	public static final int ITEM_SLOT_SIZE = 16;
	private static final int ITEM_ATLAS_MIN_SIZE = 512;
	private static final int ITEM_ATLAS_MAX_SIZE = RenderSystem.getDevice().getMaxTextureSize();
	private static final int TEXTURE_USAGE_FLAGS = 12;
	private static final int DEPTH_TEXTURE_USAGE_FLAGS = 8;
	private static final int GPU_BUFFER_USAGE = 34;

	// Публичные константы для внешнего использования
	public static final float Z_NEAR = 0.0F;
	public static final int MAX_Z = 1000;
	public static final int MIN_Z = -1000;
	public static final int INITIAL_LAYER = 0;

	private static final Comparator<ScreenRect> SCISSOR_AREA_COMPARATOR = Comparator.nullsFirst(
			Comparator
					.comparing(ScreenRect::getTop)
					.thenComparing(ScreenRect::getBottom)
					.thenComparing(ScreenRect::getLeft)
					.thenComparing(ScreenRect::getRight)
	);

	private static final Comparator<TextureSetup> TEXTURE_SETUP_COMPARATOR =
			Comparator.nullsFirst(Comparator.comparing(TextureSetup::getSortKey));

	private static final Comparator<SimpleGuiElementRenderState> SIMPLE_ELEMENT_COMPARATOR =
			Comparator.comparing(SimpleGuiElementRenderState::scissorArea, SCISSOR_AREA_COMPARATOR)
					.thenComparing(
							SimpleGuiElementRenderState::pipeline,
							java.util.Comparator.comparing(RenderPipeline::getSortKey)
					)
					.thenComparing(SimpleGuiElementRenderState::textureSetup, TEXTURE_SETUP_COMPARATOR);

	final GuiRenderState state;

	private final Map<Object, RenderedItem> renderedItems = new Object2ObjectOpenHashMap<>();
	private final Map<Object, OversizedItemGuiElementRenderer> oversizedItems = new Object2ObjectOpenHashMap<>();
	private final List<Draw> draws = new ArrayList<>();
	private final List<Preparation> preparations = new ArrayList<>();
	private final BufferAllocator allocator = new BufferAllocator(786432);
	private final Map<VertexFormat, MappableRingBuffer> bufferByVertexFormat = new Object2ObjectOpenHashMap<>();
	private final ProjectionMatrix2 guiProjectionMatrix =
			new ProjectionMatrix2("gui", GUI_PROJECTION_NEAR, GUI_PROJECTION_FAR, true);
	private final ProjectionMatrix2 itemsProjectionMatrix =
			new ProjectionMatrix2("items", ITEMS_PROJECTION_NEAR, ITEMS_PROJECTION_FAR, true);
	private final VertexConsumerProvider.Immediate vertexConsumers;
	private final OrderedRenderCommandQueue commandQueue;
	private final RenderDispatcher dispatcher;
	private final Map<Class<? extends SpecialGuiElementRenderState>, SpecialGuiElementRenderer<?>> specialElementRenderers;

	private @Nullable GpuTexture itemAtlasTexture;
	private @Nullable GpuTextureView itemAtlasTextureView;
	private @Nullable GpuTexture itemAtlasDepthTexture;
	private @Nullable GpuTextureView itemAtlasDepthTextureView;
	private int itemAtlasX;
	private int itemAtlasY;
	private int windowScaleFactor;
	private int frame;
	private int blurLayer = Integer.MAX_VALUE;

	// Временное состояние подготовки простых элементов
	private @Nullable ScreenRect scissorArea;
	private @Nullable RenderPipeline pipeline;
	private @Nullable TextureSetup textureSetup;
	private @Nullable BufferBuilder buffer;

	public GuiRenderer(
			GuiRenderState state,
			VertexConsumerProvider.Immediate vertexConsumers,
			OrderedRenderCommandQueue queue,
			RenderDispatcher dispatcher,
			List<SpecialGuiElementRenderer<?>> specialElementRenderers
	) {
		this.state = state;
		this.vertexConsumers = vertexConsumers;
		this.commandQueue = queue;
		this.dispatcher = dispatcher;

		ImmutableMap.Builder<Class<? extends SpecialGuiElementRenderState>, SpecialGuiElementRenderer<?>> builder =
				ImmutableMap.builder();

		for (SpecialGuiElementRenderer<?> renderer : specialElementRenderers) {
			builder.put(renderer.getElementClass(), renderer);
		}

		this.specialElementRenderers = builder.buildOrThrow();
	}

	public void incrementFrame() {
		frame++;
	}

	/**
	 * Выполняет полный цикл рендеринга GUI за один кадр.
	 * Подготавливает все элементы, отрисовывает их в фреймбуфер,
	 * затем очищает состояние для следующего кадра.
	 */
	public void render(GpuBufferSlice fogBuffer) {
		prepare();
		renderPreparedDraws(fogBuffer);

		for (MappableRingBuffer ringBuffer : bufferByVertexFormat.values()) {
			ringBuffer.rotate();
		}

		draws.clear();
		preparations.clear();
		state.clear();
		blurLayer = Integer.MAX_VALUE;
		clearOversizedItems();

		if (SharedConstants.SHUFFLE_UI_RENDERING_ORDER) {
			RenderPipeline.updateSortKeySeed();
			TextureSetup.shuffleRenderingOrder();
		}
	}

	/**
	 * Удаляет oversized-рендереры, которые не использовались в этом кадре.
	 * Для использованных сбрасывает флаг oversized для следующего кадра.
	 */
	private void clearOversizedItems() {
		var iterator = oversizedItems.entrySet().iterator();

		while (iterator.hasNext()) {
			var entry = iterator.next();
			OversizedItemGuiElementRenderer renderer = entry.getValue();

			if (!renderer.isOversized()) {
				renderer.close();
				iterator.remove();
			} else {
				renderer.clearOversized();
			}
		}
	}

	private void prepare() {
		vertexConsumers.draw();
		prepareSpecialElements();
		prepareItemElements();
		prepareTextElements();
		state.sortSimpleElements(SIMPLE_ELEMENT_COMPARATOR);
		prepareSimpleElements(GuiRenderState.LayerFilter.BEFORE_BLUR);
		blurLayer = preparations.size();
		prepareSimpleElements(GuiRenderState.LayerFilter.AFTER_BLUR);
		finishPreparation();
	}

	private void prepareSimpleElements(GuiRenderState.LayerFilter filter) {
		scissorArea = null;
		pipeline = null;
		textureSetup = null;
		buffer = null;

		state.forEachSimpleElement(this::prepareSimpleElement, filter);

		if (buffer != null) {
			endBuffer(buffer, pipeline, textureSetup, scissorArea);
		}
	}

	/**
	 * Отрисовывает все подготовленные draw-вызовы в основной фреймбуфер.
	 * Если есть элементы до blur — рендерит их, затем применяет blur и рендерит остальные.
	 */
	private void renderPreparedDraws(GpuBufferSlice fogBuffer) {
		if (draws.isEmpty()) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		Window window = client.getWindow();

		RenderSystem.setProjectionMatrix(
				guiProjectionMatrix.set(
						(float) window.getFramebufferWidth() / window.getScaleFactor(),
						(float) window.getFramebufferHeight() / window.getScaleFactor()
				),
				ProjectionType.ORTHOGRAPHIC
		);

		Framebuffer framebuffer = client.getFramebuffer();
		int maxIndexCount = 0;

		for (Draw draw : draws) {
			if (draw.indexCount > maxIndexCount) {
				maxIndexCount = draw.indexCount;
			}
		}

		RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
		GpuBuffer indexBuffer = shapeIndexBuffer.getIndexBuffer(maxIndexCount);
		VertexFormat.IndexType indexType = shapeIndexBuffer.getIndexType();

		GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().write(
				new Matrix4f().setTranslation(0.0F, 0.0F, GUI_Z_TRANSLATION),
				new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
				new Vector3f(),
				new Matrix4f()
		);

		if (blurLayer > 0) {
			renderRange(
					() -> "GUI before blur",
					framebuffer,
					fogBuffer,
					dynamicTransforms,
					indexBuffer,
					indexType,
					0,
					Math.min(blurLayer, draws.size())
			);
		}

		if (draws.size() > blurLayer) {
			RenderSystem.getDevice()
					.createCommandEncoder()
					.clearDepthTexture(framebuffer.getDepthAttachment(), 1.0);
			client.gameRenderer.renderBlur();
			renderRange(
					() -> "GUI after blur",
					framebuffer,
					fogBuffer,
					dynamicTransforms,
					indexBuffer,
					indexType,
					blurLayer,
					draws.size()
			);
		}
	}

	/**
	 * Создаёт render pass и отрисовывает диапазон draw-вызовов [{@code from}, {@code to}).
	 */
	private void renderRange(
			Supplier<String> nameSupplier,
			Framebuffer framebuffer,
			GpuBufferSlice fogBuffer,
			GpuBufferSlice dynamicTransformsBuffer,
			GpuBuffer indexBuffer,
			VertexFormat.IndexType indexType,
			int from,
			int to
	) {
		try (var renderPass = RenderSystem.getDevice()
				.createCommandEncoder()
				.createRenderPass(
						nameSupplier,
						framebuffer.getColorAttachmentView(),
						OptionalInt.empty(),
						framebuffer.useDepthAttachment ? framebuffer.getDepthAttachmentView() : null,
						OptionalDouble.empty()
				)
		) {
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("Fog", fogBuffer);
			renderPass.setUniform("DynamicTransforms", dynamicTransformsBuffer);

			for (int index = from; index < to; index++) {
				renderDraw(draws.get(index), renderPass, indexBuffer, indexType);
			}
		}
	}

	private void prepareSimpleElement(SimpleGuiElementRenderState elementState) {
		RenderPipeline elementPipeline = elementState.pipeline();
		TextureSetup elementTextureSetup = elementState.textureSetup();
		ScreenRect elementScissor = elementState.scissorArea();

		boolean needsNewBuffer = elementPipeline != pipeline
				|| scissorChanged(elementScissor, scissorArea)
				|| !elementTextureSetup.equals(textureSetup);

		if (needsNewBuffer) {
			if (buffer != null) {
				endBuffer(buffer, pipeline, textureSetup, scissorArea);
			}

			buffer = startBuffer(elementPipeline);
			pipeline = elementPipeline;
			textureSetup = elementTextureSetup;
			scissorArea = elementScissor;
		}

		elementState.setupVertices(buffer);
	}

	/**
	 * Подготавливает текстовые элементы: растеризует каждый в набор глифов
	 * и добавляет их как простые элементы в текущий слой.
	 */
	private void prepareTextElements() {
		state.forEachTextElement(textState -> {
			final var matrix = textState.matrix;
			final var clipBounds = textState.clipBounds;

			textState.prepare().draw(new TextRenderer.GlyphDrawer() {
				@Override
				public void drawGlyph(TextDrawable.DrawnGlyphRect glyph) {
					addGlyph(glyph);
				}

				@Override
				public void drawRectangle(TextDrawable rect) {
					addGlyph(rect);
				}

				private void addGlyph(TextDrawable drawable) {
					GuiRenderer.this.state.addPreparedTextElement(
							new GlyphGuiElementRenderState(matrix, drawable, clipBounds)
					);
				}
			});
		});
	}

	/**
	 * Подготавливает предметы для рендеринга: запекает их в атлас текстур.
	 * Oversized-предметы обрабатываются отдельно после основного прохода.
	 */
	private void prepareItemElements() {
		if (state.getItemModelKeys().isEmpty()) {
			return;
		}

		int scaleFactor = getWindowScaleFactor();
		int itemPixelSize = ITEM_SLOT_SIZE * scaleFactor;
		int atlasSideLength = calcItemAtlasSideLength(itemPixelSize);

		if (itemAtlasTexture == null) {
			createItemAtlas(atlasSideLength);
		}

		RenderSystem.outputColorTextureOverride = itemAtlasTextureView;
		RenderSystem.outputDepthTextureOverride = itemAtlasDepthTextureView;
		RenderSystem.setProjectionMatrix(itemsProjectionMatrix.set(atlasSideLength, atlasSideLength), ProjectionType.ORTHOGRAPHIC);
		MinecraftClient.getInstance().gameRenderer
				.getDiffuseLighting()
				.setShaderLights(net.minecraft.client.render.DiffuseLighting.Type.ITEMS_3D);

		MatrixStack matrices = new MatrixStack();
		MutableBoolean atlasOverflow = new MutableBoolean(false);
		MutableBoolean hasOversized = new MutableBoolean(false);

		state.forEachItemElement(elem -> {
			if (elem.oversizedBounds() != null) {
				hasOversized.setTrue();
			} else {
				bakeItemToAtlas(elem, matrices, itemPixelSize, atlasSideLength, atlasOverflow);
			}
		});

		RenderSystem.outputColorTextureOverride = null;
		RenderSystem.outputDepthTextureOverride = null;

		if (hasOversized.booleanValue()) {
			renderOversizedItems(scaleFactor);
		}
	}

	/**
	 * Запекает один предмет в атлас текстур.
	 * Если предмет уже есть в кэше и не анимирован — использует кэшированные UV.
	 * Если атлас переполнен — логирует предупреждение и пропускает предмет.
	 */
	private void bakeItemToAtlas(
			ItemGuiElementRenderState elem,
			MatrixStack matrices,
			int itemPixelSize,
			int atlasSideLength,
			MutableBoolean atlasOverflow
	) {
		KeyedItemRenderState keyedState = elem.state();
		RenderedItem cached = renderedItems.get(keyedState.getModelKey());
		boolean needsRebake = cached == null || (keyedState.isAnimated() && cached.frame != frame);

		if (!needsRebake) {
			prepareItem(elem, cached.u, cached.v, itemPixelSize, atlasSideLength);
			return;
		}

		if (itemAtlasX + itemPixelSize > atlasSideLength) {
			itemAtlasX = 0;
			itemAtlasY += itemPixelSize;
		}

		boolean isAnimatedRebake = keyedState.isAnimated() && cached != null;

		if (!isAnimatedRebake && itemAtlasY + itemPixelSize > atlasSideLength) {
			if (atlasOverflow.isFalse()) {
				LOGGER.warn("Trying to render too many items in GUI at the same time. Skipping some of them.");
				atlasOverflow.setTrue();
			}

			return;
		}

		int bakeX = isAnimatedRebake ? cached.x : itemAtlasX;
		int bakeY = isAnimatedRebake ? cached.y : itemAtlasY;

		if (isAnimatedRebake) {
			RenderSystem.getDevice()
					.createCommandEncoder()
					.clearColorAndDepthTextures(
							itemAtlasTexture,
							0,
							itemAtlasDepthTexture,
							1.0,
							bakeX,
							atlasSideLength - bakeY - itemPixelSize,
							itemPixelSize,
							itemPixelSize
					);
		}

		prepareItemInitially(keyedState, matrices, bakeX, bakeY, itemPixelSize);

		float u = (float) bakeX / atlasSideLength;
		float v = (float) (atlasSideLength - bakeY) / atlasSideLength;

		prepareItem(elem, u, v, itemPixelSize, atlasSideLength);

		if (isAnimatedRebake) {
			cached.frame = frame;
		} else {
			renderedItems.put(
					elem.state().getModelKey(),
					new RenderedItem(itemAtlasX, itemAtlasY, u, v, frame)
			);
			itemAtlasX += itemPixelSize;
		}
	}

	private void renderOversizedItems(int scaleFactor) {
		state.forEachItemElement(elem -> {
			if (elem.oversizedBounds() == null) {
				return;
			}

			KeyedItemRenderState keyedState = elem.state();
			OversizedItemGuiElementRenderer renderer = oversizedItems.computeIfAbsent(
					keyedState.getModelKey(),
					key -> new OversizedItemGuiElementRenderer(vertexConsumers)
			);

			ScreenRect oversizedRect = elem.oversizedBounds();
			OversizedItemGuiElementRenderState oversizedState = new OversizedItemGuiElementRenderState(
					elem,
					oversizedRect.getLeft(),
					oversizedRect.getTop(),
					oversizedRect.getRight(),
					oversizedRect.getBottom()
			);

			renderer.render(oversizedState, state, scaleFactor);
		});
	}

	private void prepareSpecialElements() {
		int scaleFactor = MinecraftClient.getInstance().getWindow().getScaleFactor();
		state.forEachSpecialElement(elementState -> prepareSpecialElement(elementState, scaleFactor));
	}

	@SuppressWarnings("unchecked")
	private <T extends SpecialGuiElementRenderState> void prepareSpecialElement(T elementState, int scaleFactor) {
		SpecialGuiElementRenderer<T> renderer =
				(SpecialGuiElementRenderer<T>) specialElementRenderers.get(elementState.getClass());

		if (renderer != null) {
			renderer.render(elementState, state, scaleFactor);
		}
	}

	/**
	 * Рендерит предмет в указанную позицию атласа.
	 * Настраивает scissor для ограничения отрисовки областью тайла в атласе.
	 */
	private void prepareItemInitially(KeyedItemRenderState keyedState, MatrixStack matrices, int x, int y, int scale) {
		matrices.push();
		matrices.translate(x + scale / 2.0F, y + scale / 2.0F, 0.0F);
		matrices.scale(scale, -scale, scale);

		if (!keyedState.isSideLit()) {
			MinecraftClient.getInstance().gameRenderer
					.getDiffuseLighting()
					.setShaderLights(net.minecraft.client.render.DiffuseLighting.Type.ITEMS_FLAT);
		} else {
			MinecraftClient.getInstance().gameRenderer
					.getDiffuseLighting()
					.setShaderLights(net.minecraft.client.render.DiffuseLighting.Type.ITEMS_3D);
		}

		RenderSystem.enableScissorForRenderTypeDraws(
				x,
				itemAtlasTexture.getHeight(0) - y - scale,
				scale,
				scale
		);

		keyedState.render(matrices, commandQueue, 15728880, net.minecraft.client.render.OverlayTexture.DEFAULT_UV, 0);
		dispatcher.render();
		vertexConsumers.draw();

		RenderSystem.disableScissorForRenderTypeDraws();
		matrices.pop();
	}

	/**
	 * Добавляет текстурированный квад предмета из атласа в текущий GUI-слой.
	 */
	private void prepareItem(
			ItemGuiElementRenderState elem,
			float u,
			float v,
			int pixelsPerItem,
			int atlasSideLength
	) {
		float u2 = u + (float) pixelsPerItem / atlasSideLength;
		float v2 = v + (float) (-pixelsPerItem) / atlasSideLength;

		state.addSimpleElementToCurrentLayer(
				new TexturedQuadGuiElementRenderState(
						RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
						TextureSetup.of(
								itemAtlasTextureView,
								RenderSystem.getSamplerCache().getRepeated(FilterMode.NEAREST)
						),
						elem.pose(),
						elem.x(),
						elem.y(),
						elem.x() + ITEM_SLOT_SIZE,
						elem.y() + ITEM_SLOT_SIZE,
						u,
						u2,
						v,
						v2,
						-1,
						elem.scissorArea(),
						null
				)
		);
	}

	private void createItemAtlas(int sideLength) {
		GpuDevice gpuDevice = RenderSystem.getDevice();

		itemAtlasTexture = gpuDevice.createTexture(
				"UI items atlas",
				TEXTURE_USAGE_FLAGS,
				TextureFormat.RGBA8,
				sideLength,
				sideLength,
				1,
				1
		);
		itemAtlasTextureView = gpuDevice.createTextureView(itemAtlasTexture);
		itemAtlasDepthTexture = gpuDevice.createTexture(
				"UI items atlas depth",
				DEPTH_TEXTURE_USAGE_FLAGS,
				TextureFormat.DEPTH32,
				sideLength,
				sideLength,
				1,
				1
		);
		itemAtlasDepthTextureView = gpuDevice.createTextureView(itemAtlasDepthTexture);

		gpuDevice.createCommandEncoder()
				.clearColorAndDepthTextures(itemAtlasTexture, 0, itemAtlasDepthTexture, 1.0);
	}

	/**
	 * Вычисляет необходимый размер стороны атласа предметов.
	 * Если текущий атлас достаточно велик — переиспользует его.
	 * Иначе пересоздаёт с запасом 50% для будущих предметов.
	 */
	private int calcItemAtlasSideLength(int itemPixelSize) {
		Set<Object> modelKeys = state.getItemModelKeys();
		int totalItems;

		if (renderedItems.isEmpty()) {
			totalItems = modelKeys.size();
		} else {
			totalItems = renderedItems.size();

			for (Object key : modelKeys) {
				if (!renderedItems.containsKey(key)) {
					totalItems++;
				}
			}
		}

		if (itemAtlasTexture != null) {
			int tilesPerSide = itemAtlasTexture.getWidth(0) / itemPixelSize;
			int capacity = tilesPerSide * tilesPerSide;

			if (totalItems < capacity) {
				return itemAtlasTexture.getWidth(0);
			}

			onItemAtlasChanged();
		}

		int itemCount = modelKeys.size();
		int squareSide = MathHelper.smallestEncompassingSquareSideLength(itemCount + itemCount / 2);
		long rawSize = (long) MathHelper.smallestEncompassingPowerOfTwo(squareSide * itemPixelSize);

		return (int) Math.clamp(rawSize, ITEM_ATLAS_MIN_SIZE, ITEM_ATLAS_MAX_SIZE);
	}

	/**
	 * Возвращает текущий scale-фактор окна.
	 * При изменении сбрасывает атлас предметов и кэши oversized-рендереров.
	 */
	private int getWindowScaleFactor() {
		int scaleFactor = MinecraftClient.getInstance().getWindow().getScaleFactor();

		if (scaleFactor == windowScaleFactor) {
			return scaleFactor;
		}

		onItemAtlasChanged();

		for (OversizedItemGuiElementRenderer renderer : oversizedItems.values()) {
			renderer.clearModel();
		}

		windowScaleFactor = scaleFactor;
		return scaleFactor;
	}

	private void onItemAtlasChanged() {
		itemAtlasX = 0;
		itemAtlasY = 0;
		renderedItems.clear();

		if (itemAtlasTexture != null) {
			itemAtlasTexture.close();
			itemAtlasTexture = null;
		}

		if (itemAtlasTextureView != null) {
			itemAtlasTextureView.close();
			itemAtlasTextureView = null;
		}

		if (itemAtlasDepthTexture != null) {
			itemAtlasDepthTexture.close();
			itemAtlasDepthTexture = null;
		}

		if (itemAtlasDepthTextureView != null) {
			itemAtlasDepthTextureView.close();
			itemAtlasDepthTextureView = null;
		}
	}

	private void endBuffer(
			BufferBuilder builder,
			RenderPipeline renderPipeline,
			TextureSetup texSetup,
			@Nullable ScreenRect scissor
	) {
		BuiltBuffer builtBuffer = builder.endNullable();

		if (builtBuffer != null) {
			preparations.add(new Preparation(builtBuffer, renderPipeline, texSetup, scissor));
		}
	}

	/**
		* Финализирует подготовку: инициализирует вершинные буферы и копирует
		* данные из {@link BuiltBuffer} в GPU-буферы через {@link MappableRingBuffer}.
		*/
	private void finishPreparation() {
		initVertexBuffers();

		var commandEncoder = RenderSystem.getDevice().createCommandEncoder();
		Object2IntMap<VertexFormat> offsetByFormat = new Object2IntOpenHashMap<>();

		for (Preparation preparation : preparations) {
			BuiltBuffer builtBuffer = preparation.mesh;
			BuiltBuffer.DrawParameters drawParams = builtBuffer.getDrawParameters();
			VertexFormat vertexFormat = drawParams.format();
			MappableRingBuffer ringBuffer = bufferByVertexFormat.get(vertexFormat);

			if (!offsetByFormat.containsKey(vertexFormat)) {
				offsetByFormat.put(vertexFormat, 0);
			}

			ByteBuffer data = builtBuffer.getBuffer();
			int dataSize = data.remaining();
			int offset = offsetByFormat.getInt(vertexFormat);

			try (var mappedView = commandEncoder.mapBuffer(
					ringBuffer.getBlocking().slice(offset, dataSize),
					false,
					true
			)) {
				MemoryUtil.memCopy(data, mappedView.data());
			}

			offsetByFormat.put(vertexFormat, offset + dataSize);

			draws.add(new Draw(
					ringBuffer.getBlocking(),
					offset / vertexFormat.getVertexSize(),
					drawParams.mode(),
					drawParams.indexCount(),
					preparation.pipeline,
					preparation.textureSetup,
					preparation.scissorArea
			));

			preparation.close();
		}
	}

	/**
		* Инициализирует или расширяет GPU-буферы для каждого используемого формата вершин.
		* Если существующий буфер слишком мал — пересоздаёт его.
		*/
	private void initVertexBuffers() {
		Object2IntMap<VertexFormat> sizeByFormat = collectVertexSizes();

		for (var entry : sizeByFormat.object2IntEntrySet()) {
			VertexFormat vertexFormat = entry.getKey();
			int requiredSize = entry.getIntValue();
			MappableRingBuffer existing = bufferByVertexFormat.get(vertexFormat);

			if (existing != null && existing.size() >= requiredSize) {
				continue;
			}

			if (existing != null) {
				existing.close();
			}

			bufferByVertexFormat.put(
					vertexFormat,
					new MappableRingBuffer(
							() -> "GUI vertex buffer for " + vertexFormat,
							GPU_BUFFER_USAGE,
							requiredSize
					)
			);
		}
	}

	private Object2IntMap<VertexFormat> collectVertexSizes() {
		Object2IntMap<VertexFormat> sizeByFormat = new Object2IntOpenHashMap<>();

		for (Preparation preparation : preparations) {
			BuiltBuffer.DrawParameters drawParams = preparation.mesh.getDrawParameters();
			VertexFormat vertexFormat = drawParams.format();

			if (!sizeByFormat.containsKey(vertexFormat)) {
				sizeByFormat.put(vertexFormat, 0);
			}

			sizeByFormat.put(
					vertexFormat,
					sizeByFormat.getInt(vertexFormat) + drawParams.vertexCount() * vertexFormat.getVertexSize()
			);
		}

		return sizeByFormat;
	}

	private void renderDraw(Draw draw, com.mojang.blaze3d.systems.RenderPass pass, GpuBuffer indexBuffer, VertexFormat.IndexType indexType) {
		pass.setPipeline(draw.pipeline());
		pass.setVertexBuffer(0, draw.vertexBuffer);

		ScreenRect drawScissor = draw.scissorArea();

		if (drawScissor != null) {
			enableScissor(drawScissor, pass);
		} else {
			pass.disableScissor();
		}

		if (draw.textureSetup.texure0() != null) {
			pass.bindTexture("Sampler0", draw.textureSetup.texure0(), draw.textureSetup.sampler0());
		}

		if (draw.textureSetup.texure1() != null) {
			pass.bindTexture("Sampler1", draw.textureSetup.texure1(), draw.textureSetup.sampler1());
		}

		if (draw.textureSetup.texure2() != null) {
			pass.bindTexture("Sampler2", draw.textureSetup.texure2(), draw.textureSetup.sampler2());
		}

		pass.setIndexBuffer(indexBuffer, indexType);
		pass.drawIndexed(draw.baseVertex, 0, draw.indexCount, 1);
	}

	private BufferBuilder startBuffer(RenderPipeline renderPipeline) {
		return new BufferBuilder(allocator, renderPipeline.getVertexFormatMode(), renderPipeline.getVertexFormat());
	}

	private boolean scissorChanged(@Nullable ScreenRect newScissor, @Nullable ScreenRect currentScissor) {
		if (newScissor == currentScissor) {
			return false;
		}

		return newScissor == null || !newScissor.equals(currentScissor);
	}

	private void enableScissor(ScreenRect scissor, com.mojang.blaze3d.systems.RenderPass pass) {
		Window window = MinecraftClient.getInstance().getWindow();
		int framebufferHeight = window.getFramebufferHeight();
		int scaleFactor = window.getScaleFactor();

		int x = (int) (scissor.getLeft() * scaleFactor);
		int y = (int) (framebufferHeight - scissor.getBottom() * scaleFactor);
		int width = Math.max(0, (int) (scissor.width() * scaleFactor));
		int height = Math.max(0, (int) (scissor.height() * scaleFactor));

		pass.enableScissor(x, y, width, height);
	}

	@Override
	public void close() {
		allocator.close();

		if (itemAtlasTexture != null) {
			itemAtlasTexture.close();
		}

		if (itemAtlasTextureView != null) {
			itemAtlasTextureView.close();
		}

		if (itemAtlasDepthTexture != null) {
			itemAtlasDepthTexture.close();
		}

		if (itemAtlasDepthTextureView != null) {
			itemAtlasDepthTextureView.close();
		}

		specialElementRenderers.values().forEach(SpecialGuiElementRenderer::close);
		guiProjectionMatrix.close();
		itemsProjectionMatrix.close();

		for (MappableRingBuffer ringBuffer : bufferByVertexFormat.values()) {
			ringBuffer.close();
		}

		oversizedItems.values().forEach(SpecialGuiElementRenderer::close);
	}

	// -------------------------------------------------------------------------
	// Вложенные типы
	// -------------------------------------------------------------------------

	/** Готовый draw-вызов с GPU-буфером, пайплайном и параметрами отрисовки. */
	@Environment(EnvType.CLIENT)
	record Draw(
			GpuBuffer vertexBuffer,
			int baseVertex,
			VertexFormat.DrawMode mode,
			int indexCount,
			RenderPipeline pipeline,
			TextureSetup textureSetup,
			@Nullable ScreenRect scissorArea
	) {
	}

	/** Подготовленный меш с метаданными для последующей загрузки в GPU-буфер. */
	@Environment(EnvType.CLIENT)
	record Preparation(
			BuiltBuffer mesh,
			RenderPipeline pipeline,
			TextureSetup textureSetup,
			@Nullable ScreenRect scissorArea
	) implements AutoCloseable {

		@Override
		public void close() {
			mesh.close();
		}
	}

	/** Кэш запечённого предмета в атласе: позиция в пикселях и UV-координаты. */
	@Environment(EnvType.CLIENT)
	static final class RenderedItem {

		final int x;
		final int y;
		final float u;
		final float v;
		int frame;

		RenderedItem(int x, int y, float u, float v, int frame) {
			this.x = x;
			this.y = y;
			this.u = u;
			this.v = v;
			this.frame = frame;
		}
	}
}
