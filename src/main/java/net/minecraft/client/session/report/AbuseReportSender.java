package net.minecraft.client.session.report;

import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest;
import com.mojang.datafixers.util.Unit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.util.TextifiedException;
import net.minecraft.util.Util;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Интерфейс отправки жалоб на нарушение правил через API Mojang.
 * Реализация {@link Impl} выполняет HTTP-запрос в фоновом потоке через {@code UserApiService}.
 */
@Environment(EnvType.CLIENT)
public interface AbuseReportSender {

	static AbuseReportSender create(ReporterEnvironment environment, UserApiService userApiService) {
		return new AbuseReportSender.Impl(environment, userApiService);
	}

	CompletableFuture<Unit> send(UUID id, AbuseReportType type, com.mojang.authlib.minecraft.report.AbuseReport report);

	boolean canSendReports();

	default AbuseReportLimits getLimits() {
		return AbuseReportLimits.DEFAULTS;
	}

	/** Исключение, оборачивающее ошибку отправки жалобы с локализованным текстом для UI. */
	@Environment(EnvType.CLIENT)
	public static class AbuseReportException extends TextifiedException {

		public AbuseReportException(Text text, Throwable throwable) {
			super(text, throwable);
		}
	}

	/**
	 * Реализация отправителя жалоб через {@link UserApiService}.
	 * Версия API жалобы фиксирована: {@code 1}.
	 */
	@Environment(EnvType.CLIENT)
	public record Impl(ReporterEnvironment environment, UserApiService userApiService) implements AbuseReportSender {

		private static final Text
				SERVICE_UNAVAILABLE_ERROR_TEXT =
				Text.translatable("gui.abuseReport.send.service_unavailable");
		private static final Text HTTP_ERROR_TEXT = Text.translatable("gui.abuseReport.send.http_error");
		private static final Text JSON_ERROR_TEXT = Text.translatable("gui.abuseReport.send.json_error");

		private static final int REPORT_API_VERSION = 1;

		@Override
		public CompletableFuture<Unit> send(
				UUID id,
				AbuseReportType type,
				com.mojang.authlib.minecraft.report.AbuseReport report
		) {
			return CompletableFuture.supplyAsync(
					() -> {
						AbuseReportRequest request = new AbuseReportRequest(
								REPORT_API_VERSION,
								id,
								report,
								environment.toClientInfo(),
								environment.toThirdPartyServerInfo(),
								environment.toRealmInfo(),
								type.getName()
						);

						try {
							userApiService.reportAbuse(request);
							return Unit.INSTANCE;
						}
						catch (MinecraftClientHttpException httpException) {
							Text errorText = getErrorText(httpException);
							throw new CompletionException(new AbuseReportSender.AbuseReportException(errorText, httpException));
						}
						catch (MinecraftClientException clientException) {
							Text errorText = getErrorText(clientException);
							throw new CompletionException(new AbuseReportSender.AbuseReportException(errorText, clientException));
						}
					},
					Util.getIoWorkerExecutor()
			);
		}

		@Override
		public boolean canSendReports() {
			return userApiService.canSendReports();
		}

		private Text getErrorText(MinecraftClientHttpException exception) {
			return Text.translatable("gui.abuseReport.send.error_message", exception.getMessage());
		}

		private Text getErrorText(MinecraftClientException exception) {
			return switch (exception.getType()) {
				case SERVICE_UNAVAILABLE -> SERVICE_UNAVAILABLE_ERROR_TEXT;
				case HTTP_ERROR -> HTTP_ERROR_TEXT;
				case JSON_ERROR -> JSON_ERROR_TEXT;
				default -> throw new MatchException(null, null);
			};
		}

		@Override
		public AbuseReportLimits getLimits() {
			return userApiService.getAbuseReportLimits();
		}
	}
}
