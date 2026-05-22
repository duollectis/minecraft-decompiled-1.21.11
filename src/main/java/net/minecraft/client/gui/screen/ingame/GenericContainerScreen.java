package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Экран универсального контейнера (сундук, бочка). Высота фона зависит от
 * количества рядов слотов, переданных через обработчик экрана.
 */
@Environment(EnvType.CLIENT)
public class GenericContainerScreen extends HandledScreen<GenericContainerScreenHandler> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/generic_54.png");
	private static final int BASE_HEIGHT = 114;
	private static final int PLAYER_INVENTORY_OFFSET = 94;
	private static final int SLOT_HEIGHT = 18;
	private static final int HEADER_HEIGHT = 17;
	private static final float PLAYER_INVENTORY_V = 126.0F;
	private static final int PLAYER_INVENTORY_HEIGHT = 96;

	private final int rows;

	public GenericContainerScreen(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		rows = handler.getRows();
		backgroundHeight = BASE_HEIGHT + rows * SLOT_HEIGHT;
		playerInventoryTitleY = backgroundHeight - PLAYER_INVENTORY_OFFSET;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
		int bgX = (width - backgroundWidth) / 2;
		int bgY = (height - backgroundHeight) / 2;
		int containerHeight = rows * SLOT_HEIGHT + HEADER_HEIGHT;

		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			TEXTURE,
			bgX,
			bgY,
			0.0F,
			0.0F,
			backgroundWidth,
			containerHeight,
			256,
			256
		);
		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			TEXTURE,
			bgX,
			bgY + containerHeight,
			0.0F,
			PLAYER_INVENTORY_V,
			backgroundWidth,
			PLAYER_INVENTORY_HEIGHT,
			256,
			256
		);
	}
}
