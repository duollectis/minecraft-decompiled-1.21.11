package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

import java.util.List;

/**
 * Анимированная иконка слота, циклически переключающаяся между несколькими текстурами
 * с плавным переходом (fade) каждые {@code CYCLE_INTERVAL_TICKS} тиков.
 */
@Environment(EnvType.CLIENT)
public class CyclingSlotIcon {

	private static final int CYCLE_INTERVAL_TICKS = 30;
	private static final int ICON_SIZE = 16;
	private static final int FADE_TICKS = 4;

	private final int slotId;
	private List<Identifier> textures = List.of();
	private int timer;
	private int currentIndex;

	public CyclingSlotIcon(int slotId) {
		this.slotId = slotId;
	}

	public void updateTexture(List<Identifier> textures) {
		if (!this.textures.equals(textures)) {
			this.textures = textures;
			currentIndex = 0;
		}

		if (!this.textures.isEmpty() && ++timer % CYCLE_INTERVAL_TICKS == 0) {
			currentIndex = (currentIndex + 1) % this.textures.size();
		}
	}

	public void render(ScreenHandler screenHandler, DrawContext context, float deltaTicks, int x, int y) {
		Slot slot = screenHandler.getSlot(slotId);
		if (textures.isEmpty() || slot.hasStack()) {
			return;
		}

		boolean isCycling = textures.size() > 1 && timer >= CYCLE_INTERVAL_TICKS;
		float alpha = isCycling ? computeAlpha(deltaTicks) : 1.0F;

		if (alpha < 1.0F) {
			int previousIndex = Math.floorMod(currentIndex - 1, textures.size());
			drawIcon(slot, textures.get(previousIndex), 1.0F - alpha, context, x, y);
		}

		drawIcon(slot, textures.get(currentIndex), alpha, context, x, y);
	}

	private void drawIcon(Slot slot, Identifier texture, float alpha, DrawContext context, int x, int y) {
		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			texture,
			x + slot.x,
			y + slot.y,
			ICON_SIZE,
			ICON_SIZE,
			ColorHelper.getWhite(alpha)
		);
	}

	private float computeAlpha(float deltaTicks) {
		float elapsed = timer % CYCLE_INTERVAL_TICKS + deltaTicks;
		return Math.min(elapsed, FADE_TICKS) / FADE_TICKS;
	}
}
