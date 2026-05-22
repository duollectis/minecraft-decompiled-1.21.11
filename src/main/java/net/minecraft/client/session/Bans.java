package net.minecraft.client.session;

import com.mojang.authlib.minecraft.BanDetails;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Urls;
import net.minecraft.util.Util;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

/**
 * Утилитарный класс для создания экранов уведомления о блокировке аккаунта.
 * Формирует тексты заголовков и описаний на основе данных {@link BanDetails} от Mojang.
 */
@Environment(EnvType.CLIENT)
public class Bans {

	private static final Text
			TEMPORARY_TITLE =
			Text.translatable("gui.banned.title.temporary").formatted(Formatting.BOLD);
	private static final Text
			PERMANENT_TITLE =
			Text.translatable("gui.banned.title.permanent").formatted(Formatting.BOLD);
	public static final Text NAME_TITLE = Text.translatable("gui.banned.name.title").formatted(Formatting.BOLD);
	private static final Text SKIN_TITLE = Text.translatable("gui.banned.skin.title").formatted(Formatting.BOLD);
	private static final Text
			SKIN_DESCRIPTION =
			Text.translatable("gui.banned.skin.description", Text.of(Urls.JAVA_MODERATION));

	/**
	 * Создаёт ban screen.
	 *
	 * @param callback callback
	 * @param banDetails ban details
	 *
	 * @return ConfirmLinkScreen — результат операции
	 */
	public static ConfirmLinkScreen createBanScreen(BooleanConsumer callback, BanDetails banDetails) {
		return new ConfirmLinkScreen(
				callback,
				getTitle(banDetails),
				getDescriptionText(banDetails),
				Urls.JAVA_MODERATION,
				ScreenTexts.ACKNOWLEDGE,
				true
		);
	}

	/**
	 * Создаёт skin ban screen.
	 *
	 * @param onClose on close
	 *
	 * @return ConfirmLinkScreen — результат операции
	 */
	public static ConfirmLinkScreen createSkinBanScreen(Runnable onClose) {
		URI uri = Urls.JAVA_MODERATION;
		return new ConfirmLinkScreen(
				confirmed -> {
					if (confirmed) {
						Util.getOperatingSystem().open(uri);
					}

					onClose.run();
				}, SKIN_TITLE, SKIN_DESCRIPTION, uri, ScreenTexts.ACKNOWLEDGE, true
		);
	}

	/**
	 * Создаёт username ban screen.
	 *
	 * @param username username
	 * @param onClose on close
	 *
	 * @return ConfirmLinkScreen — результат операции
	 */
	public static ConfirmLinkScreen createUsernameBanScreen(String username, Runnable onClose) {
		URI uri = Urls.JAVA_MODERATION;
		return new ConfirmLinkScreen(
				confirmed -> {
					if (confirmed) {
						Util.getOperatingSystem().open(uri);
					}

					onClose.run();
				},
				NAME_TITLE,
				Text.translatable(
						"gui.banned.name.description",
						Text.literal(username).formatted(Formatting.YELLOW),
						Text.of(Urls.JAVA_MODERATION)
				),
				uri,
				ScreenTexts.ACKNOWLEDGE,
				true
		);
	}

	private static Text getTitle(BanDetails banDetails) {
		return isTemporary(banDetails) ? TEMPORARY_TITLE : PERMANENT_TITLE;
	}

	private static Text getDescriptionText(BanDetails banDetails) {
		return Text.translatable(
				"gui.banned.description",
				getReasonText(banDetails),
				getDurationText(banDetails),
				Text.of(Urls.JAVA_MODERATION)
		);
	}

	private static Text getReasonText(BanDetails banDetails) {
		String reason = banDetails.reason();
		String reasonMessage = banDetails.reasonMessage();

		if (!StringUtils.isNumeric(reason)) {
			return Text.translatable("gui.banned.description.unknownreason");
		}

		int reasonId = Integer.parseInt(reason);
		BanReason banReason = BanReason.byId(reasonId);

		Text reasonText;
		if (banReason != null) {
			reasonText = Texts.withStyle(banReason.getDescription(), Style.EMPTY.withBold(true));
		}
		else if (reasonMessage != null) {
			reasonText = Text
					.translatable("gui.banned.description.reason_id_message", reasonId, reasonMessage)
					.formatted(Formatting.BOLD);
		}
		else {
			reasonText = Text.translatable("gui.banned.description.reason_id", reasonId).formatted(Formatting.BOLD);
		}

		return Text.translatable("gui.banned.description.reason", reasonText);
	}

	private static Text getDurationText(BanDetails banDetails) {
		if (isTemporary(banDetails)) {
			Text text = getTemporaryBanDurationText(banDetails);
			return Text.translatable(
					"gui.banned.description.temporary",
					Text.translatable("gui.banned.description.temporary.duration", text).formatted(Formatting.BOLD)
			);
		}
		else {
			return Text.translatable("gui.banned.description.permanent").formatted(Formatting.BOLD);
		}
	}

	private static Text getTemporaryBanDurationText(BanDetails banDetails) {
		Duration duration = Duration.between(Instant.now(), banDetails.expires());
		long hours = duration.toHours();

		if (hours > 72L) {
			return ScreenTexts.days(duration.toDays());
		}

		return hours < 1L
				? ScreenTexts.minutes(duration.toMinutes())
				: ScreenTexts.hours(hours);
	}

	private static boolean isTemporary(BanDetails banDetails) {
		return banDetails.expires() != null;
	}
}
