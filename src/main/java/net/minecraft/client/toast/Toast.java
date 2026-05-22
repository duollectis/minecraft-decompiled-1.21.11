package net.minecraft.client.toast;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

/**
 * Интерфейс всплывающего уведомления (toast) в правом верхнем углу экрана.
 *
 * <p>Каждый toast имеет тип ({@link #getType()}), используемый для дедупликации
 * в {@link ToastManager}, и видимость ({@link Visibility}), управляющую анимацией
 * появления/исчезновения.
 */
@Environment(EnvType.CLIENT)
public interface Toast {

	Object TYPE = new Object();

	int BASE_WIDTH = 160;
	int BASE_HEIGHT = 32;

	Toast.Visibility getVisibility();

	void update(ToastManager manager, long time);

	default @Nullable SoundEvent getSoundEvent() {
		return null;
	}

	void draw(DrawContext context, TextRenderer textRenderer, long startTime);

	default Object getType() {
		return TYPE;
	}

	default float getXPos(int scaledWindowWidth, float visibleWidthPortion) {
		return scaledWindowWidth - getWidth() * visibleWidthPortion;
	}

	default float getYPos(int topIndex) {
		return topIndex * getHeight();
	}

	default int getWidth() {
		return BASE_WIDTH;
	}

	default int getHeight() {
		return BASE_HEIGHT;
	}

	default int getRequiredSpaceCount() {
		return MathHelper.ceilDiv(getHeight(), BASE_HEIGHT);
	}

	default void onFinishedRendering() {
	}

	/** Состояние видимости toast: появляется или исчезает. */
	@Environment(EnvType.CLIENT)
	enum Visibility {
		SHOW(SoundEvents.UI_TOAST_IN),
		HIDE(SoundEvents.UI_TOAST_OUT);

		private final SoundEvent sound;

		Visibility(final SoundEvent sound) {
			this.sound = sound;
		}

		public void playSound(SoundManager soundManager) {
			soundManager.play(PositionedSoundInstance.master(sound, 1.0F, 1.0F));
		}
	}
}
