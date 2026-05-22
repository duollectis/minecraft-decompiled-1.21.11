package net.minecraft.client.render.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.renderer.v1.render.FabricLayerRenderState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Мутабельный контейнер состояния рендеринга предмета, накапливающий слои отрисовки
 * ({@link LayerRenderState}) перед финальным проходом GPU.
 * <p>
 * Каждый слой соответствует одному визуальному компоненту предмета: базовой геометрии,
 * специальной модели, блеску (glint) и т.д. Состояние сбрасывается через {@link #clear()}
 * перед каждым кадром.
 */
@Environment(EnvType.CLIENT)
public class ItemRenderState implements FabricRenderState {

	public ItemDisplayContext displayContext = ItemDisplayContext.NONE;
	private int layerCount;
	private boolean animated;
	private boolean oversizedInGui;
	private @Nullable Box cachedModelBoundingBox;
	private ItemRenderState.LayerRenderState[]
			layers =
			new ItemRenderState.LayerRenderState[]{new ItemRenderState.LayerRenderState()};

	/**
	 * Резервирует место для {@code add} дополнительных слоёв, расширяя внутренний массив
	 * при необходимости. Вызывается перед {@link #newLayer()} для batch-аллокации.
	 *
	 * @param add количество слоёв, которые планируется добавить
	 */
	public void addLayers(int add) {
		int currentCapacity = layers.length;
		int required = layerCount + add;

		if (required > currentCapacity) {
			layers = Arrays.copyOf(layers, required);

			for (int idx = currentCapacity; idx < required; idx++) {
				layers[idx] = new ItemRenderState.LayerRenderState();
			}
		}
	}

	public ItemRenderState.LayerRenderState newLayer() {
		addLayers(1);
		return layers[layerCount++];
	}

	public void clear() {
		displayContext = ItemDisplayContext.NONE;

		for (int idx = 0; idx < layerCount; idx++) {
			layers[idx].clear();
		}

		layerCount = 0;
		animated = false;
		oversizedInGui = false;
		cachedModelBoundingBox = null;
	}

	public void markAnimated() {
		animated = true;
	}

	public boolean isAnimated() {
		return animated;
	}

	public void addModelKey(Object modelKey) {
	}

	private ItemRenderState.LayerRenderState getFirstLayer() {
		return layers[0];
	}

	public boolean isEmpty() {
		return layerCount == 0;
	}

	public boolean isSideLit() {
		return getFirstLayer().useLight;
	}

	public @Nullable Sprite getParticleSprite(Random random) {
		return layerCount == 0 ? null : layers[random.nextInt(layerCount)].particle;
	}

	/**
	 * Обходит все вершины всех слоёв с учётом трансформации контекста отображения
	 * и передаёт их в {@code posConsumer}. Используется для вычисления AABB модели.
	 *
	 * @param posConsumer получатель позиций вершин в мировом пространстве
	 */
	public void load(Consumer<Vector3fc> posConsumer) {
		Vector3f scratch = new Vector3f();
		MatrixStack.Entry entry = new MatrixStack.Entry();

		for (int idx = 0; idx < layerCount; idx++) {
			ItemRenderState.LayerRenderState layer = layers[idx];
			layer.transform.apply(displayContext.isLeftHand(), entry);
			Matrix4f matrix = entry.getPositionMatrix();
			Vector3fc[] vertices = layer.vertices.get();

			for (Vector3fc vertex : vertices) {
				posConsumer.accept(scratch.set(vertex).mulPosition(matrix));
			}

			entry.loadIdentity();
		}
	}

	public void render(
			MatrixStack matrices,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int light,
			int overlay,
			int seed
	) {
		for (int idx = 0; idx < layerCount; idx++) {
			layers[idx].render(matrices, orderedRenderCommandQueue, light, overlay, seed);
		}
	}

	public Box getModelBoundingBox() {
		if (cachedModelBoundingBox != null) {
			return cachedModelBoundingBox;
		}

		Box.Builder builder = new Box.Builder();
		load(builder::encompass);
		Box box = builder.build();
		cachedModelBoundingBox = box;
		return box;
	}

	public void setOversizedInGui(boolean oversizedInGui) {
		this.oversizedInGui = oversizedInGui;
	}

	public boolean isOversizedInGui() {
		return oversizedInGui;
	}

	/** Тип блеска (glint) предмета: отсутствует, стандартный или специальный (компас/часы). */
	@Environment(EnvType.CLIENT)
	public enum Glint {
		NONE,
		STANDARD,
		SPECIAL;
	}

	/**
	 * Состояние одного визуального слоя предмета: геометрия, текстурный слой,
	 * тинты, блеск и опциональная специальная модель.
	 */
	@Environment(EnvType.CLIENT)
	public class LayerRenderState implements FabricLayerRenderState, FabricRenderState {

		private static final Vector3fc[] EMPTY = new Vector3fc[0];
		public static final Supplier<Vector3fc[]> DEFAULT = () -> EMPTY;
		private final List<BakedQuad> quads = new ArrayList<>();
		boolean useLight;
		@Nullable Sprite particle;
		Transformation transform = Transformation.IDENTITY;
		private @Nullable RenderLayer renderLayer;
		private ItemRenderState.Glint glint = ItemRenderState.Glint.NONE;
		private int[] tints = new int[0];
		private @Nullable SpecialModelRenderer<Object> specialModelType;
		private @Nullable Object data;
		Supplier<Vector3fc[]> vertices = DEFAULT;

		public void clear() {
			quads.clear();
			renderLayer = null;
			glint = ItemRenderState.Glint.NONE;
			specialModelType = null;
			data = null;
			Arrays.fill(tints, -1);
			useLight = false;
			particle = null;
			transform = Transformation.IDENTITY;
			vertices = DEFAULT;
		}

		public List<BakedQuad> getQuads() {
			return quads;
		}

		public void setRenderLayer(RenderLayer layer) {
			renderLayer = layer;
		}

		public void setUseLight(boolean useLight) {
			this.useLight = useLight;
		}

		public void setVertices(Supplier<Vector3fc[]> vertices) {
			this.vertices = vertices;
		}

		public void setParticle(Sprite particle) {
			this.particle = particle;
		}

		public void setTransform(Transformation transform) {
			this.transform = transform;
		}

		public <T> void setSpecialModel(SpecialModelRenderer<T> specialModelType, @Nullable T data) {
			this.specialModelType = eraseType(specialModelType);
			this.data = data;
		}

		private static SpecialModelRenderer<Object> eraseType(SpecialModelRenderer<?> specialModelType) {
			return (SpecialModelRenderer<Object>) specialModelType;
		}

		public void setGlint(ItemRenderState.Glint glint) {
			this.glint = glint;
		}

		/**
		 * Инициализирует или расширяет массив тинтов до {@code maxIndex} элементов,
		 * заполняя новые позиции значением {@code -1} (нет тинта).
		 *
		 * @param maxIndex требуемый размер массива тинтов
		 * @return массив тинтов для заполнения вызывающим кодом
		 */
		public int[] initTints(int maxIndex) {
			if (maxIndex > tints.length) {
				tints = new int[maxIndex];
				Arrays.fill(tints, -1);
			}

			return tints;
		}

		void render(
				MatrixStack matrices,
				OrderedRenderCommandQueue orderedRenderCommandQueue,
				int light,
				int overlay,
				int seed
		) {
			matrices.push();
			transform.apply(ItemRenderState.this.displayContext.isLeftHand(), matrices.peek());

			if (specialModelType != null) {
				specialModelType.render(
						data,
						ItemRenderState.this.displayContext,
						matrices,
						orderedRenderCommandQueue,
						light,
						overlay,
						glint != ItemRenderState.Glint.NONE,
						seed
				);
			}
			else if (renderLayer != null) {
				orderedRenderCommandQueue.submitItem(
						matrices,
						ItemRenderState.this.displayContext,
						light,
						overlay,
						seed,
						tints,
						quads,
						renderLayer,
						glint
				);
			}

			matrices.pop();
		}
	}
}
