package net.minecraft.client.gui.screen.report;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.LayoutWidgets;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.UsernameAbuseReport;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Экран формы жалобы на никнейм игрока.
 * Отображает имя нарушителя и позволяет добавить комментарий к жалобе.
 */
@Environment(EnvType.CLIENT)
public class UsernameReportScreen extends ReportScreen<UsernameAbuseReport.Builder> {

	private static final Text TITLE_TEXT = Text.translatable("gui.abuseReport.name.title");
	private static final Text COMMENT_BOX_LABEL = Text.translatable("gui.abuseReport.name.comment_box_label");

	private @Nullable EditBoxWidget commentsBox;

	private UsernameReportScreen(Screen parent, AbuseReportContext context, UsernameAbuseReport.Builder reportBuilder) {
		super(TITLE_TEXT, parent, context, reportBuilder);
	}

	public UsernameReportScreen(Screen parent, AbuseReportContext context, UUID reportedPlayerUuid, String username) {
		this(
				parent,
				context,
				new UsernameAbuseReport.Builder(reportedPlayerUuid, username, context.getSender().getLimits())
		);
	}

	public UsernameReportScreen(Screen parent, AbuseReportContext context, UsernameAbuseReport report) {
		this(parent, context, new UsernameAbuseReport.Builder(report, context.getSender().getLimits()));
	}

	@Override
	protected void addContent() {
		Text usernameText = Text.literal(reportBuilder.getReport().getUsername()).formatted(Formatting.YELLOW);
		layout.add(
				new TextWidget(Text.translatable("gui.abuseReport.name.reporting", usernameText), textRenderer),
				positioner -> positioner.alignHorizontalCenter().margin(0, 8)
		);

		commentsBox = createCommentsBox(
				CONTENT_WIDTH, 9 * 8, comments -> {
					reportBuilder.setOpinionComments(comments);
					onChange();
				}
		);

		layout.add(LayoutWidgets.createLabeledWidget(
				textRenderer,
				commentsBox,
				COMMENT_BOX_LABEL,
				positioner -> positioner.marginBottom(12)
		));
	}

	@Override
	public boolean mouseReleased(Click click) {
		return super.mouseReleased(click)
				|| (commentsBox != null && commentsBox.mouseReleased(click));
	}
}
