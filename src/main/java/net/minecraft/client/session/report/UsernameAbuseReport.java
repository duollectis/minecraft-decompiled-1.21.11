package net.minecraft.client.session.report;

import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.minecraft.report.ReportedEntity;
import com.mojang.datafixers.util.Either;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.report.UsernameReportScreen;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Жалоба на нарушение правил, связанное с именем пользователя.
 * Не требует выбора причины — достаточно комментария и подтверждения.
 */
@Environment(EnvType.CLIENT)
public class UsernameAbuseReport extends AbuseReport {

	private final String username;

	UsernameAbuseReport(UUID reportId, Instant currentTime, UUID reportedPlayerUuid, String username) {
		super(reportId, currentTime, reportedPlayerUuid);
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public UsernameAbuseReport copy() {
		UsernameAbuseReport copy = new UsernameAbuseReport(reportId, currentTime, reportedPlayerUuid, username);
		copy.opinionComments = opinionComments;
		copy.attested = attested;
		return copy;
	}

	@Override
	public Screen createReportScreen(Screen parent, AbuseReportContext context) {
		return new UsernameReportScreen(parent, context, this);
	}

	/** Строитель жалобы на имя пользователя. */
	@Environment(EnvType.CLIENT)
	public static class Builder extends AbuseReport.Builder<UsernameAbuseReport> {

		public Builder(UsernameAbuseReport report, AbuseReportLimits limits) {
			super(report, limits);
		}

		public Builder(UUID reportedPlayerUuid, String username, AbuseReportLimits limits) {
			super(new UsernameAbuseReport(UUID.randomUUID(), Instant.now(), reportedPlayerUuid, username), limits);
		}

		@Override
		public boolean hasEnoughInfo() {
			return StringUtils.isNotEmpty(this.getOpinionComments());
		}

		@Override
		public AbuseReport.@Nullable ValidationError validate() {
			return this.report.opinionComments.length() > this.limits.maxOpinionCommentsLength()
			       ? AbuseReport.ValidationError.COMMENTS_TOO_LONG
			       : super.validate();
		}

		@Override
		public Either<AbuseReport.ReportWithId, AbuseReport.ValidationError> build(AbuseReportContext context) {
			AbuseReport.ValidationError validationError = validate();
			if (validationError != null) {
				return Either.right(validationError);
			}

			ReportedEntity reportedEntity = new ReportedEntity(report.reportedPlayerUuid);
			com.mojang.authlib.minecraft.report.AbuseReport abuseReport =
					com.mojang.authlib.minecraft.report.AbuseReport.name(
							report.opinionComments, reportedEntity, report.currentTime
					);
			return Either.left(new AbuseReport.ReportWithId(report.reportId, AbuseReportType.USERNAME, abuseReport));
		}
	}
}
