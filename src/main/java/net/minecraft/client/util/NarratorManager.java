package net.minecraft.client.util;

import com.mojang.logging.LogUtils;
import com.mojang.text2speech.Narrator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.NarratorMode;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

/**
 * Управляет синтезом речи (text-to-speech) для доступности интерфейса.
 * Делегирует озвучивание библиотеке {@code text2speech} с учётом текущего режима нарратора.
 */
@Environment(EnvType.CLIENT)
public class NarratorManager {

	public static final Text EMPTY = ScreenTexts.EMPTY;

	private static final Logger LOGGER = LogUtils.getLogger();

	private final MinecraftClient client;
	private final Narrator narrator = Narrator.getNarrator();

	public NarratorManager(MinecraftClient client) {
		this.client = client;
	}

	public void narrateChatMessage(Text message) {
		if (getNarratorMode().shouldNarrateChat()) {
			narrateText(message);
		}
	}

	public void narrate(Text message) {
		if (getNarratorMode().shouldNarrate()) {
			narrateText(message);
		}
	}

	public void narrateSystemMessage(Text message) {
		if (getNarratorMode().shouldNarrateSystem()) {
			narrateText(message);
		}
	}

	private void narrateText(Text message) {
		String text = message.getString();
		if (text.isEmpty()) {
			return;
		}

		debugPrintMessage(text);
		say(text, false);
	}

	public void narrateSystemImmediately(Text text) {
		narrateSystemImmediately(text.getString());
	}

	public void narrateSystemImmediately(String text) {
		if (!getNarratorMode().shouldNarrateSystem() || text.isEmpty()) {
			return;
		}

		debugPrintMessage(text);
		if (narrator.active()) {
			narrator.clear();
			say(text, true);
		}
	}

	private void say(String text, boolean interrupt) {
		narrator.say(text, interrupt, client.options.getSoundVolume(SoundCategory.VOICE));
	}

	private NarratorMode getNarratorMode() {
		return client.options.getNarrator().getValue();
	}

	private void debugPrintMessage(String message) {
		if (SharedConstants.isDevelopment) {
			LOGGER.debug("Narrating: {}", message.replaceAll("\n", "\\\\n"));
		}
	}

	/**
	 * Обрабатывает смену режима нарратора: озвучивает новый режим и показывает тост.
	 * Если нарратор недоступен, всегда показывает тост «отключён».
	 *
	 * @param mode новый режим нарратора
	 */
	public void onModeChange(NarratorMode mode) {
		clear();
		say(Text.translatable("options.narrator").append(" : ").append(mode.getName()).getString(), true);

		ToastManager toastManager = MinecraftClient.getInstance().getToastManager();
		if (!narrator.active()) {
			SystemToast.show(
				toastManager,
				SystemToast.Type.NARRATOR_TOGGLE,
				Text.translatable("narrator.toast.disabled"),
				Text.translatable("options.narrator.notavailable")
			);
			return;
		}

		if (mode == NarratorMode.OFF) {
			SystemToast.show(
				toastManager,
				SystemToast.Type.NARRATOR_TOGGLE,
				Text.translatable("narrator.toast.disabled"),
				null
			);
		} else {
			SystemToast.show(
				toastManager,
				SystemToast.Type.NARRATOR_TOGGLE,
				Text.translatable("narrator.toast.enabled"),
				mode.getName()
			);
		}
	}

	public boolean isActive() {
		return narrator.active();
	}

	public void clear() {
		if (getNarratorMode() != NarratorMode.OFF && narrator.active()) {
			narrator.clear();
		}
	}

	public void destroy() {
		narrator.destroy();
	}

	/**
	 * Проверяет доступность библиотеки нарратора и предлагает пользователю продолжить при её отсутствии.
	 *
	 * @param narratorEnabled запрошено ли включение нарратора
	 * @throws InactiveNarratorLibraryException если нарратор недоступен и пользователь отказался продолжать
	 */
	public void checkNarratorLibrary(boolean narratorEnabled) {
		if (!narratorEnabled || isActive()) {
			return;
		}

		boolean continueWithoutNarrator = TinyFileDialogs.tinyfd_messageBox(
			"Minecraft",
			"Failed to initialize text-to-speech library. Do you want to continue?\nIf this problem persists, please report it at bugs.mojang.com",
			"yesno",
			"error",
			true
		);
		if (!continueWithoutNarrator) {
			throw new InactiveNarratorLibraryException("Narrator library is not active");
		}
	}

	@Environment(EnvType.CLIENT)
	public static class InactiveNarratorLibraryException extends GlException {

		public InactiveNarratorLibraryException(String message) {
			super(message);
		}
	}
}
