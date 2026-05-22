package net.minecraft.client.gui.screen.advancement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * Тип вкладки достижений — определяет расположение (сверху, снизу, слева, справа),
 * размеры, количество вкладок и смещения иконок для каждой стороны экрана достижений.
 */
@Environment(EnvType.CLIENT)
enum AdvancementTabType {
	ABOVE(
		new Textures(
			Identifier.ofVanilla("advancements/tab_above_left_selected"),
			Identifier.ofVanilla("advancements/tab_above_middle_selected"),
			Identifier.ofVanilla("advancements/tab_above_right_selected")
		),
		new Textures(
			Identifier.ofVanilla("advancements/tab_above_left"),
			Identifier.ofVanilla("advancements/tab_above_middle"),
			Identifier.ofVanilla("advancements/tab_above_right")
		),
		28,
		32,
		8
	),
	BELOW(
		new Textures(
			Identifier.ofVanilla("advancements/tab_below_left_selected"),
			Identifier.ofVanilla("advancements/tab_below_middle_selected"),
			Identifier.ofVanilla("advancements/tab_below_right_selected")
		),
		new Textures(
			Identifier.ofVanilla("advancements/tab_below_left"),
			Identifier.ofVanilla("advancements/tab_below_middle"),
			Identifier.ofVanilla("advancements/tab_below_right")
		),
		28,
		32,
		8
	),
	LEFT(
		new Textures(
			Identifier.ofVanilla("advancements/tab_left_top_selected"),
			Identifier.ofVanilla("advancements/tab_left_middle_selected"),
			Identifier.ofVanilla("advancements/tab_left_bottom_selected")
		),
		new Textures(
			Identifier.ofVanilla("advancements/tab_left_top"),
			Identifier.ofVanilla("advancements/tab_left_middle"),
			Identifier.ofVanilla("advancements/tab_left_bottom")
		),
		32,
		28,
		5
	),
	RIGHT(
		new Textures(
			Identifier.ofVanilla("advancements/tab_right_top_selected"),
			Identifier.ofVanilla("advancements/tab_right_middle_selected"),
			Identifier.ofVanilla("advancements/tab_right_bottom_selected")
		),
		new Textures(
			Identifier.ofVanilla("advancements/tab_right_top"),
			Identifier.ofVanilla("advancements/tab_right_middle"),
			Identifier.ofVanilla("advancements/tab_right_bottom")
		),
		32,
		28,
		5
	);

	private static final int RIGHT_SIDE_X = 248;

	private final Textures selectedTextures;
	private final Textures unselectedTextures;
	private final int width;
	private final int height;
	private final int tabCount;

	AdvancementTabType(
		final Textures selectedTextures,
		final Textures unselectedTextures,
		final int width,
		final int height,
		final int tabCount
	) {
		this.selectedTextures = selectedTextures;
		this.unselectedTextures = unselectedTextures;
		this.width = width;
		this.height = height;
		this.tabCount = tabCount;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getTabCount() {
		return tabCount;
	}

	/**
	 * Рисует фоновую текстуру вкладки. Выбирает текстуру (первая/средняя/последняя)
	 * в зависимости от индекса вкладки в ряду.
	 */
	public void drawBackground(DrawContext context, int x, int y, boolean selected, int index) {
		Textures textures = selected ? selectedTextures : unselectedTextures;
		Identifier texture;
		if (index == 0) {
			texture = textures.first();
		}
		else if (index == tabCount - 1) {
			texture = textures.last();
		}
		else {
			texture = textures.middle();
		}

		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, width, height);
	}

	public void drawIcon(DrawContext context, int x, int y, int index, ItemStack stack) {
		int iconX = x + getTabX(index);
		int iconY = y + getTabY(index);
		switch (this) {
			case ABOVE -> {
				iconX += 6;
				iconY += 9;
			}
			case BELOW -> {
				iconX += 6;
				iconY += 6;
			}
			case LEFT -> {
				iconX += 10;
				iconY += 5;
			}
			case RIGHT -> {
				iconX += 6;
				iconY += 5;
			}
		}

		context.drawItemWithoutEntity(stack, iconX, iconY);
	}

	public int getTabX(int index) {
		return switch (this) {
			case ABOVE, BELOW -> (width + 4) * index;
			case LEFT -> -width + 4;
			case RIGHT -> RIGHT_SIDE_X;
		};
	}

	public int getTabY(int index) {
		return switch (this) {
			case ABOVE -> -height + 4;
			case BELOW -> 136;
			case LEFT, RIGHT -> height * index;
		};
	}

	public boolean isClickOnTab(int screenX, int screenY, int index, double mouseX, double mouseY) {
		int tabX = screenX + getTabX(index);
		int tabY = screenY + getTabY(index);
		return mouseX > tabX && mouseX < tabX + width && mouseY > tabY && mouseY < tabY + height;
	}

	@Environment(EnvType.CLIENT)
	record Textures(Identifier first, Identifier middle, Identifier last) {
	}
}
