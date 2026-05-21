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

import java.util.*;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code GuiRenderState}.
 */
public class GuiRenderState {

	private static final int DEFAULT_BACKGROUND_COLOR = 2000962815;
	private final List<GuiRenderState.Layer> rootLayers = new ArrayList<>();
	private int blurLayer = Integer.MAX_VALUE;
	private GuiRenderState.Layer currentLayer;
	private final Set<Object> itemModelKeys = new HashSet<>();
	private @Nullable ScreenRect currentLayerBounds;

	public GuiRenderState() {
		this.createNewRootLayer();
	}

	public void createNewRootLayer() {
		this.currentLayer = new GuiRenderState.Layer(null);
		this.rootLayers.add(this.currentLayer);
	}

	public void applyBlur() {
		if (this.blurLayer != Integer.MAX_VALUE) {
			throw new IllegalStateException("Can only blur once per frame");
		}
		else {
			this.blurLayer = this.rootLayers.size() - 1;
		}
	}

	public void goUpLayer() {
		if (this.currentLayer.up == null) {
			this.currentLayer.up = new GuiRenderState.Layer(this.currentLayer);
		}

		this.currentLayer = this.currentLayer.up;
	}

	public void addItem(ItemGuiElementRenderState state) {
		if (this.findAndGoToLayerToAdd(state)) {
			this.itemModelKeys.add(state.state().getModelKey());
			this.currentLayer.addItem(state);
			this.onElementAdded(state.bounds());
		}
	}

	public void addText(TextGuiElementRenderState state) {
		if (this.findAndGoToLayerToAdd(state)) {
			this.currentLayer.addText(state);
			this.onElementAdded(state.bounds());
		}
	}

	public void addSpecialElement(SpecialGuiElementRenderState state) {
		if (this.findAndGoToLayerToAdd(state)) {
			this.currentLayer.addSpecialElement(state);
			this.onElementAdded(state.bounds());
		}
	}

	public void addSimpleElement(SimpleGuiElementRenderState state) {
		if (this.findAndGoToLayerToAdd(state)) {
			this.currentLayer.addSimpleElement(state);
			this.onElementAdded(state.bounds());
		}
	}

	private void onElementAdded(@Nullable ScreenRect bounds) {
		if (SharedConstants.RENDER_UI_LAYERING_RECTANGLES && bounds != null) {
			this.goUpLayer();
			this.currentLayer
					.addSimpleElement(
							new ColoredQuadGuiElementRenderState(
									RenderPipelines.GUI,
									TextureSetup.empty(),
									new Matrix3x2f(),
									0,
									0,
									10000,
									10000,
									2000962815,
									2000962815,
									bounds
							)
					);
		}
	}

	private boolean findAndGoToLayerToAdd(GuiElementRenderState state) {
		ScreenRect screenRect = state.bounds();
		if (screenRect == null) {
			return false;
		}
		else {
			if (this.currentLayerBounds != null && this.currentLayerBounds.contains(screenRect)) {
				this.goUpLayer();
			}
			else {
				this.findAndGoToLayerIntersecting(screenRect);
			}

			this.currentLayerBounds = screenRect;
			return true;
		}
	}

	private void findAndGoToLayerIntersecting(ScreenRect bounds) {
		GuiRenderState.Layer layer = this.rootLayers.getLast();

		while (layer.up != null) {
			layer = layer.up;
		}

		boolean bl = false;

		while (!bl) {
			bl = this.anyIntersect(bounds, layer.simpleElementRenderStates)
					|| this.anyIntersect(bounds, layer.itemElementRenderStates)
					|| this.anyIntersect(bounds, layer.textElementRenderStates)
					|| this.anyIntersect(bounds, layer.specialElementRenderStates);
			if (layer.parent == null) {
				break;
			}

			if (!bl) {
				layer = layer.parent;
			}
		}

		this.currentLayer = layer;
		if (bl) {
			this.goUpLayer();
		}
	}

	private boolean anyIntersect(
			ScreenRect bounds,
			@Nullable List<? extends GuiElementRenderState> elementRenderStates
	) {
		if (elementRenderStates != null) {
			for (GuiElementRenderState guiElementRenderState : elementRenderStates) {
				ScreenRect screenRect = guiElementRenderState.bounds();
				if (screenRect != null && screenRect.intersects(bounds)) {
					return true;
				}
			}
		}

		return false;
	}

	public void addSimpleElementToCurrentLayer(TexturedQuadGuiElementRenderState state) {
		this.currentLayer.addSimpleElement(state);
	}

	public void addPreparedTextElement(SimpleGuiElementRenderState state) {
		this.currentLayer.addPreparedText(state);
	}

	public Set<Object> getItemModelKeys() {
		return this.itemModelKeys;
	}

	public void forEachSimpleElement(
			Consumer<SimpleGuiElementRenderState> consumer,
			GuiRenderState.LayerFilter filter
	) {
		this.forEachLayer(
				layer -> {
					if (layer.simpleElementRenderStates != null || layer.preparedTextElementRenderStates != null) {
						if (layer.simpleElementRenderStates != null) {
							for (SimpleGuiElementRenderState simpleGuiElementRenderState : layer.simpleElementRenderStates) {
								consumer.accept(simpleGuiElementRenderState);
							}
						}

						if (layer.preparedTextElementRenderStates != null) {
							for (SimpleGuiElementRenderState simpleGuiElementRenderState : layer.preparedTextElementRenderStates) {
								consumer.accept(simpleGuiElementRenderState);
							}
						}
					}
				}, filter
		);
	}

