package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;

/**
 * Кнопка-замок для блокировки сложности. Отображает иконку в зависимости от состояния
 * {@link #locked} и активности виджета.
 */
@Environment(EnvType.CLIENT)
public class LockButtonWidget extends ButtonWidget {

	private static final int BUTTON_SIZE = 20;

	private boolean locked;

	public LockButtonWidget(int x, int y, ButtonWidget.PressAction action) {
		super(
				x,
				y,
				BUTTON_SIZE,
				BUTTON_SIZE,
				net.minecraft.text.Text.translatable("narrator.button.difficulty_lock"),
				action,
				DEFAULT_NARRATION_SUPPLIER
		);
	}

	@Override
	protected MutableText getNarrationMessage() {
		return ScreenTexts.joinSentences(
				super.getNarrationMessage(),
				isLocked()
				? net.minecraft.text.Text.translatable("narrator.button.difficulty_lock.locked")
				: net.minecraft.text.Text.translatable("narrator.button.difficulty_lock.unlocked")
		);
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	@Override
	public void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		Icon icon;
		if (active) {
			icon = isSelected()
					? (locked ? Icon.LOCKED_HOVER : Icon.UNLOCKED_HOVER)
					: (locked ? Icon.LOCKED : Icon.UNLOCKED);
		}
		else {
			icon = locked ? Icon.LOCKED_DISABLED : Icon.UNLOCKED_DISABLED;
		}

		context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				icon.texture,
				getX(),
				getY(),
				width,
				height
		);
	}

	@Environment(EnvType.CLIENT)
	enum Icon {
		LOCKED(Identifier.ofVanilla("widget/locked_button")),
		LOCKED_HOVER(Identifier.ofVanilla("widget/locked_button_highlighted")),
		LOCKED_DISABLED(Identifier.ofVanilla("widget/locked_button_disabled")),
		UNLOCKED(Identifier.ofVanilla("widget/unlocked_button")),
		UNLOCKED_HOVER(Identifier.ofVanilla("widget/unlocked_button_highlighted")),
		UNLOCKED_DISABLED(Identifier.ofVanilla("widget/unlocked_button_disabled"));

		final Identifier texture;

		private Icon(final Identifier texture) {
			this.texture = texture;
		}
	}
}
