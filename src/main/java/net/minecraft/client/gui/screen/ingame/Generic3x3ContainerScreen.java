package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Экран контейнера 3×3 (диспенсер, дропер). Центрирует заголовок по ширине фона.
 */
@Environment(EnvType.CLIENT)
public class Generic3x3ContainerScreen extends HandledScreen<Generic3x3ContainerScreenHandler> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/dispenser.png");

	public Generic3x3ContainerScreen(Generic3x3ContainerScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
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
