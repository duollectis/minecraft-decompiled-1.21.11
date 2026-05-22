package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Экран воронки.
 */
@Environment(EnvType.CLIENT)
public class HopperScreen extends HandledScreen<HopperScreenHandler> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/hopper.png");
	private static final int BACKGROUND_HEIGHT_HOPPER = 133;
	private static final int PLAYER_INVENTORY_OFFSET = 94;

	public HopperScreen(HopperScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		backgroundHeight = BACKGROUND_HEIGHT_HOPPER;
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
		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			TEXTURE,
			bgX,
			bgY,
			0.0F,
			0.0F,
			backgroundWidth,
			backgroundHeight,
			256,
			256
		);
	}
}
