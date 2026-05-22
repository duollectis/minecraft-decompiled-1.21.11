package net.minecraft.client.gui.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import net.minecraft.client.gui.render.state.special.OversizedItemGuiElementRenderState;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.item.KeyedItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Рендерер предметов, чья 3D-модель выходит за стандартные границы слота 16×16.
 * Такие предметы не могут быть запечены в атлас и рендерятся напрямую в GUI.
 * Кэширует ключ модели для оптимизации: если модель не изменилась и не анимирована,
 * повторная отрисовка в текстуру пропускается.
 */
@Environment(EnvType.CLIENT)
public class OversizedItemGuiElementRenderer extends SpecialGuiElementRenderer<OversizedItemGuiElementRenderState> {

	private static final float ITEM_CENTER_OFFSET = 8.0F;
	private static final float OVERSIZED_SCALE_DIVISOR = 16.0F;

	private boolean oversized;
	private @Nullable Object modelKey;

	public OversizedItemGuiElementRenderer(VertexConsumerProvider.Immediate immediate) {
		super(immediate);
	}

	public boolean isOversized() {
		return oversized;
	}

	public void clearOversized() {
		oversized = false;
	}

	public void clearModel() {
		modelKey = null;
	}

	@Override
	public Class<OversizedItemGuiElementRenderState> getElementClass() {
		return OversizedItemGuiElementRenderState.class;
	}

	/**
	 * Отрисовывает oversized-предмет напрямую в GUI без запекания в атлас.
	 * Вычисляет смещение так, чтобы центр модели совпадал с центром слота предмета,
	 * а не с центром oversized-области.
	 */
	@Override
	protected void render(OversizedItemGuiElementRenderState state, MatrixStack matrices) {
		matrices.scale(1.0F, -1.0F, -1.0F);

		ItemGuiElementRenderState itemState = state.guiItemRenderState();
		ScreenRect oversizedRect = itemState.oversizedBounds();
		Objects.requireNonNull(oversizedRect);

		float rectCenterX = (oversizedRect.getLeft() + oversizedRect.getRight()) / 2.0F;
		float rectCenterY = (oversizedRect.getTop() + oversizedRect.getBottom()) / 2.0F;
		float slotCenterX = itemState.x() + ITEM_CENTER_OFFSET;
		float slotCenterY = itemState.y() + ITEM_CENTER_OFFSET;

		matrices.translate(
				(slotCenterX - rectCenterX) / OVERSIZED_SCALE_DIVISOR,
				(rectCenterY - slotCenterY) / OVERSIZED_SCALE_DIVISOR,
				0.0F
		);

		KeyedItemRenderState keyedState = itemState.state();
		boolean isFlat = !keyedState.isSideLit();

		if (isFlat) {
			MinecraftClient.getInstance().gameRenderer
					.getDiffuseLighting()
					.setShaderLights(DiffuseLighting.Type.ITEMS_FLAT);
		} else {
			MinecraftClient.getInstance().gameRenderer
					.getDiffuseLighting()
					.setShaderLights(DiffuseLighting.Type.ITEMS_3D);
		}

		RenderDispatcher renderDispatcher = MinecraftClient.getInstance().gameRenderer.getEntityRenderDispatcher();
		OrderedRenderCommandQueueImpl commandQueue = renderDispatcher.getQueue();

		keyedState.render(matrices, commandQueue, 15728880, OverlayTexture.DEFAULT_UV, 0);
		renderDispatcher.render();

		modelKey = keyedState.getModelKey();
	}

	/**
	 * Помечает элемент как oversized и делегирует отрисовку базовому классу.
	 */
	public void renderElement(OversizedItemGuiElementRenderState state, GuiRenderState guiState) {
		super.renderElement(state, guiState);
		oversized = true;
	}

	/**
	 * Проверяет, можно ли пропустить повторную отрисовку в текстуру.
	 * Масштабирование обходится, если модель не анимирована и ключ модели не изменился.
	 */
	@Override
	public boolean shouldBypassScaling(OversizedItemGuiElementRenderState state) {
		KeyedItemRenderState keyedState = state.guiItemRenderState().state();
		return !keyedState.isAnimated() && keyedState.getModelKey().equals(modelKey);
	}

	@Override
	protected float getYOffset(int height, int windowScaleFactor) {
		return height / 2.0F;
	}

	@Override
	protected String getName() {
		return "oversized_item";
	}
}
