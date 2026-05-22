package net.minecraft.client.session.report;

import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.datafixers.util.Either;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Базовый класс жалобы на нарушение правил (abuse report).
 * Содержит общие поля: идентификатор жалобы, UUID нарушителя, комментарий и причину.
 * Конкретные типы жалоб (чат, скин, имя) реализуются в подклассах.
 */
@Environment(EnvType.CLIENT)
public abstract class AbuseReport {

	protected final UUID reportId;
	protected final Instant currentTime;
	protected final UUID reportedPlayerUuid;
	protected String opinionComments = "";
	protected @Nullable AbuseReportReason reason;
	protected boolean attested;

	public AbuseReport(UUID reportId, Instant currentTime, UUID reportedPlayerUuid) {
		this.reportId = reportId;
		this.currentTime = currentTime;
		this.reportedPlayerUuid = reportedPlayerUuid;
	}

	/**
	 * Player uuid equals.
	 *
	 * @param uuid uuid
	 *
	 * @return boolean — результат операции
	 */
	public boolean playerUuidEquals(UUID uuid) {
		return uuid.equals(this.reportedPlayerUuid);
	}

	/**
	 * Copy.
	 *
	 * @return AbuseReport — результат операции
	 */
	public abstract AbuseReport copy();

	/**
	 * Создаёт report screen.
	 *
	 * @param parent parent
	 * @param context context
	 *
	 * @return Screen — результат операции
	 */
	public abstract Screen createReportScreen(Screen parent, AbuseReportContext context);

	/**
	 * Базовый строитель жалобы. Хранит ссылку на изменяемый объект жалобы
	 * и лимиты, полученные от сервера авторизации Mojang.
	 */
	@Environment(EnvType.CLIENT)
	public abstract static class Builder<R extends AbuseReport> {

		protected final R report;
		protected final AbuseReportLimits limits;

		protected Builder(R report, AbuseReportLimits limits) {
			this.report = report;
			this.limits = limits;
		}

		public R getReport() {
			return report;
		}

		public UUID getReportedPlayerUuid() {
			return report.reportedPlayerUuid;
		}

		public String getOpinionComments() {
			return report.opinionComments;
		}

		public boolean isAttested() {
			return report.attested;
		}

		public void setOpinionComments(String opinionComments) {
			report.opinionComments = opinionComments;
		}

		public @Nullable AbuseReportReason getReason() {
			return report.reason;
		}

		public void setReason(AbuseReportReason reason) {
			report.reason = reason;
		}

		public void setAttested(boolean attested) {
			report.attested = attested;
		}

		public abstract boolean hasEnoughInfo();

		public AbuseReport.@Nullable ValidationError validate() {
			return report.attested ? null : AbuseReport.ValidationError.NOT_ATTESTED;
		}

		public abstract Either<AbuseReport.ReportWithId, AbuseReport.ValidationError> build(AbuseReportContext context);
	}

	/**
	 * Жалоба, готовая к отправке: содержит сгенерированный UUID и сериализованный объект Mojang Authlib.
	 */
	@Environment(EnvType.CLIENT)
	public record ReportWithId(
			UUID id,
			AbuseReportType reportType,
			com.mojang.authlib.minecraft.report.AbuseReport report
	) {
	}

	/**
	 * Ошибка валидации жалобы перед отправкой. Содержит локализованное сообщение для отображения пользователю.
	 */
	@Environment(EnvType.CLIENT)
	public record ValidationError(Text message) {

		public static final AbuseReport.ValidationError
				NO_REASON =
				new AbuseReport.ValidationError(Text.translatable("gui.abuseReport.send.no_reason"));
		public static final AbuseReport.ValidationError NO_REPORTED_MESSAGES = new AbuseReport.ValidationError(
				Text.translatable("gui.chatReport.send.no_reported_messages")
		);
		public static final AbuseReport.ValidationError TOO_MANY_MESSAGES = new AbuseReport.ValidationError(
				Text.translatable("gui.chatReport.send.too_many_messages")
		);
		public static final AbuseReport.ValidationError COMMENTS_TOO_LONG = new AbuseReport.ValidationError(
				Text.translatable("gui.abuseReport.send.comment_too_long")
		);
		public static final AbuseReport.ValidationError
				NOT_ATTESTED =
				new AbuseReport.ValidationError(Text.translatable("gui.abuseReport.send.not_attested"));

		public Tooltip createTooltip() {
			return Tooltip.of(this.message);
		}
	}
}
