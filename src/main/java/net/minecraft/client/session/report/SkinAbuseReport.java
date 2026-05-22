package net.minecraft.client.session.report;

import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.minecraft.report.ReportedEntity;
import com.mojang.datafixers.util.Either;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.report.SkinReportScreen;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Жалоба на нарушение правил, связанное со скином игрока.
 * Содержит поставщик текстур скина для включения URL в запрос к API Mojang.
 */
@Environment(EnvType.CLIENT)
public class SkinAbuseReport extends AbuseReport {

	final Supplier<SkinTextures> skinSupplier;

	SkinAbuseReport(UUID reportId, Instant currentTime, UUID reportedPlayerUuid, Supplier<SkinTextures> skinSupplier) {
		super(reportId, currentTime, reportedPlayerUuid);
		this.skinSupplier = skinSupplier;
	}

	public Supplier<SkinTextures> getSkinSupplier() {
		return skinSupplier;
	}

	public SkinAbuseReport copy() {
		SkinAbuseReport copy = new SkinAbuseReport(reportId, currentTime, reportedPlayerUuid, skinSupplier);
		copy.opinionComments = opinionComments;
		copy.reason = reason;
		copy.attested = attested;
		return copy;
	}

	@Override
	public Screen createReportScreen(Screen parent, AbuseReportContext context) {
		return new SkinReportScreen(parent, context, this);
	}

	/** Строитель жалобы на скин: извлекает URL скина из текстур и формирует запрос. */
	@Environment(EnvType.CLIENT)
	public static class Builder extends AbuseReport.Builder<SkinAbuseReport> {

		public Builder(SkinAbuseReport report, AbuseReportLimits limits) {
			super(report, limits);
		}

		public Builder(UUID reportedPlayerUuid, Supplier<SkinTextures> skinSupplier, AbuseReportLimits limits) {
			super(new SkinAbuseReport(UUID.randomUUID(), Instant.now(), reportedPlayerUuid, skinSupplier), limits);
		}

		@Override
		public boolean hasEnoughInfo() {
			return StringUtils.isNotEmpty(this.getOpinionComments()) || this.getReason() != null;
		}

		@Override
		public AbuseReport.@Nullable ValidationError validate() {
			if (this.report.reason == null) {
				return AbuseReport.ValidationError.NO_REASON;
			}
			else {
				return this.report.opinionComments.length() > this.limits.maxOpinionCommentsLength()
				       ? AbuseReport.ValidationError.COMMENTS_TOO_LONG
				       : super.validate();
			}
		}

		@Override
		public Either<AbuseReport.ReportWithId, AbuseReport.ValidationError> build(AbuseReportContext context) {
			AbuseReport.ValidationError validationError = validate();
			if (validationError != null) {
				return Either.right(validationError);
			}

			String reasonId = Objects.requireNonNull(report.reason).getId();
			ReportedEntity reportedEntity = new ReportedEntity(report.reportedPlayerUuid);
			SkinTextures skinTextures = report.skinSupplier.get();
			String skinUrl = skinTextures.body() instanceof AssetInfo.SkinAssetInfo skinAssetInfo
					? skinAssetInfo.url()
					: null;
			com.mojang.authlib.minecraft.report.AbuseReport abuseReport =
					com.mojang.authlib.minecraft.report.AbuseReport.skin(
							report.opinionComments, reasonId, skinUrl, reportedEntity, report.currentTime
					);
			return Either.left(new AbuseReport.ReportWithId(report.reportId, AbuseReportType.SKIN, abuseReport));
		}
	}
}
