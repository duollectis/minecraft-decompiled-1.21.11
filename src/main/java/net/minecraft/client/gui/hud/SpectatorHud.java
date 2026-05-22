package net.minecraft.client.gui.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.spectator.SpectatorMenu;
import net.minecraft.client.gui.hud.spectator.SpectatorMenuCloseCallback;
import net.minecraft.client.gui.hud.spectator.SpectatorMenuCommand;
import net.minecraft.client.gui.hud.spectator.SpectatorMenuState;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

/**
 * HUD-компонент спектатора: отображает меню быстрого выбора команд и подсказку выбранной команды.
 */
@Environment(EnvType.CLIENT)
public class SpectatorHud implements SpectatorMenuCloseCallback {

	private static final Identifier HOTBAR_TEXTURE = Identifier.ofVanilla("hud/hotbar");
	private static final Identifier HOTBAR_SELECTION_TEXTURE = Identifier.ofVanilla("hud/hotbar_selection");
	private static final long FADE_OUT_DELAY = 5000L;
	private static final long FADE_OUT_DURATION = 2000L;
	private static final int HOTBAR_WIDTH = 182;
	private static final int HOTBAR_HEIGHT = 22;
	private static final int HOTBAR_HALF_WIDTH = 91;
	private static final int SELECTION_WIDTH = 24;
	private static final int SELECTION_HEIGHT = 23;
	private static final int SLOT_SIZE = 20;
	private static final int SLOTS_COUNT = 9;
	private static final int PROMPT_BOTTOM_OFFSET = 35;
	private static final float DISABLED_ICON_ALPHA = 0.25F;

	private final MinecraftClient client;
	private long lastInteractionTime;
	private @Nullable SpectatorMenu spectatorMenu;

	public SpectatorHud(MinecraftClient client) {
		this.client = client;
	}

	public void selectSlot(int slot) {
		lastInteractionTime = Util.getMeasuringTimeMs();

		if (spectatorMenu != null) {
			spectatorMenu.useCommand(slot);
		} else {
			spectatorMenu = new SpectatorMenu(this);
		}
	}

	private float getSpectatorMenuHeight() {
		long remaining = lastInteractionTime - Util.getMeasuringTimeMs() + FADE_OUT_DELAY;
		return MathHelper.clamp((float) remaining / FADE_OUT_DURATION, 0.0F, 1.0F);
	}

	public void renderSpectatorMenu(DrawContext context) {
		if (spectatorMenu == null) {
			return;
		}

		float opacity = getSpectatorMenuHeight();

		if (opacity <= 0.0F) {
			spectatorMenu.close();
			return;
		}

		int centerX = context.getScaledWindowWidth() / 2;
		int barY = MathHelper.floor(context.getScaledWindowHeight() - HOTBAR_HEIGHT * opacity);
		renderSpectatorMenu(context, opacity, centerX, barY, spectatorMenu.getCurrentState());
	}

	protected void renderSpectatorMenu(DrawContext context, float opacity, int x, int y, SpectatorMenuState state) {
		int color = ColorHelper.getWhite(opacity);
		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, x - HOTBAR_HALF_WIDTH, y, HOTBAR_WIDTH, HOTBAR_HEIGHT, color);

		if (state.getSelectedSlot() >= 0) {
			context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				HOTBAR_SELECTION_TEXTURE,
				x - HOTBAR_HALF_WIDTH - 1 + state.getSelectedSlot() * SLOT_SIZE,
				y - 1,
				SELECTION_WIDTH,
				SELECTION_HEIGHT,
				color
			);
		}

		for (int slot = 0; slot < SLOTS_COUNT; slot++) {
			renderSpectatorCommand(
				context,
				slot,
				context.getScaledWindowWidth() / 2 - 90 + slot * SLOT_SIZE + 2,
				y + 3,
				opacity,
				state.getCommand(slot)
			);
		}
	}

	private void renderSpectatorCommand(
		DrawContext context,
		int slot,
		int x,
		float y,
		float opacity,
		SpectatorMenuCommand command
	) {
		if (command == SpectatorMenu.BLANK_COMMAND) {
			return;
		}

		context.getMatrices().pushMatrix();
		context.getMatrices().translate(x, y);
		float iconAlpha = command.isEnabled() ? 1.0F : DISABLED_ICON_ALPHA;
		command.renderIcon(context, iconAlpha, opacity);
		context.getMatrices().popMatrix();

		if (opacity > 0.0F && command.isEnabled()) {
			Text keyText = client.options.hotbarKeys[slot].getBoundKeyLocalizedText();
			context.drawTextWithShadow(
				client.textRenderer,
				keyText,
				x + 19 - 2 - client.textRenderer.getWidth(keyText),
				(int) y + 6 + 3,
				ColorHelper.getWhite(opacity)
			);
		}
	}

	public void render(DrawContext context) {
		float opacity = getSpectatorMenuHeight();

		if (opacity <= 0.0F || spectatorMenu == null) {
			return;
		}

		SpectatorMenuCommand selectedCommand = spectatorMenu.getSelectedCommand();
		Text prompt = selectedCommand == SpectatorMenu.BLANK_COMMAND
			? spectatorMenu.getCurrentGroup().getPrompt()
			: selectedCommand.getName();

		int textWidth = client.textRenderer.getWidth(prompt);
		int textX = (context.getScaledWindowWidth() - textWidth) / 2;
		int textY = context.getScaledWindowHeight() - PROMPT_BOTTOM_OFFSET;

		context.drawTextWithBackground(client.textRenderer, prompt, textX, textY, textWidth, ColorHelper.getWhite(opacity));
	}

	@Override
	public void close(SpectatorMenu menu) {
		spectatorMenu = null;
		lastInteractionTime = 0L;
	}

	public boolean isOpen() {
		return spectatorMenu != null;
	}

	public void cycleSlot(int offset) {
		int slot = spectatorMenu.getSelectedSlot() + offset;

		while (slot >= 0
			&& slot <= 8
			&& (spectatorMenu.getCommand(slot) == SpectatorMenu.BLANK_COMMAND
				|| !spectatorMenu.getCommand(slot).isEnabled())
		) {
			slot += offset;
		}

		if (slot >= 0 && slot <= 8) {
			spectatorMenu.useCommand(slot);
			lastInteractionTime = Util.getMeasuringTimeMs();
		}
	}

	public void useSelectedCommand() {
		lastInteractionTime = Util.getMeasuringTimeMs();

		if (isOpen()) {
			int slot = spectatorMenu.getSelectedSlot();

			if (slot != -1) {
				spectatorMenu.useCommand(slot);
			}
		} else {
			spectatorMenu = new SpectatorMenu(this);
		}
	}
}
