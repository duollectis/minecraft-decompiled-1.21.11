package net.minecraft.client.realms.util;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.realms.RealmsClient;
import net.minecraft.client.realms.exception.RealmsServiceException;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Утилитарный класс для общих операций Realms-клиента:
 * форматирование временных меток, отрисовка аватаров игроков,
 * асинхронное выполнение запросов к Realms API и обработка ошибок.
 */
@Environment(EnvType.CLIENT)
public class RealmsUtil {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Text NOW_TEXT = Text.translatable("mco.util.time.now");
	private static final int SECONDS_PER_MINUTE = 60;
	private static final int SECONDS_PER_HOUR = 3600;
	private static final int SECONDS_PER_DAY = 86400;

	/**
	 * Преобразует количество миллисекунд в человекочитаемую строку вида «N минут назад».
	 * Отрицательное значение означает «только что».
	 *
	 * @param milliseconds прошедшее время в миллисекундах
	 * @return локализованный текст с относительным временем
	 */
	public static Text convertToAgePresentation(long milliseconds) {
		if (milliseconds < 0L) {
			return NOW_TEXT;
		}

		long totalSeconds = milliseconds / 1000L;

		if (totalSeconds < SECONDS_PER_MINUTE) {
			return Text.translatable("mco.time.secondsAgo", totalSeconds);
		}

		if (totalSeconds < SECONDS_PER_HOUR) {
			return Text.translatable("mco.time.minutesAgo", totalSeconds / SECONDS_PER_MINUTE);
		}

		if (totalSeconds < SECONDS_PER_DAY) {
			return Text.translatable("mco.time.hoursAgo", totalSeconds / SECONDS_PER_HOUR);
		}

		return Text.translatable("mco.time.daysAgo", totalSeconds / SECONDS_PER_DAY);
	}

	public static Text convertToAgePresentation(Instant instant) {
		return convertToAgePresentation(System.currentTimeMillis() - instant.toEpochMilli());
	}

	public static void drawPlayerHead(DrawContext context, int x, int y, int size, UUID playerUuid) {
		PlayerSkinCache.Entry entry = MinecraftClient.getInstance()
			.getPlayerSkinCache()
			.get(ProfileComponent.ofDynamic(playerUuid));

		PlayerSkinDrawer.draw(context, entry.getTextures(), x, y, size);
	}

	/**
	 * Выполняет асинхронный запрос к Realms API в пуле загрузочных потоков.
	 * При {@link RealmsServiceException} вызывает {@code errorCallback} (если задан),
	 * при любой другой ошибке — логирует и пробрасывает как {@link RuntimeException}.
	 *
	 * @param supplier      поставщик результата, получающий {@link RealmsClient}
	 * @param errorCallback обработчик ошибок Realms API или {@code null}
	 * @param <T>           тип результата
	 * @return {@link CompletableFuture} с результатом операции
	 */
	public static <T> CompletableFuture<T> runAsync(
		RealmsUtil.RealmsSupplier<T> supplier,
		@Nullable Consumer<RealmsServiceException> errorCallback
	) {
		return CompletableFuture.supplyAsync(
			() -> {
				RealmsClient realmsClient = RealmsClient.create();

				try {
					return supplier.apply(realmsClient);
				} catch (Throwable ex) {
					if (ex instanceof RealmsServiceException serviceException) {
						if (errorCallback != null) {
							errorCallback.accept(serviceException);
						}
					} else {
						LOGGER.error("Unhandled exception", ex);
					}

					throw new RuntimeException(ex);
				}
			},
			Util.getDownloadWorkerExecutor()
		);
	}

	public static CompletableFuture<Void> runAsync(
		RealmsUtil.RealmsRunnable runnable,
		@Nullable Consumer<RealmsServiceException> errorCallback
	) {
		return RealmsUtil.<Void>runAsync((RealmsUtil.RealmsSupplier<Void>) runnable, errorCallback);
	}

	public static Consumer<RealmsServiceException> openingScreen(Function<RealmsServiceException, Screen> screenCreator) {
		MinecraftClient client = MinecraftClient.getInstance();
		return error -> client.execute(() -> client.setScreen(screenCreator.apply(error)));
	}

	public static Consumer<RealmsServiceException> openingScreenAndLogging(
		Function<RealmsServiceException, Screen> screenCreator,
		String errorPrefix
	) {
		return openingScreen(screenCreator).andThen(error -> LOGGER.error(errorPrefix, error));
	}

	/**
	 * Функциональный интерфейс для операций Realms, не возвращающих значение.
	 * Реализует {@link RealmsSupplier}{@code <Void>} через делегирование к {@link #accept}.
	 */
	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	public interface RealmsRunnable extends RealmsUtil.RealmsSupplier<Void> {

		void accept(RealmsClient client) throws RealmsServiceException;

		default Void apply(RealmsClient client) throws RealmsServiceException {
			accept(client);
			return null;
		}
	}

	/**
	 * Функциональный интерфейс для операций Realms, возвращающих значение типа {@code T}.
	 *
	 * @param <T> тип возвращаемого значения
	 */
	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	public interface RealmsSupplier<T> {

		T apply(RealmsClient client) throws RealmsServiceException;
	}
}
