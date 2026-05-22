package net.minecraft.client.realms;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.realms.exception.RealmsServiceException;
import net.minecraft.client.realms.gui.screen.RealmsClientIncompatibleScreen;
import net.minecraft.client.realms.gui.screen.RealmsGenericErrorScreen;
import net.minecraft.client.realms.gui.screen.RealmsParentalConsentScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Проверяет доступность сервиса Realms для текущего клиента.
 * Кэширует результат в {@link #currentFuture} и повторяет проверку только при ошибке.
 * Результат проверки инкапсулирован в {@link Info}, который умеет создавать
 * соответствующий экран ошибки через {@link Info#createScreen(Screen)}.
 */
@Environment(EnvType.CLIENT)
public class RealmsAvailability {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int HTTP_UNAUTHORIZED = 401;

	private static @Nullable CompletableFuture<RealmsAvailability.Info> currentFuture;

	/**
	 * Возвращает актуальный результат проверки доступности Realms.
	 * Повторяет запрос только если предыдущий завершился ошибкой.
	 *
	 * @return будущий результат проверки
	 */
	public static CompletableFuture<RealmsAvailability.Info> check() {
		if (currentFuture == null || wasUnsuccessful(currentFuture)) {
			currentFuture = checkInternal();
		}

		return currentFuture;
	}

	private static boolean wasUnsuccessful(CompletableFuture<RealmsAvailability.Info> future) {
		RealmsAvailability.Info info = future.getNow(null);
		return info != null && info.exception() != null;
	}

	private static CompletableFuture<RealmsAvailability.Info> checkInternal() {
		if (MinecraftClient.getInstance().isOfflineDeveloperMode()) {
			return CompletableFuture.completedFuture(new Info(Type.AUTHENTICATION_ERROR));
		}

		if (SharedConstants.BYPASS_REALMS_VERSION_CHECK) {
			return CompletableFuture.completedFuture(new Info(Type.SUCCESS));
		}

		return CompletableFuture.supplyAsync(
				() -> {
					RealmsClient realmsClient = RealmsClient.create();

					try {
						if (realmsClient.clientCompatible() != RealmsClient.CompatibleVersionResponse.COMPATIBLE) {
							return new Info(Type.INCOMPATIBLE_CLIENT);
						}

						return realmsClient.mcoEnabled()
								? new Info(Type.SUCCESS)
								: new Info(Type.NEEDS_PARENTAL_CONSENT);
					} catch (RealmsServiceException ex) {
						LOGGER.error("Couldn't connect to realms", ex);
						return ex.error.getErrorCode() == HTTP_UNAUTHORIZED
								? new Info(Type.AUTHENTICATION_ERROR)
								: new Info(ex);
					}
				},
				Util.getIoWorkerExecutor()
		);
	}

	/**
	 * Результат проверки доступности Realms.
	 * Содержит тип статуса и опциональное исключение для случая непредвиденной ошибки.
	 */
	@Environment(EnvType.CLIENT)
	public record Info(RealmsAvailability.Type type, @Nullable RealmsServiceException exception) {

		public Info(RealmsAvailability.Type type) {
			this(type, null);
		}

		public Info(RealmsServiceException exception) {
			this(Type.UNEXPECTED_ERROR, exception);
		}

		/**
		 * Создаёт экран ошибки, соответствующий текущему типу недоступности.
		 * Возвращает {@code null} при успешном статусе — экран показывать не нужно.
		 *
		 * @param parent родительский экран для возврата
		 * @return экран ошибки или {@code null} при {@link Type#SUCCESS}
		 */
		public @Nullable Screen createScreen(Screen parent) {
			return switch (type) {
				case SUCCESS -> null;
				case INCOMPATIBLE_CLIENT -> new RealmsClientIncompatibleScreen(parent);
				case NEEDS_PARENTAL_CONSENT -> new RealmsParentalConsentScreen(parent);
				case AUTHENTICATION_ERROR -> new RealmsGenericErrorScreen(
						Text.translatable("mco.error.invalid.session.title"),
						Text.translatable("mco.error.invalid.session.message"),
						parent
				);
				case UNEXPECTED_ERROR -> new RealmsGenericErrorScreen(Objects.requireNonNull(exception), parent);
			};
		}
	}

	@Environment(EnvType.CLIENT)
	public enum Type {
		SUCCESS,
		INCOMPATIBLE_CLIENT,
		NEEDS_PARENTAL_CONSENT,
		AUTHENTICATION_ERROR,
		UNEXPECTED_ERROR
	}
}
