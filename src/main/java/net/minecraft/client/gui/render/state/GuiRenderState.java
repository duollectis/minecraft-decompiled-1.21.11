package net.minecraft.client.gui.render.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Центральное хранилище состояния GUI-рендера для одного кадра.
 * Организует элементы в дерево слоёв ({@link Layer}), где каждый слой содержит
 * элементы, которые не пересекаются между собой. Пересекающиеся элементы
 * автоматически помещаются в дочерний слой, обеспечивая корректный z-order.
 *
 * <p>Поддерживает разделение на зоны до и после blur-эффекта через {@link #applyBlur()}.
 */
@Environment(EnvType.CLIENT)
public class GuiRenderState {

	/** Цвет отладочного фона для визуализации слоёв GUI (полупрозрачный синий). */
	private static final int DEBUG_LAYER_BACKGROUND_COLOR = 2000962815;

	private final List<Layer> rootLayers = new ArrayList<>();
	private int blurLayer = Integer.MAX_VALUE;
	private Layer currentLayer;
	private final Set<Object> itemModelKeys = new HashSet<>();
	private @Nullable ScreenRect currentLayerBounds;

	public GuiRenderState() {
		createNewRootLayer();
	}

	public void createNewRootLayer() {
		currentLayer = new Layer(null);
		rootLayers.add(currentLayer);
	}

	/**
	 * Фиксирует текущую позицию как границу blur-эффекта.
	 * Все элементы до этой точки рендерятся до blur, после — поверх него.
	 *
	 * @throws IllegalStateException если blur уже был применён в этом кадре
	 */
	public void applyBlur() {
		if (blurLayer != Integer.MAX_VALUE) {
			throw new IllegalStateException("Can only blur once per frame");
		}

		blurLayer = rootLayers.size() - 1;
	}

	public void goUpLayer() {
		if (currentLayer.up == null) {
			currentLayer.up = new Layer(currentLayer);
		}

		currentLayer = currentLayer.up;
	}

	public void addItem(ItemGuiElementRenderState state) {
		if (!findAndGoToLayerToAdd(state)) {
			return;
		}

		itemModelKeys.add(state.state().getModelKey());
		currentLayer.addItem(state);
		onElementAdded(state.bounds());
	}

	public void addText(TextGuiElementRenderState state) {
		if (!findAndGoToLayerToAdd(state)) {
			return;
		}

		currentLayer.addText(state);
		onElementAdded(state.bounds());
	}

	public void addSpecialElement(SpecialGuiElementRenderState state) {
		if (!findAndGoToLayerToAdd(state)) {
			return;
		}

		currentLayer.addSpecialElement(state);
		onElementAdded(state.bounds());
	}

	public void addSimpleElement(SimpleGuiElementRenderState state) {
		if (!findAndGoToLayerToAdd(state)) {
			return;
		}

		currentLayer.addSimpleElement(state);
		onElementAdded(state.bounds());
	}

	/**
	 * В режиме отладки слоёв добавляет полупрозрачный фон поверх каждого элемента,
	 * чтобы визуально показать границы слоёв GUI.
	 */
	private void onElementAdded(@Nullable ScreenRect bounds) {
		if (!SharedConstants.RENDER_UI_LAYERING_RECTANGLES || bounds == null) {
			return;
		}

		goUpLayer();
		currentLayer.addSimpleElement(
				new ColoredQuadGuiElementRenderState(
						RenderPipelines.GUI,
						TextureSetup.empty(),
						new Matrix3x2f(),
						0,
						0,
						10000,
						10000,
						DEBUG_LAYER_BACKGROUND_COLOR,
						DEBUG_LAYER_BACKGROUND_COLOR,
						bounds
				)
		);
	}

	/**
	 * Находит подходящий слой для нового элемента и переходит в него.
	 * Если элемент не имеет bounds — пропускается.
	 * Если текущий слой содержит элемент, который полностью содержит новый — переходим вверх.
	 * Иначе ищем слой с пересечением и переходим на уровень выше него.
	 *
	 * @return {@code true}, если элемент нужно добавить; {@code false} — если bounds == null
	 */
	private boolean findAndGoToLayerToAdd(GuiElementRenderState state) {
		ScreenRect bounds = state.bounds();
		if (bounds == null) {
			return false;
		}

		if (currentLayerBounds != null && currentLayerBounds.contains(bounds)) {
			goUpLayer();
		} else {
			findAndGoToLayerIntersecting(bounds);
		}

		currentLayerBounds = bounds;
		return true;
	}

	/**
	 * Обходит дерево слоёв снизу вверх, ища слой, элементы которого пересекаются
	 * с переданными bounds. Если пересечение найдено — переходит на уровень выше.
	 */
	private void findAndGoToLayerIntersecting(ScreenRect bounds) {
		Layer layer = rootLayers.getLast();

		while (layer.up != null) {
			layer = layer.up;
		}

		boolean hasIntersection = false;

		while (!hasIntersection) {
			hasIntersection = anyIntersect(bounds, layer.simpleElementRenderStates)
					|| anyIntersect(bounds, layer.itemElementRenderStates)
					|| anyIntersect(bounds, layer.textElementRenderStates)
					|| anyIntersect(bounds, layer.specialElementRenderStates);

			if (layer.parent == null) {
				break;
			}

			if (!hasIntersection) {
				layer = layer.parent;
			}
		}

		currentLayer = layer;

		if (hasIntersection) {
			goUpLayer();
		}
	}

	private boolean anyIntersect(ScreenRect bounds, @Nullable List<? extends GuiElementRenderState> elements) {
		if (elements == null) {
			return false;
		}

		for (GuiElementRenderState element : elements) {
			ScreenRect elementBounds = element.bounds();
			if (elementBounds != null && elementBounds.intersects(bounds)) {
				return true;
			}
		}

		return false;
	}

	public void addSimpleElementToCurrentLayer(TexturedQuadGuiElementRenderState state) {
		currentLayer.addSimpleElement(state);
	}

	public void addPreparedTextElement(SimpleGuiElementRenderState state) {
		currentLayer.addPreparedText(state);
	}

	public Set<Object> getItemModelKeys() {
		return itemModelKeys;
	}

	public void forEachSimpleElement(Consumer<SimpleGuiElementRenderState> consumer, LayerFilter filter) {
		forEachLayer(
				layer -> {
					if (layer.simpleElementRenderStates == null && layer.preparedTextElementRenderStates == null) {
						return;
					}

					if (layer.simpleElementRenderStates != null) {
						for (SimpleGuiElementRenderState element : layer.simpleElementRenderStates) {
							consumer.accept(element);
						}
					}

					if (layer.preparedTextElementRenderStates != null) {
						for (SimpleGuiElementRenderState element : layer.preparedTextElementRenderStates) {
							consumer.accept(element);
						}
					}
				},
				filter
		);
	}

	/**
	 * Итерирует все элементы-предметы по всем слоям, временно переключая
	 * {@code currentLayer} на обрабатываемый слой для корректной работы
	 * методов добавления элементов внутри колбэка.
	 */
	public void forEachItemElement(Consumer<ItemGuiElementRenderState> consumer) {
		Layer savedLayer = currentLayer;

		forEachLayer(
				layer -> {
					if (layer.itemElementRenderStates == null) {
						return;
					}

					currentLayer = layer;

					for (ItemGuiElementRenderState element : layer.itemElementRenderStates) {
						consumer.accept(element);
					}
				},
				LayerFilter.ALL
		);

		currentLayer = savedLayer;
	}

	/**
	 * Итерирует все текстовые элементы по всем слоям, временно переключая
	 * {@code currentLayer} для корректной работы методов добавления глифов.
	 */
	public void forEachTextElement(Consumer<TextGuiElementRenderState> consumer) {
		Layer savedLayer = currentLayer;

		forEachLayer(
				layer -> {
					if (layer.textElementRenderStates == null) {
						return;
					}

					for (TextGuiElementRenderState element : layer.textElementRenderStates) {
						currentLayer = layer;
						consumer.accept(element);
					}
				},
				LayerFilter.ALL
		);

		currentLayer = savedLayer;
	}

	/**
	 * Итерирует все специальные элементы по всем слоям, временно переключая
	 * {@code currentLayer} для корректной работы методов добавления PIP-квадов.
	 */
	public void forEachSpecialElement(Consumer<SpecialGuiElementRenderState> consumer) {
		Layer savedLayer = currentLayer;

		forEachLayer(
				layer -> {
					if (layer.specialElementRenderStates == null) {
						return;
					}

					currentLayer = layer;

					for (SpecialGuiElementRenderState element : layer.specialElementRenderStates) {
						consumer.accept(element);
					}
				},
				LayerFilter.ALL
		);

		currentLayer = savedLayer;
	}

	public void sortSimpleElements(Comparator<SimpleGuiElementRenderState> comparator) {
		forEachLayer(
				layer -> {
					if (layer.simpleElementRenderStates == null) {
						return;
					}

					if (SharedConstants.SHUFFLE_UI_RENDERING_ORDER) {
						Collections.shuffle(layer.simpleElementRenderStates);
					}

					layer.simpleElementRenderStates.sort(comparator);
				},
				LayerFilter.ALL
		);
	}

	private void forEachLayer(Consumer<Layer> layerConsumer, LayerFilter filter) {
		int from = 0;
		int to = rootLayers.size();

		if (filter == LayerFilter.BEFORE_BLUR) {
			to = Math.min(blurLayer, rootLayers.size());
		} else if (filter == LayerFilter.AFTER_BLUR) {
			from = blurLayer;
		}

		for (int index = from; index < to; index++) {
			traverseLayers(rootLayers.get(index), layerConsumer);
		}
	}

	private void traverseLayers(Layer layer, Consumer<Layer> layerConsumer) {
		layerConsumer.accept(layer);

		if (layer.up != null) {
			traverseLayers(layer.up, layerConsumer);
		}
	}

	public void clear() {
		itemModelKeys.clear();
		rootLayers.clear();
		blurLayer = Integer.MAX_VALUE;
		currentLayerBounds = null;
		createNewRootLayer();
	}

	// -------------------------------------------------------------------------
	// Вложенные типы
	// -------------------------------------------------------------------------

	/**
	 * Один слой дерева GUI-рендера. Хранит списки элементов разных типов,
	 * инициализируемые лениво при первом добавлении элемента.
	 */
	@Environment(EnvType.CLIENT)
	static class Layer {

		public final @Nullable Layer parent;
		public @Nullable Layer up;
		public @Nullable List<SimpleGuiElementRenderState> simpleElementRenderStates;
		public @Nullable List<SimpleGuiElementRenderState> preparedTextElementRenderStates;
		public @Nullable List<ItemGuiElementRenderState> itemElementRenderStates;
		public @Nullable List<TextGuiElementRenderState> textElementRenderStates;
		public @Nullable List<SpecialGuiElementRenderState> specialElementRenderStates;

		Layer(@Nullable Layer parent) {
			this.parent = parent;
		}

		public void addItem(ItemGuiElementRenderState state) {
			if (itemElementRenderStates == null) {
				itemElementRenderStates = new ArrayList<>();
			}

			itemElementRenderStates.add(state);
		}

		public void addText(TextGuiElementRenderState state) {
			if (textElementRenderStates == null) {
				textElementRenderStates = new ArrayList<>();
			}

			textElementRenderStates.add(state);
		}

		public void addSpecialElement(SpecialGuiElementRenderState state) {
			if (specialElementRenderStates == null) {
				specialElementRenderStates = new ArrayList<>();
			}

			specialElementRenderStates.add(state);
		}

		public void addSimpleElement(SimpleGuiElementRenderState state) {
			if (simpleElementRenderStates == null) {
				simpleElementRenderStates = new ArrayList<>();
			}

			simpleElementRenderStates.add(state);
		}

		public void addPreparedText(SimpleGuiElementRenderState state) {
			if (preparedTextElementRenderStates == null) {
				preparedTextElementRenderStates = new ArrayList<>();
			}

			preparedTextElementRenderStates.add(state);
		}
	}

	/** Фильтр слоёв относительно позиции blur-эффекта. */
	@Environment(EnvType.CLIENT)
	public enum LayerFilter {
		ALL,
		BEFORE_BLUR,
		AFTER_BLUR
	}
}
