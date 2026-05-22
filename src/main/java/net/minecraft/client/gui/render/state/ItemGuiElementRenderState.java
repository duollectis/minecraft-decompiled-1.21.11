package net.minecraft.client.gui.render.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.item.KeyedItemRenderState;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * Состояние предмета в GUI-слоте.
 * При создании вычисляет {@code oversizedBounds} — область, выходящую за стандартные
 * границы слота 16×16, если модель предмета является oversized.
 * Ограничивающий прямоугольник {@code bounds} учитывает матрицу трансформации и scissor.
 */
@Environment(EnvType.CLIENT)
public final class ItemGuiElementRenderState implements GuiElementRenderState {

	private static final int SLOT_SIZE = 16;
	private static final float SLOT_CENTER_OFFSET = 8.0F;

	private final String name;
	private final Matrix3x2f pose;
	private final KeyedItemRenderState state;
	private final int x;
	private final int y;
	private final @Nullable ScreenRect scissorArea;
	private final @Nullable ScreenRect oversizedBounds;
	private final @Nullable ScreenRect bounds;

	public ItemGuiElementRenderState(
			String name,
			Matrix3x2f pose,
			KeyedItemRenderState state,
			int x,
			int y,
			@Nullable ScreenRect scissor
	) {
		this.name = name;
		this.pose = pose;
		this.state = state;
		this.x = x;
		this.y = y;
		this.scissorArea = scissor;
		this.oversizedBounds = state.isOversizedInGui() ? createOversizedBounds() : null;
		this.bounds = createBounds(
				oversizedBounds != null ? oversizedBounds : new ScreenRect(x, y, SLOT_SIZE, SLOT_SIZE)
		);
	}

	/**
	 * Вычисляет область, выходящую за стандартные границы слота, на основе
	 * bounding box модели предмета. Возвращает {@code null}, если модель
	 * вписывается в стандартный слот 16×16.
	 */
	private @Nullable ScreenRect createOversizedBounds() {
		Box box = state.getModelBoundingBox();
		int width = MathHelper.ceil(box.getLengthX() * SLOT_SIZE);
		int height = MathHelper.ceil(box.getLengthY() * SLOT_SIZE);

		if (width <= SLOT_SIZE && height <= SLOT_SIZE) {
			return null;
		}

		float minX = (float) (box.minX * SLOT_SIZE);
		float maxY = (float) (box.maxY * SLOT_SIZE);
		int left = x + MathHelper.floor(minX) + (int) SLOT_CENTER_OFFSET;
		int top = y - MathHelper.floor(maxY) + (int) SLOT_CENTER_OFFSET;

		return new ScreenRect(left, top, width, height);
	}

	private @Nullable ScreenRect createBounds(ScreenRect rect) {
		ScreenRect transformed = rect.transformEachVertex(pose);
		return scissorArea != null ? scissorArea.intersection(transformed) : transformed;
	}

	public String name() {
		return name;
	}

	public Matrix3x2f pose() {
		return pose;
	}

	public KeyedItemRenderState state() {
		return state;
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	public @Nullable ScreenRect scissorArea() {
		return scissorArea;
	}

	public @Nullable ScreenRect oversizedBounds() {
		return oversizedBounds;
	}

	@Override
	public @Nullable ScreenRect bounds() {
		return bounds;
	}
}
