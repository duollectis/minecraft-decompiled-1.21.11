package net.minecraft.client.gui.render;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.*;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
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
import net.minecraft.client.gui.render.state.*;
import net.minecraft.client.gui.render.state.special.OversizedItemGuiElementRenderState;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.item.KeyedItemRenderState;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
/**
 * {@code GuiRenderer}.
 */
public class GuiRenderer implements AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final float FAR_PLANE = 10000.0F;
	public static final float Z_NEAR = 0.0F;
	private static final float NEAR_PLANE = 1000.0F;
	public static final int MAX_Z = 1000;
	public static final int MIN_Z = -1000;
	public static final int BUFFER_SIZE = 16;
	private static final int TEXTURE_SIZE = 512;
	private static final int MAX_TEXTURE_SIZE = RenderSystem.getDevice().getMaxTextureSize();
	public static final int INITIAL_LAYER = 0;
	private static final Comparator<ScreenRect> SCISSOR_AREA_COMPARATOR = Comparator.nullsFirst(
			Comparator
					.comparing(ScreenRect::getTop)
					.thenComparing(ScreenRect::getBottom)
					.thenComparing(ScreenRect::getLeft)
					.thenComparing(ScreenRect::getRight)
	);
	private static final Comparator<TextureSetup>
			TEXTURE_SETUP_COMPARATOR =
			Comparator.nullsFirst(Comparator.comparing(TextureSetup::getSortKey));
	private static final Comparator<SimpleGuiElementRenderState> SIMPLE_ELEMENT_COMPARATOR = Comparator.comparing(
			                                                                                                   SimpleGuiElementRenderState::scissorArea, SCISSOR_AREA_COMPARATOR
	                                                                                                   )
	                                                                                                   .thenComparing(
			                                                                                                   SimpleGuiElementRenderState::pipeline,
			                                                                                                   Comparator.comparing(
					                                                                                                   RenderPipeline::getSortKey)
	                                                                                                   )
	                                                                                                   .thenComparing(
			                                                                                                   SimpleGuiElementRenderState::textureSetup,
			                                                                                                   TEXTURE_SETUP_COMPARATOR
	                                                                                                   );
	private final Map<Object, GuiRenderer.RenderedItem> renderedItems = new Object2ObjectOpenHashMap();
	private final Map<Object, OversizedItemGuiElementRenderer> oversizedItems = new Object2ObjectOpenHashMap();
	final GuiRenderState state;
	private final List<GuiRenderer.Draw> draws = new ArrayList<>();
	private final List<GuiRenderer.Preparation> preparations = new ArrayList<>();
	private final BufferAllocator allocator = new BufferAllocator(786432);
	private final Map<VertexFormat, MappableRingBuffer> bufferByVertexFormat = new Object2ObjectOpenHashMap();
	private int blurLayer = Integer.MAX_VALUE;
	private final ProjectionMatrix2 guiProjectionMatrix = new ProjectionMatrix2("gui", 1000.0F, 11000.0F, true);
	private final ProjectionMatrix2 itemsProjectionMatrix = new ProjectionMatrix2("items", -1000.0F, 1000.0F, true);
	private final VertexConsumerProvider.Immediate vertexConsumers;
	private final OrderedRenderCommandQueue commandQueue;
	private final RenderDispatcher dispatcher;
	private final Map<Class<? extends SpecialGuiElementRenderState>, SpecialGuiElementRenderer<?>>
			specialElementRenderers;
	private @Nullable GpuTexture itemAtlasTexture;
	private @Nullable GpuTextureView itemAtlasTextureView;
	private @Nullable GpuTexture itemAtlasDepthTexture;
	private @Nullable GpuTextureView itemAtlasDepthTextureView;
	private int itemAtlasX;
	private int itemAtlasY;
	private int windowScaleFactor;
	private int frame;
	private @Nullable ScreenRect scissorArea = null;
	private @Nullable RenderPipeline pipeline = null;
	private @Nullable TextureSetup textureSetup = null;
	private @Nullable BufferBuilder buffer = null;

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
		Builder<Class<? extends SpecialGuiElementRenderState>, SpecialGuiElementRenderer<?>>
				builder =
				ImmutableMap.builder();

		for (SpecialGuiElementRenderer<?> specialGuiElementRenderer : specialElementRenderers) {
			builder.put(specialGuiElementRenderer.getElementClass(), specialGuiElementRenderer);
		}

		this.specialElementRenderers = builder.buildOrThrow();
	}

	public void incrementFrame() {
		this.frame++;
	}

	public void render(GpuBufferSlice fogBuffer) {
		this.prepare();
		this.renderPreparedDraws(fogBuffer);

		for (MappableRingBuffer mappableRingBuffer : this.bufferByVertexFormat.values()) {
			mappableRingBuffer.rotate();
		}

		this.draws.clear();
		this.preparations.clear();
		this.state.clear();
		this.blurLayer = Integer.MAX_VALUE;
		this.clearOversizedItems();
		if (SharedConstants.SHUFFLE_UI_RENDERING_ORDER) {
			RenderPipeline.updateSortKeySeed();
			TextureSetup.shuffleRenderingOrder();
		}
	}

	private void clearOversizedItems() {
		Iterator<Entry<Object, OversizedItemGuiElementRenderer>> iterator = this.oversizedItems.entrySet().iterator();

		while (iterator.hasNext()) {
			Entry<Object, OversizedItemGuiElementRenderer> entry = iterator.next();
			OversizedItemGuiElementRenderer oversizedItemGuiElementRenderer = entry.getValue();
			if (!oversizedItemGuiElementRenderer.isOversized()) {
				oversizedItemGuiElementRenderer.close();
				iterator.remove();
			}
			else {
				oversizedItemGuiElementRenderer.clearOversized();
			}
		}
	}

	private void prepare() {
		this.vertexConsumers.draw();
		this.prepareSpecialElements();
		this.prepareItemElements();
		this.prepareTextElements();
		this.state.sortSimpleElements(SIMPLE_ELEMENT_COMPARATOR);
		this.prepareSimpleElements(GuiRenderState.LayerFilter.BEFORE_BLUR);
		this.blurLayer = this.preparations.size();
		this.prepareSimpleElements(GuiRenderState.LayerFilter.AFTER_BLUR);
		this.finishPreparation();
	}

	private void prepareSimpleElements(GuiRenderState.LayerFilter filter) {
		this.scissorArea = null;
		this.pipeline = null;
		this.textureSetup = null;
		this.buffer = null;
		this.state.forEachSimpleElement(this::prepareSimpleElement, filter);
		if (this.buffer != null) {
			this.endBuffer(this.buffer, this.pipeline, this.textureSetup, this.scissorArea);
		}
	}

	private void renderPreparedDraws(GpuBufferSlice fogBuffer) {
		if (!this.draws.isEmpty()) {
			MinecraftClient minecraftClient = MinecraftClient.getInstance();
			Window window = minecraftClient.getWindow();
			RenderSystem.setProjectionMatrix(
					this.guiProjectionMatrix
							.set(
									(float) window.getFramebufferWidth() / window.getScaleFactor(),
									(float) window.getFramebufferHeight() / window.getScaleFactor()
							),
					ProjectionType.ORTHOGRAPHIC
			);
			Framebuffer framebuffer = minecraftClient.getFramebuffer();
			int i = 0;

			for (GuiRenderer.Draw draw : this.draws) {
				if (draw.indexCount > i) {
					i = draw.indexCount;
				}
			}

			RenderSystem.ShapeIndexBuffer
					shapeIndexBuffer =
					RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
			GpuBuffer gpuBuffer = shapeIndexBuffer.getIndexBuffer(i);
			VertexFormat.IndexType indexType = shapeIndexBuffer.getIndexType();
			GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
			                                            .write(
					                                            new Matrix4f().setTranslation(0.0F, 0.0F, -11000.0F),
					                                            new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
					                                            new Vector3f(),
					                                            new Matrix4f()
			                                            );
			if (this.blurLayer > 0) {
				this.render(
						() -> "GUI before blur",
						framebuffer,
						fogBuffer,
						gpuBufferSlice,
						gpuBuffer,
						indexType,
						0,
						Math.min(this.blurLayer, this.draws.size())
				);
			}

			if (this.draws.size() > this.blurLayer) {
				RenderSystem
						.getDevice()
						.createCommandEncoder()
						.clearDepthTexture(framebuffer.getDepthAttachment(), 1.0);
				minecraftClient.gameRenderer.renderBlur();
				this.render(
						() -> "GUI after blur",
						framebuffer,
						fogBuffer,
						gpuBufferSlice,
						gpuBuffer,
						indexType,
						this.blurLayer,
						this.draws.size()
				);
			}
		}
	}

	private void render(
			Supplier<String> nameSupplier,
			Framebuffer framebuffer,
			GpuBufferSlice fogBuffer,
			GpuBufferSlice dynamicTransformsBuffer,
			GpuBuffer buffer,
			VertexFormat.IndexType indexType,
			int from,
			int to
	) {
		try (RenderPass renderPass = RenderSystem.getDevice()
		                                         .createCommandEncoder()
		                                         .createRenderPass(
				                                         nameSupplier,
				                                         framebuffer.getColorAttachmentView(),
				                                         OptionalInt.empty(),
				                                         framebuffer.useDepthAttachment
				                                         ? framebuffer.getDepthAttachmentView() : null,
				                                         OptionalDouble.empty()
		                                         )
		) {
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("Fog", fogBuffer);
			renderPass.setUniform("DynamicTransforms", dynamicTransformsBuffer);

			for (int i = from; i < to; i++) {
				GuiRenderer.Draw draw = this.draws.get(i);
				this.render(draw, renderPass, buffer, indexType);
			}
		}
	}

	private void prepareSimpleElement(SimpleGuiElementRenderState state) {
		RenderPipeline renderPipeline = state.pipeline();
		TextureSetup textureSetup = state.textureSetup();
		ScreenRect screenRect = state.scissorArea();
		if (renderPipeline != this.pipeline || this.scissorChanged(screenRect, this.scissorArea)
				|| !textureSetup.equals(this.textureSetup)) {
			if (this.buffer != null) {
				this.endBuffer(this.buffer, this.pipeline, this.textureSetup, this.scissorArea);
			}

			this.buffer = this.startBuffer(renderPipeline);
			this.pipeline = renderPipeline;
			this.textureSetup = textureSetup;
			this.scissorArea = screenRect;
		}

		state.setupVertices(this.buffer);
	}

	private void prepareTextElements() {
		this.state.forEachTextElement(state -> {
			final Matrix3x2fc matrix3x2fc = state.matrix;
			final ScreenRect screenRect = state.clipBounds;
			state.prepare().draw(new TextRenderer.GlyphDrawer() {
				@Override
				public void drawGlyph(TextDrawable.DrawnGlyphRect glyph) {
					this.draw(glyph);
				}

				@Override
				public void drawRectangle(TextDrawable rect) {
					this.draw(rect);
				}

				private void draw(TextDrawable drawable) {
					GuiRenderer.this.state.addPreparedTextElement(new GlyphGuiElementRenderState(
							matrix3x2fc,
							drawable,
							screenRect
					));
				}
			});
		});
	}

	private void prepareItemElements() {
		if (!this.state.getItemModelKeys().isEmpty()) {
			int i = this.getWindowScaleFactor();
			int j = 16 * i;
			int k = this.calcItemAtlasSideLength(j);
			if (this.itemAtlasTexture == null) {
				this.createItemAtlas(k);
			}

			RenderSystem.outputColorTextureOverride = this.itemAtlasTextureView;
			RenderSystem.outputDepthTextureOverride = this.itemAtlasDepthTextureView;
			RenderSystem.setProjectionMatrix(this.itemsProjectionMatrix.set(k, k), ProjectionType.ORTHOGRAPHIC);
			MinecraftClient.getInstance().gameRenderer
					.getDiffuseLighting()
					.setShaderLights(DiffuseLighting.Type.ITEMS_3D);
			MatrixStack matrixStack = new MatrixStack();
			MutableBoolean mutableBoolean = new MutableBoolean(false);
			MutableBoolean mutableBoolean2 = new MutableBoolean(false);
			this.state
					.forEachItemElement(
							elem -> {
								if (elem.oversizedBounds() != null) {
									mutableBoolean2.setTrue();
								}
								else {
									KeyedItemRenderState keyedItemRenderState = elem.state();
									GuiRenderer.RenderedItem
											renderedItem =
											this.renderedItems.get(keyedItemRenderState.getModelKey());
									if (renderedItem == null
											|| keyedItemRenderState.isAnimated() && renderedItem.frame != this.frame) {
										if (this.itemAtlasX + j > k) {
											this.itemAtlasX = 0;
											this.itemAtlasY += j;
										}

										boolean bl = keyedItemRenderState.isAnimated() && renderedItem != null;
										if (!bl && this.itemAtlasY + j > k) {
											if (mutableBoolean.isFalse()) {
												LOGGER.warn(
														"Trying to render too many items in GUI at the same time. Skipping some of them.");
												mutableBoolean.setTrue();
											}
										}
										else {
											int kx = bl ? renderedItem.x : this.itemAtlasX;
											int l = bl ? renderedItem.y : this.itemAtlasY;
											if (bl) {
												RenderSystem.getDevice()
												            .createCommandEncoder()
												            .clearColorAndDepthTextures(
														            this.itemAtlasTexture,
														            0,
														            this.itemAtlasDepthTexture,
														            1.0,
														            kx,
														            k - l - j,
														            j,
														            j
												            );
											}

											this.prepareItemInitially(keyedItemRenderState, matrixStack, kx, l, j);
											float f = (float) kx / k;
											float g = (float) (k - l) / k;
											this.prepareItem(elem, f, g, j, k);
											if (bl) {
												renderedItem.frame = this.frame;
											}
											else {
												this.renderedItems
														.put(
																elem.state().getModelKey(),
																new GuiRenderer.RenderedItem(
																		this.itemAtlasX,
																		this.itemAtlasY,
																		f,
																		g,
																		this.frame
																)
														);
												this.itemAtlasX += j;
											}
										}
									}
									else {
										this.prepareItem(elem, renderedItem.u, renderedItem.v, j, k);
									}
								}
							}
					);
			RenderSystem.outputColorTextureOverride = null;
			RenderSystem.outputDepthTextureOverride = null;
			if (mutableBoolean2.booleanValue()) {
				this.state
						.forEachItemElement(
								elem -> {
									if (elem.oversizedBounds() != null) {
										KeyedItemRenderState keyedItemRenderState = elem.state();
										OversizedItemGuiElementRenderer
												oversizedItemGuiElementRenderer =
												this.oversizedItems
														.computeIfAbsent(
																keyedItemRenderState.getModelKey(),
																object -> new OversizedItemGuiElementRenderer(this.vertexConsumers)
														);
										ScreenRect screenRect = elem.oversizedBounds();
										OversizedItemGuiElementRenderState
												oversizedItemGuiElementRenderState =
												new OversizedItemGuiElementRenderState(
														elem,
														screenRect.getLeft(),
														screenRect.getTop(),
														screenRect.getRight(),
														screenRect.getBottom()
												);
										oversizedItemGuiElementRenderer.render(
												oversizedItemGuiElementRenderState,
												this.state,
												i
										);
									}
								}
						);
			}
		}
	}

	private void prepareSpecialElements() {
		int i = MinecraftClient.getInstance().getWindow().getScaleFactor();
		this.state.forEachSpecialElement(state -> this.prepareSpecialElement(state, i));
	}

	private <T extends SpecialGuiElementRenderState> void prepareSpecialElement(T elementState, int windowScaleFactor) {
		SpecialGuiElementRenderer<T>
				specialGuiElementRenderer =
				(SpecialGuiElementRenderer<T>) this.specialElementRenderers.get(elementState.getClass());
		if (specialGuiElementRenderer != null) {
			specialGuiElementRenderer.render(elementState, this.state, windowScaleFactor);
		}
	}

	private void prepareItemInitially(KeyedItemRenderState state, MatrixStack matrices, int x, int y, int scale) {
		matrices.push();
		matrices.translate(x + scale / 2.0F, y + scale / 2.0F, 0.0F);
		matrices.scale(scale, -scale, scale);
		boolean bl = !state.isSideLit();
		if (bl) {
			MinecraftClient.getInstance().gameRenderer
					.getDiffuseLighting()
					.setShaderLights(DiffuseLighting.Type.ITEMS_FLAT);
		}
		else {
			MinecraftClient.getInstance().gameRenderer
					.getDiffuseLighting()
					.setShaderLights(DiffuseLighting.Type.ITEMS_3D);
		}

		RenderSystem.enableScissorForRenderTypeDraws(x, this.itemAtlasTexture.getHeight(0) - y - scale, scale, scale);
		state.render(matrices, this.commandQueue, 15728880, OverlayTexture.DEFAULT_UV, 0);
		this.dispatcher.render();
		this.vertexConsumers.draw();
		RenderSystem.disableScissorForRenderTypeDraws();
		matrices.pop();
	}

	private void prepareItem(
			ItemGuiElementRenderState state,
			float u,
			float v,
			int pixelsPerItem,
			int itemAtlasSideLength
	) {
		float f = u + (float) pixelsPerItem / itemAtlasSideLength;
		float g = v + (float) (-pixelsPerItem) / itemAtlasSideLength;
		this.state
				.addSimpleElementToCurrentLayer(
						new TexturedQuadGuiElementRenderState(
								RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
								TextureSetup.of(
										this.itemAtlasTextureView,
										RenderSystem.getSamplerCache().getRepeated(FilterMode.NEAREST)
								),
								state.pose(),
								state.x(),
								state.y(),
								state.x() + 16,
								state.y() + 16,
								u,
								f,
								v,
								g,
								-1,
								state.scissorArea(),
								null
						)
				);
	}

	private void createItemAtlas(int sideLength) {
		GpuDevice gpuDevice = RenderSystem.getDevice();
		this.itemAtlasTexture =
				gpuDevice.createTexture("UI items atlas", 12, TextureFormat.RGBA8, sideLength, sideLength, 1, 1);
		this.itemAtlasTextureView = gpuDevice.createTextureView(this.itemAtlasTexture);
		this.itemAtlasDepthTexture =
				gpuDevice.createTexture("UI items atlas depth", 8, TextureFormat.DEPTH32, sideLength, sideLength, 1, 1);
		this.itemAtlasDepthTextureView = gpuDevice.createTextureView(this.itemAtlasDepthTexture);
		gpuDevice
				.createCommandEncoder()
				.clearColorAndDepthTextures(this.itemAtlasTexture, 0, this.itemAtlasDepthTexture, 1.0);
	}

	private int calcItemAtlasSideLength(int itemCount) {
		Set<Object> set = this.state.getItemModelKeys();
		int i;
		if (this.renderedItems.isEmpty()) {
			i = set.size();
		}
		else {
			i = this.renderedItems.size();

			for (Object object : set) {
				if (!this.renderedItems.containsKey(object)) {
					i++;
				}
			}
		}

		if (this.itemAtlasTexture != null) {
			int j = this.itemAtlasTexture.getWidth(0) / itemCount;
			int k = j * j;
			if (i < k) {
				return this.itemAtlasTexture.getWidth(0);
			}

			this.onItemAtlasChanged();
		}

		int j = set.size();
		int k = MathHelper.smallestEncompassingSquareSideLength(j + j / 2);
		return Math.clamp((long) MathHelper.smallestEncompassingPowerOfTwo(k * itemCount), 512, MAX_TEXTURE_SIZE);
	}

	private int getWindowScaleFactor() {
		int i = MinecraftClient.getInstance().getWindow().getScaleFactor();
		if (i != this.windowScaleFactor) {
			this.onItemAtlasChanged();

			for (OversizedItemGuiElementRenderer oversizedItemGuiElementRenderer : this.oversizedItems.values()) {
				oversizedItemGuiElementRenderer.clearModel();
			}

			this.windowScaleFactor = i;
		}

		return i;
	}

	private void onItemAtlasChanged() {
		this.itemAtlasX = 0;
		this.itemAtlasY = 0;
		this.renderedItems.clear();
		if (this.itemAtlasTexture != null) {
			this.itemAtlasTexture.close();
			this.itemAtlasTexture = null;
		}

		if (this.itemAtlasTextureView != null) {
			this.itemAtlasTextureView.close();
			this.itemAtlasTextureView = null;
		}

		if (this.itemAtlasDepthTexture != null) {
			this.itemAtlasDepthTexture.close();
			this.itemAtlasDepthTexture = null;
		}

		if (this.itemAtlasDepthTextureView != null) {
			this.itemAtlasDepthTextureView.close();
			this.itemAtlasDepthTextureView = null;
		}
	}

	private void endBuffer(
			BufferBuilder builder,
			RenderPipeline pipeline,
			TextureSetup textureSetup,
			@Nullable ScreenRect scissorArea
	) {
		BuiltBuffer builtBuffer = builder.endNullable();
		if (builtBuffer != null) {
			this.preparations.add(new GuiRenderer.Preparation(builtBuffer, pipeline, textureSetup, scissorArea));
		}
	}

	private void finishPreparation() {
		this.initVertexBuffers();
		CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
		Object2IntMap<VertexFormat> object2IntMap = new Object2IntOpenHashMap();

		for (GuiRenderer.Preparation preparation : this.preparations) {
			BuiltBuffer builtBuffer = preparation.mesh;
			BuiltBuffer.DrawParameters drawParameters = builtBuffer.getDrawParameters();
			VertexFormat vertexFormat = drawParameters.format();
			MappableRingBuffer mappableRingBuffer = this.bufferByVertexFormat.get(vertexFormat);
			if (!object2IntMap.containsKey(vertexFormat)) {
				object2IntMap.put(vertexFormat, 0);
			}

			ByteBuffer byteBuffer = builtBuffer.getBuffer();
			int i = byteBuffer.remaining();
			int j = object2IntMap.getInt(vertexFormat);

			try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(
					mappableRingBuffer
							.getBlocking()
							.slice(j, i), false, true
			)
			) {
				MemoryUtil.memCopy(byteBuffer, mappedView.data());
			}

			object2IntMap.put(vertexFormat, j + i);
			this.draws
					.add(
							new GuiRenderer.Draw(
									mappableRingBuffer.getBlocking(),
									j / vertexFormat.getVertexSize(),
									drawParameters.mode(),
									drawParameters.indexCount(),
									preparation.pipeline,
									preparation.textureSetup,
									preparation.scissorArea
							)
					);
			preparation.close();
		}
	}

	private void initVertexBuffers() {
		Object2IntMap<VertexFormat> object2IntMap = this.collectVertexSizes();
		ObjectIterator var2 = object2IntMap.object2IntEntrySet().iterator();

		while (var2.hasNext()) {
			it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<VertexFormat>
					entry =
					(it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<VertexFormat>) var2.next();
			VertexFormat vertexFormat = (VertexFormat) entry.getKey();
			int i = entry.getIntValue();
			MappableRingBuffer mappableRingBuffer = this.bufferByVertexFormat.get(vertexFormat);
			if (mappableRingBuffer == null || mappableRingBuffer.size() < i) {
				if (mappableRingBuffer != null) {
					mappableRingBuffer.close();
				}

				this.bufferByVertexFormat.put(
						vertexFormat,
						new MappableRingBuffer(() -> "GUI vertex buffer for " + vertexFormat, 34, i)
				);
			}
		}
	}

	private Object2IntMap<VertexFormat> collectVertexSizes() {
		Object2IntMap<VertexFormat> object2IntMap = new Object2IntOpenHashMap();

		for (GuiRenderer.Preparation preparation : this.preparations) {
			BuiltBuffer.DrawParameters drawParameters = preparation.mesh.getDrawParameters();
			VertexFormat vertexFormat = drawParameters.format();
			if (!object2IntMap.containsKey(vertexFormat)) {
				object2IntMap.put(vertexFormat, 0);
			}

			object2IntMap.put(
					vertexFormat,
					object2IntMap.getInt(vertexFormat) + drawParameters.vertexCount() * vertexFormat.getVertexSize()
			);
		}

		return object2IntMap;
	}

	private void render(
			GuiRenderer.Draw draw,
			RenderPass pass,
			GpuBuffer indexBuffer,
			VertexFormat.IndexType indexType
	) {
		RenderPipeline renderPipeline = draw.pipeline();
		pass.setPipeline(renderPipeline);
		pass.setVertexBuffer(0, draw.vertexBuffer);
		ScreenRect screenRect = draw.scissorArea();
		if (screenRect != null) {
			this.enableScissor(screenRect, pass);
		}
		else {
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

	private BufferBuilder startBuffer(RenderPipeline pipeline) {
		return new BufferBuilder(this.allocator, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
	}

	private boolean scissorChanged(@Nullable ScreenRect oldScissorArea, @Nullable ScreenRect newScissorArea) {
		if (oldScissorArea == newScissorArea) {
			return false;
		}
		else {
			return oldScissorArea != null ? !oldScissorArea.equals(newScissorArea) : true;
		}
	}

	private void enableScissor(ScreenRect scissorArea, RenderPass pass) {
		Window window = MinecraftClient.getInstance().getWindow();
		int i = window.getFramebufferHeight();
		int j = window.getScaleFactor();
		double d = scissorArea.getLeft() * j;
		double e = i - scissorArea.getBottom() * j;
		double f = scissorArea.width() * j;
		double g = scissorArea.height() * j;
		pass.enableScissor((int) d, (int) e, Math.max(0, (int) f), Math.max(0, (int) g));
	}

	@Override
	public void close() {
		this.allocator.close();
		if (this.itemAtlasTexture != null) {
			this.itemAtlasTexture.close();
		}

		if (this.itemAtlasTextureView != null) {
			this.itemAtlasTextureView.close();
		}

		if (this.itemAtlasDepthTexture != null) {
			this.itemAtlasDepthTexture.close();
		}

		if (this.itemAtlasDepthTextureView != null) {
			this.itemAtlasDepthTextureView.close();
		}

		this.specialElementRenderers.values().forEach(SpecialGuiElementRenderer::close);
		this.guiProjectionMatrix.close();
		this.itemsProjectionMatrix.close();

		for (MappableRingBuffer mappableRingBuffer : this.bufferByVertexFormat.values()) {
			mappableRingBuffer.close();
		}

		this.oversizedItems.values().forEach(SpecialGuiElementRenderer::close);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Draw}.
	 */
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

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Preparation}.
	 */
	record Preparation(
			BuiltBuffer mesh,
			RenderPipeline pipeline,
			TextureSetup textureSetup,
			@Nullable ScreenRect scissorArea
	) implements AutoCloseable {

		@Override
		public void close() {
			this.mesh.close();
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code RenderedItem}.
	 */
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
