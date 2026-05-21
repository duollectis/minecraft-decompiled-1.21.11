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

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsUtil}.
 */
public class RealmsUtil {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Text NOW_TEXT = Text.translatable("mco.util.time.now");
	private static final int SECONDS_PER_MINUTE = 60;
	private static final int SECONDS_PER_HOUR = 3600;
	private static final int SECONDS_PER_DAY = 86400;

	/**
	 * Конвертирует to age presentation.
	 *
	 * @param milliseconds milliseconds
	 *
	 * @return Text — результат операции
	 */
	public static Text convertToAgePresentation(long milliseconds) {
		if (milliseconds < 0L) {
			return NOW_TEXT;
		}
		else {
			long l = milliseconds / 1000L;
			if (l < 60L) {
				return Text.translatable("mco.time.secondsAgo", l);
			}
			else if (l < 3600L) {
				long m = l / 60L;
				return Text.translatable("mco.time.minutesAgo", m);
			}
			else if (l < 86400L) {
				long m = l / 3600L;
				return Text.translatable("mco.time.hoursAgo", m);
			}
			else {
				long m = l / 86400L;
				return Text.translatable("mco.time.daysAgo", m);
			}
		}
	}

	/**
	 * Конвертирует to age presentation.
	 *
	 * @param instant instant
	 *
	 * @return Text — результат операции
	 */
	public static Text convertToAgePresentation(Instant instant) {
		return convertToAgePresentation(System.currentTimeMillis() - instant.toEpochMilli());
	}

	/**
	 * Draw player head.
	 *
	 * @param context context
	 * @param x x
	 * @param y y
	 * @param size size
	 * @param playerUuid player uuid
	 */
	public static void drawPlayerHead(DrawContext context, int x, int y, int size, UUID playerUuid) {
		PlayerSkinCache.Entry
				entry =
				MinecraftClient.getInstance().getPlayerSkinCache().get(ProfileComponent.ofDynamic(playerUuid));
		PlayerSkinDrawer.draw(context, entry.getTextures(), x, y, size);
	}

	public static <T> CompletableFuture<T> runAsync(
			RealmsUtil.RealmsSupplier<T> supplier,
			@Nullable Consumer<RealmsServiceException> errorCallback
	) {
		return CompletableFuture.supplyAsync(
				() -> {
					RealmsClient realmsClient = RealmsClient.create();

					try {
						return supplier.apply(realmsClient);
					}
					catch (Throwable var5) {
						if (var5 instanceof RealmsServiceException realmsServiceException) {
							if (errorCallback != null) {
								errorCallback.accept(realmsServiceException);
							}
						}
						else {
							LOGGER.error("Unhandled exception", var5);
						}

						throw new RuntimeException(var5);
					}
				}, Util.getDownloadWorkerExecutor()
		);
	}

	public static CompletableFuture<Void> runAsync(
			RealmsUtil.RealmsRunnable runnable,
			@Nullable Consumer<RealmsServiceException> errorCallback
	) {
		return RealmsUtil.<Void>runAsync((RealmsUtil.RealmsSupplier<Void>) runnable, errorCallback);
	}

	/**
	 * Открывает ing screen.
	 *
	 * @param screenCreator screen creator
	 *
	 * @return Consumer — результат операции
	 */
	public static Consumer<RealmsServiceException> openingScreen(Function<RealmsServiceException, Screen> screenCreator) {
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		return error -> minecraftClient.execute(() -> minecraftClient.setScreen(screenCreator.apply(error)));
	}

	public static Consumer<RealmsServiceException> openingScreenAndLogging(
			Function<RealmsServiceException, Screen> screenCreator,
			String errorPrefix
	) {
		return openingScreen(screenCreator).andThen(error -> LOGGER.error(errorPrefix, error));
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	/**
	 * {@code RealmsRunnable}.
	 */
	public interface RealmsRunnable extends RealmsUtil.RealmsSupplier<Void> {

		void accept(RealmsClient client) throws RealmsServiceException;

		default Void apply(RealmsClient realmsClient) throws RealmsServiceException {
			this.accept(realmsClient);
			return null;
		}
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	/**
	 * {@code RealmsSupplier}.
	 */
	public interface RealmsSupplier<T> {

		T apply(RealmsClient client) throws RealmsServiceException;
	}
}
