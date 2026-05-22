package net.minecraft.client.gui.screen.report;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.AbuseReportReason;
import net.minecraft.client.session.report.AbuseReportType;
import net.minecraft.client.session.report.SkinAbuseReport;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Экран формы жалобы на скин игрока.
 * Отображает превью скина, позволяет выбрать причину жалобы и добавить комментарий.
 */
@Environment(EnvType.CLIENT)
public class SkinReportScreen extends ReportScreen<SkinAbuseReport.Builder> {

	private static final int SKIN_WIDGET_WIDTH = 85;
	private static final int REASON_BUTTON_AND_COMMENTS_BOX_WIDTH = 178;
	private static final Text TITLE_TEXT = Text.translatable("gui.abuseReport.skin.title");

	private EditBoxWidget commentsBox;
	private ButtonWidget selectReasonButton;

	private SkinReportScreen(Screen parent, AbuseReportContext context, SkinAbuseReport.Builder reportBuilder) {
		super(TITLE_TEXT, parent, context, reportBuilder);
	}

	public SkinReportScreen(
			Screen parent,
			AbuseReportContext context,
			UUID reportedPlayerUuid,
			Supplier<SkinTextures> skinSupplier
	) {
		this(
				parent,
				context,
				new SkinAbuseReport.Builder(reportedPlayerUuid, skinSupplier, context.getSender().getLimits())
		);
	}

	public SkinReportScreen(Screen parent, AbuseReportContext context, SkinAbuseReport report) {
		this(parent, context, new SkinAbuseReport.Builder(report, context.getSender().getLimits()));
	}

	@Override
	protected void addContent() {
		DirectionalLayoutWidget contentRow = layout.add(DirectionalLayoutWidget.horizontal().spacing(8));
		contentRow.getMainPositioner().alignVerticalCenter();

		contentRow.add(new PlayerSkinWidget(
				SKIN_WIDGET_WIDTH,
				COMMENT_BOX_HEIGHT,
				client.getLoadedEntityModels(),
				reportBuilder.getReport().getSkinSupplier()
		));

		DirectionalLayoutWidget rightColumn = contentRow.add(DirectionalLayoutWidget.vertical().spacing(8));

		selectReasonButton = ButtonWidget.builder(
				SELECT_REASON_TEXT,
				button -> client.setScreen(new AbuseReportReasonScreen(
						this, reportBuilder.getReason(), AbuseReportType.SKIN, reason -> {
							reportBuilder.setReason(reason);
							onChange();
						}
				))
		).width(REASON_BUTTON_AND_COMMENTS_BOX_WIDTH).build();

		rightColumn.add(LayoutWidgets.createLabeledWidget(textRenderer, selectReasonButton, OBSERVED_WHAT_TEXT));

		commentsBox = createCommentsBox(
				REASON_BUTTON_AND_COMMENTS_BOX_WIDTH, 9 * 8, comments -> {
					reportBuilder.setOpinionComments(comments);
					onChange();
				}
		);

		rightColumn.add(LayoutWidgets.createLabeledWidget(
				textRenderer,
				commentsBox,
				MORE_COMMENTS_TEXT,
				positioner -> positioner.marginBottom(12)
		));
	}

	@Override
	protected void onChange() {
		AbuseReportReason reason = reportBuilder.getReason();
		selectReasonButton.setMessage(reason != null ? reason.getText() : SELECT_REASON_TEXT);
		super.onChange();
	}

	@Override
	public boolean mouseReleased(Click click) {
		return super.mouseReleased(click) || commentsBox.mouseReleased(click);
	}
}
