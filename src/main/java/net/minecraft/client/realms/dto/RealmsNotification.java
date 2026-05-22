package net.minecraft.client.realms.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.PopupScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.realms.util.JsonUtils;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Базовый класс уведомлений сервера Realms.
 * Подклассы {@link InfoPopup} и {@link VisitUrl} реализуют конкретные типы уведомлений.
 * Тип определяется полем {@code type} в JSON и диспетчеризуется в {@link #fromJson(JsonObject)}.
 */
@Environment(EnvType.CLIENT)
public class RealmsNotification {

	static final Logger LOGGER = LogUtils.getLogger();
	private static final String NOTIFICATION_UUID_KEY = "notificationUuid";
	private static final String DISMISSABLE_KEY = "dismissable";
	private static final String SEEN_KEY = "seen";
	private static final String TYPE_KEY = "type";
	private static final String VISIT_URL_TYPE = "visitUrl";
	private static final String INFO_POPUP_TYPE = "infoPopup";

	static final Text OPEN_LINK_TEXT = Text.translatable("mco.notification.visitUrl.buttonText.default");

	final UUID uuid;
	final boolean dismissable;
	final boolean seen;
	final String type;

	RealmsNotification(UUID uuid, boolean dismissable, boolean seen, String type) {
		this.uuid = uuid;
		this.dismissable = dismissable;
		this.seen = seen;
		this.type = type;
	}

	public boolean isSeen() {
		return seen;
	}

	public boolean isDismissable() {
		return dismissable;
	}

	public UUID getUuid() {
		return uuid;
	}

	/**
	 * Парсит список уведомлений из JSON-строки ответа сервера Realms.
	 * Неизвестные типы уведомлений сохраняются как базовый {@link RealmsNotification}.
	 *
	 * @param json JSON-строка с массивом {@code notifications}
	 * @return список уведомлений (может быть пустым при ошибке)
	 */
	public static List<RealmsNotification> parse(String json) {
		List<RealmsNotification> result = new ArrayList<>();

		try {
			for (JsonElement element : LenientJsonParser
					.parse(json)
					.getAsJsonObject()
					.get("notifications")
					.getAsJsonArray()) {
				result.add(fromJson(element.getAsJsonObject()));
			}
		} catch (Exception ex) {
			LOGGER.error("Could not parse list of RealmsNotifications", ex);
		}

		return result;
	}

	private static RealmsNotification fromJson(JsonObject json) {
		UUID uuid = JsonUtils.getUuidOr(NOTIFICATION_UUID_KEY, json, null);

		if (uuid == null) {
			throw new IllegalStateException("Missing required property notificationUuid");
		}

		boolean dismissable = JsonUtils.getBooleanOr(DISMISSABLE_KEY, json, true);
		boolean seen = JsonUtils.getBooleanOr(SEEN_KEY, json, false);
		String type = JsonUtils.getString(TYPE_KEY, json);
		RealmsNotification base = new RealmsNotification(uuid, dismissable, seen, type);

		return switch (type) {
			case VISIT_URL_TYPE -> VisitUrl.fromJson(base, json);
			case INFO_POPUP_TYPE -> InfoPopup.fromJson(base, json);
			default -> base;
		};
	}

	/**
	 * Уведомление с всплывающим окном, содержащим изображение, текст и опциональную кнопку-ссылку.
	 */
	@Environment(EnvType.CLIENT)
	public static class InfoPopup extends RealmsNotification {

		private static final String TITLE_KEY = "title";
		private static final String MESSAGE_KEY = "message";
		private static final String IMAGE_KEY = "image";
		private static final String URL_BUTTON_KEY = "urlButton";

		private final RealmsText title;
		private final RealmsText message;
		private final Identifier image;
		private final RealmsNotification.@Nullable UrlButton urlButton;

		private InfoPopup(
				RealmsNotification parent,
				RealmsText title,
				RealmsText message,
				Identifier image,
				RealmsNotification.@Nullable UrlButton urlButton
		) {
			super(parent.uuid, parent.dismissable, parent.seen, parent.type);
			this.title = title;
			this.message = message;
			this.image = image;
			this.urlButton = urlButton;
		}

		public static RealmsNotification.InfoPopup fromJson(RealmsNotification parent, JsonObject json) {
			RealmsText title = JsonUtils.get(TITLE_KEY, json, RealmsText::fromJson);
			RealmsText message = JsonUtils.get(MESSAGE_KEY, json, RealmsText::fromJson);
			Identifier image = Identifier.of(JsonUtils.getString(IMAGE_KEY, json));
			RealmsNotification.UrlButton urlButton = JsonUtils.getNullable(URL_BUTTON_KEY, json, UrlButton::fromJson);
			return new InfoPopup(parent, title, message, image, urlButton);
		}

		/**
		 * Создаёт всплывающий экран уведомления.
		 * Возвращает {@code null}, если заголовок не имеет перевода в текущей локали.
		 *
		 * @param backgroundScreen фоновый экран
		 * @param dismissCallback  колбэк для отметки уведомления как прочитанного
		 * @return экран уведомления или {@code null} при отсутствии перевода заголовка
		 */
		public @Nullable PopupScreen createScreen(Screen backgroundScreen, Consumer<UUID> dismissCallback) {
			Text titleText = title.toText();

			if (titleText == null) {
				LOGGER.warn("Realms info popup had title with no available translation: {}", title);
				return null;
			}

			PopupScreen.Builder builder = new PopupScreen.Builder(backgroundScreen, titleText)
					.image(image)
					.message(message.toText(ScreenTexts.EMPTY));

			if (urlButton != null) {
				builder.button(
						urlButton.urlText.toText(OPEN_LINK_TEXT),
						screen -> {
							MinecraftClient client = MinecraftClient.getInstance();
							client.setScreen(new ConfirmLinkScreen(
									confirmed -> {
										if (confirmed) {
											Util.getOperatingSystem().open(urlButton.url);
											client.setScreen(backgroundScreen);
										} else {
											client.setScreen(screen);
										}
									},
									urlButton.url,
									true
							));
							dismissCallback.accept(getUuid());
						}
				);
			}

			builder.button(ScreenTexts.OK, screen -> {
				screen.close();
				dismissCallback.accept(getUuid());
			});
			builder.onClosed(() -> dismissCallback.accept(getUuid()));
			return builder.build();
		}
	}

	@Environment(EnvType.CLIENT)
	record UrlButton(String url, RealmsText urlText) {

		private static final String URL_KEY = "url";
		private static final String URL_TEXT_KEY = "urlText";

		public static RealmsNotification.UrlButton fromJson(JsonObject json) {
			String url = JsonUtils.getString(URL_KEY, json);
			RealmsText urlText = JsonUtils.get(URL_TEXT_KEY, json, RealmsText::fromJson);
			return new UrlButton(url, urlText);
		}
	}

	/**
	 * Уведомление с кнопкой перехода по внешней ссылке.
	 */
	@Environment(EnvType.CLIENT)
	public static class VisitUrl extends RealmsNotification {

		private static final String URL_KEY = "url";
		private static final String BUTTON_TEXT_KEY = "buttonText";
		private static final String MESSAGE_KEY = "message";

		private final String url;
		private final RealmsText buttonText;
		private final RealmsText message;

		private VisitUrl(RealmsNotification parent, String url, RealmsText buttonText, RealmsText message) {
			super(parent.uuid, parent.dismissable, parent.seen, parent.type);
			this.url = url;
			this.buttonText = buttonText;
			this.message = message;
		}

		public static RealmsNotification.VisitUrl fromJson(RealmsNotification parent, JsonObject json) {
			String url = JsonUtils.getString(URL_KEY, json);
			RealmsText buttonText = JsonUtils.get(BUTTON_TEXT_KEY, json, RealmsText::fromJson);
			RealmsText message = JsonUtils.get(MESSAGE_KEY, json, RealmsText::fromJson);
			return new VisitUrl(parent, url, buttonText, message);
		}

		public Text getDefaultMessage() {
			return message.toText(Text.translatable("mco.notification.visitUrl.message.default"));
		}

		public ButtonWidget createButton(Screen currentScreen) {
			Text label = buttonText.toText(OPEN_LINK_TEXT);
			return ButtonWidget.builder(label, ConfirmLinkScreen.opening(currentScreen, url)).build();
		}
	}
}
