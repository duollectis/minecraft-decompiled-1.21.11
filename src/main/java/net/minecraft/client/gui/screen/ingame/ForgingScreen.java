package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Базовый экран для всех столов ковки (наковальня, точильный камень, кузнечный стол).
 * Регистрируется как {@link ScreenHandlerListener} для отслеживания изменений слотов.
 */
@Environment(EnvType.CLIENT)
public abstract class ForgingScreen<T extends ForgingScreenHandler> extends HandledScreen<T> implements ScreenHandlerListener {

	private final Identifier texture;

	public ForgingScreen(T handler, PlayerInventory playerInventory, Text title, Identifier texture) {
		super(handler, playerInventory, title);
		this.texture = texture;
	}

	protected void setup() {
	}

	@Override
	protected void init() {
		super.init();
		setup();
		handler.addListener(this);
	}

	@Override
	public void removed() {
		super.removed();
		handler.removeListener(this);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
		context.drawTexture(
			RenderPipelines.GUI_TEXTURED,
			texture,
			x,
			y,
			0.0F,
			0.0F,
			backgroundWidth,
			backgroundHeight,
			256,
			256
		);
		drawInvalidRecipeArrow(context, x, y);
	}

	protected abstract void drawInvalidRecipeArrow(DrawContext context, int x, int y);

	@Override
	public void onPropertyUpdate(ScreenHandler handler, int property, int value) {
	}

	@Override
	public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
	}
}
