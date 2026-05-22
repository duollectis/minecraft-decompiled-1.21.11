package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Экран инвентаря сундука-шалкера.
 */
@Environment(EnvType.CLIENT)
public class ShulkerBoxScreen extends HandledScreen<ShulkerBoxScreenHandler> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/shulker_box.png");
	private static final int TEXTURE_SIZE = 256;

	public ShulkerBoxScreen(ShulkerBoxScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		backgroundHeight++;
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

		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			TEXTURE,
			bgX,
			bgY,
			0.0F,
			0.0F,
			backgroundWidth,
			backgroundHeight,
			TEXTURE_SIZE,
			TEXTURE_SIZE
		);
	}
}