	public void forEachItemElement(Consumer<ItemGuiElementRenderState> itemElementStateConsumer) {
		GuiRenderState.Layer layer = this.currentLayer;
		this.forEachLayer(
				layerx -> {
					if (layerx.itemElementRenderStates != null) {
						this.currentLayer = layerx;

						for (ItemGuiElementRenderState itemGuiElementRenderState : layerx.itemElementRenderStates) {
							itemElementStateConsumer.accept(itemGuiElementRenderState);
						}
					}
				}, GuiRenderState.LayerFilter.ALL
		);
		this.currentLayer = layer;
	}

	public void forEachTextElement(Consumer<TextGuiElementRenderState> textElementStateConsumer) {
		GuiRenderState.Layer layer = this.currentLayer;
		this.forEachLayer(
				layerx -> {
					if (layerx.textElementRenderStates != null) {
						for (TextGuiElementRenderState textGuiElementRenderState : layerx.textElementRenderStates) {
							this.currentLayer = layerx;
							textElementStateConsumer.accept(textGuiElementRenderState);
						}
					}
				}, GuiRenderState.LayerFilter.ALL
		);
		this.currentLayer = layer;
	}

	public void forEachSpecialElement(Consumer<SpecialGuiElementRenderState> specialElementStateConsumer) {
		GuiRenderState.Layer layer = this.currentLayer;
		this.forEachLayer(
				layerx -> {
					if (layerx.specialElementRenderStates != null) {
						this.currentLayer = layerx;

						for (SpecialGuiElementRenderState specialGuiElementRenderState : layerx.specialElementRenderStates) {
							specialElementStateConsumer.accept(specialGuiElementRenderState);
						}
					}
				}, GuiRenderState.LayerFilter.ALL
		);
		this.currentLayer = layer;
	}

	public void sortSimpleElements(Comparator<SimpleGuiElementRenderState> simpleElementStateComparator) {
		this.forEachLayer(
				layer -> {
					if (layer.simpleElementRenderStates != null) {
						if (SharedConstants.SHUFFLE_UI_RENDERING_ORDER) {
							Collections.shuffle(layer.simpleElementRenderStates);
						}

						layer.simpleElementRenderStates.sort(simpleElementStateComparator);
					}
				}, GuiRenderState.LayerFilter.ALL
		);
	}

	private void forEachLayer(Consumer<GuiRenderState.Layer> layerConsumer, GuiRenderState.LayerFilter filter) {
		int i = 0;
		int j = this.rootLayers.size();
		if (filter == GuiRenderState.LayerFilter.BEFORE_BLUR) {
			j = Math.min(this.blurLayer, this.rootLayers.size());
		}
		else if (filter == GuiRenderState.LayerFilter.AFTER_BLUR) {
			i = this.blurLayer;
		}

		for (int k = i; k < j; k++) {
			GuiRenderState.Layer layer = this.rootLayers.get(k);
			this.traverseLayers(layer, layerConsumer);
		}
	}

	private void traverseLayers(GuiRenderState.Layer layer, Consumer<GuiRenderState.Layer> layerConsumer) {
		layerConsumer.accept(layer);
		if (layer.up != null) {
			this.traverseLayers(layer.up, layerConsumer);
		}
	}

	public void clear() {
		this.itemModelKeys.clear();
		this.rootLayers.clear();
		this.blurLayer = Integer.MAX_VALUE;
		this.createNewRootLayer();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Layer}.
	 */
	static class Layer {

		public final GuiRenderState.@Nullable Layer parent;
		public GuiRenderState.@Nullable Layer up;
		public @Nullable List<SimpleGuiElementRenderState> simpleElementRenderStates;
		public @Nullable List<SimpleGuiElementRenderState> preparedTextElementRenderStates;
		public @Nullable List<ItemGuiElementRenderState> itemElementRenderStates;
		public @Nullable List<TextGuiElementRenderState> textElementRenderStates;
		public @Nullable List<SpecialGuiElementRenderState> specialElementRenderStates;

		Layer(GuiRenderState.@Nullable Layer parent) {
			this.parent = parent;
		}

		public void addItem(ItemGuiElementRenderState state) {
			if (this.itemElementRenderStates == null) {
				this.itemElementRenderStates = new ArrayList<>();
			}

			this.itemElementRenderStates.add(state);
		}

		public void addText(TextGuiElementRenderState state) {
			if (this.textElementRenderStates == null) {
				this.textElementRenderStates = new ArrayList<>();
			}

			this.textElementRenderStates.add(state);
		}

		public void addSpecialElement(SpecialGuiElementRenderState state) {
			if (this.specialElementRenderStates == null) {
				this.specialElementRenderStates = new ArrayList<>();
			}

			this.specialElementRenderStates.add(state);
		}

		public void addSimpleElement(SimpleGuiElementRenderState state) {
			if (this.simpleElementRenderStates == null) {
				this.simpleElementRenderStates = new ArrayList<>();
			}

			this.simpleElementRenderStates.add(state);
		}

		public void addPreparedText(SimpleGuiElementRenderState state) {
			if (this.preparedTextElementRenderStates == null) {
				this.preparedTextElementRenderStates = new ArrayList<>();
			}

			this.preparedTextElementRenderStates.add(state);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code LayerFilter}.
	 */
	public static enum LayerFilter {
		ALL,
		BEFORE_BLUR,
		AFTER_BLUR;
	}
}
