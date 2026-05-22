package net.minecraft.client.gui.screen.report;

import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.LayoutWidgets;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.AbuseReportReason;
import net.minecraft.client.session.report.AbuseReportType;
import net.minecraft.client.session.report.ChatAbuseReport;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * Экран формы жалобы на сообщения в чате.
 * Позволяет выбрать конкретные сообщения, указать причину жалобы и добавить комментарий.
 */
@Environment(EnvType.CLIENT)
public class ChatReportScreen extends ReportScreen<ChatAbuseReport.Builder> {

	private static final Text TITLE_TEXT = Text.translatable("gui.chatReport.title");
	private static final Text SELECT_CHAT_TEXT = Text.translatable("gui.chatReport.select_chat");

	private EditBoxWidget commentsBox;
	private ButtonWidget selectChatButton;
	private ButtonWidget selectReasonButton;

	private ChatReportScreen(Screen parent, AbuseReportContext context, ChatAbuseReport.Builder reportBuilder) {
		super(TITLE_TEXT, parent, context, reportBuilder);
	}

	public ChatReportScreen(Screen parent, AbuseReportContext reporter, UUID reportedPlayerUuid) {
		this(parent, reporter, new ChatAbuseReport.Builder(reportedPlayerUuid, reporter.getSender().getLimits()));
	}

	public ChatReportScreen(Screen parent, AbuseReportContext context, ChatAbuseReport report) {
		this(parent, context, new ChatAbuseReport.Builder(report, context.getSender().getLimits()));
	}

	@Override
	protected void addContent() {
		selectChatButton = layout.add(
				ButtonWidget.builder(
						SELECT_CHAT_TEXT,
						button -> client.setScreen(new ChatSelectionScreen(
								this, context, reportBuilder, updatedBuilder -> {
									reportBuilder = updatedBuilder;
									onChange();
								}
						))
				).width(CONTENT_WIDTH).build()
		);

		selectReasonButton = ButtonWidget.builder(
				SELECT_REASON_TEXT,
				button -> client.setScreen(new AbuseReportReasonScreen(
						this, reportBuilder.getReason(), AbuseReportType.CHAT, reason -> {
							reportBuilder.setReason(reason);
							onChange();
						}
				))
		).width(CONTENT_WIDTH).build();

		layout.add(LayoutWidgets.createLabeledWidget(textRenderer, selectReasonButton, OBSERVED_WHAT_TEXT));

		commentsBox = createCommentsBox(
				CONTENT_WIDTH, 9 * 8, comments -> {
					reportBuilder.setOpinionComments(comments);
					onChange();
				}
		);

		layout.add(LayoutWidgets.createLabeledWidget(
				textRenderer,
				commentsBox,
				MORE_COMMENTS_TEXT,
				positioner -> positioner.marginBottom(12)
		));
	}

	@Override
	protected void onChange() {
		IntSet selectedMessages = reportBuilder.getSelectedMessages();
		selectChatButton.setMessage(
				selectedMessages.isEmpty()
						? SELECT_CHAT_TEXT
						: Text.translatable("gui.chatReport.selected_chat", selectedMessages.size())
		);

		AbuseReportReason reason = reportBuilder.getReason();
		selectReasonButton.setMessage(reason != null ? reason.getText() : SELECT_REASON_TEXT);

		super.onChange();
	}

	@Override
	public boolean mouseReleased(Click click) {
		return super.mouseReleased(click) || commentsBox.mouseReleased(click);
	}
}
