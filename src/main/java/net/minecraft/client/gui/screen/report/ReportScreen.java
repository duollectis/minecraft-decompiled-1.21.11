package net.minecraft.client.gui.screen.report;

import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TaskScreen;
import net.minecraft.client.gui.screen.WarningScreen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.session.report.AbuseReport;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Nullables;
import net.minecraft.util.TextifiedException;
import org.slf4j.Logger;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Абстрактный базовый экран для всех форм отправки жалоб на игроков.
 * Управляет общей структурой: заголовок, контент, чекбокс аттестации и кнопка отправки.
 * Реализует логику отправки жалобы через {@link AbuseReportContext} и обработку ошибок.
 *
 * @param <B> тип строителя жалобы, расширяющий {@link AbuseReport.Builder}
 */
@Environment(EnvType.CLIENT)
public abstract class ReportScreen<B extends AbuseReport.Builder<?>> extends Screen {

	private static final Text REPORT_SENT_MESSAGE_TEXT = Text.translatable("gui.abuseReport.report_sent_msg");
	private static final Text SENDING_TITLE_TEXT = Text.translatable("gui.abuseReport.sending.title").formatted(Formatting.BOLD);
	private static final Text SENT_TITLE_TEXT = Text.translatable("gui.abuseReport.sent.title").formatted(Formatting.BOLD);
	private static final Text ERROR_TITLE_TEXT = Text.translatable("gui.abuseReport.error.title").formatted(Formatting.BOLD);
	private static final Text GENERIC_ERROR_TEXT = Text.translatable("gui.abuseReport.send.generic_error");
	protected static final Text SEND_TEXT = Text.translatable("gui.abuseReport.send");
	protected static final Text OBSERVED_WHAT_TEXT = Text.translatable("gui.abuseReport.observed_what");
	protected static final Text SELECT_REASON_TEXT = Text.translatable("gui.abuseReport.select_reason");
	private static final Text DESCRIBE_TEXT = Text.translatable("gui.abuseReport.describe");
	protected static final Text MORE_COMMENTS_TEXT = Text.translatable("gui.abuseReport.more_comments");
	private static final Text COMMENTS_TEXT = Text.translatable("gui.abuseReport.comments");
	private static final Text ATTESTATION_TEXT = Text.translatable("gui.abuseReport.attestation").withColor(-2039584);
	protected static final int COMMENT_BOX_HEIGHT = 120;
	protected static final int SECTION_SPACING = 20;
	protected static final int CONTENT_WIDTH = 280;
	protected static final int PADDING = 8;
	private static final Logger LOGGER = LogUtils.getLogger();

	protected final Screen parent;
	protected final AbuseReportContext context;
	protected final DirectionalLayoutWidget layout = DirectionalLayoutWidget.vertical().spacing(8);
	protected B reportBuilder;
	private CheckboxWidget checkbox;
	protected ButtonWidget sendButton;

	protected ReportScreen(Text title, Screen parent, AbuseReportContext context, B reportBuilder) {
		super(title);
		this.parent = parent;
		this.context = context;
		this.reportBuilder = reportBuilder;
	}

	/**
	 * Создаёт поле для ввода дополнительных комментариев к жалобе.
	 * Автоматически устанавливает максимальную длину из лимитов платформы
	 * и восстанавливает ранее введённый текст из строителя жалобы.
	 */
	protected EditBoxWidget createCommentsBox(int width, int height, Consumer<String> changeListener) {
		AbuseReportLimits limits = context.getSender().getLimits();
		EditBoxWidget editBox = EditBoxWidget
				.builder()
				.placeholder(DESCRIBE_TEXT)
				.build(textRenderer, width, height, COMMENTS_TEXT);

		editBox.setText(reportBuilder.getOpinionComments());
		editBox.setMaxLength(limits.maxOpinionCommentsLength());
		editBox.setChangeListener(changeListener);

		return editBox;
	}

	@Override
	protected void init() {
		layout.getMainPositioner().alignHorizontalCenter();
		addTitle();
		addContent();
		addAttestationCheckboxAndSendButton();
		onChange();
		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	protected void addTitle() {
		layout.add(new TextWidget(title, textRenderer));
	}

	protected abstract void addContent();

	protected void addAttestationCheckboxAndSendButton() {
		checkbox = layout.add(
				CheckboxWidget.builder(ATTESTATION_TEXT, textRenderer)
				              .checked(reportBuilder.isAttested())
				              .maxWidth(CONTENT_WIDTH)
				              .callback((cb, attested) -> {
					              reportBuilder.setAttested(attested);
					              onChange();
				              })
				              .build()
		);

		DirectionalLayoutWidget buttonRow = layout.add(DirectionalLayoutWidget.horizontal().spacing(8));
		buttonRow.add(ButtonWidget.builder(ScreenTexts.BACK, button -> close()).width(COMMENT_BOX_HEIGHT).build());
		sendButton = buttonRow.add(
				ButtonWidget.builder(SEND_TEXT, button -> trySend())
				            .width(COMMENT_BOX_HEIGHT)
				            .build()
		);
	}

	protected void onChange() {
		AbuseReport.ValidationError validationError = reportBuilder.validate();
		sendButton.active = validationError == null && checkbox.isChecked();
		sendButton.setTooltip(Nullables.map(validationError, AbuseReport.ValidationError::createTooltip));
	}

	@Override
	protected void refreshWidgetPositions() {
		layout.refreshPositions();
		SimplePositioningWidget.setPos(layout, getNavigationFocus());
	}

	/**
	 * Выполняет попытку отправки жалобы.
	 * При успехе показывает экран ожидания с возможностью отмены.
	 * При ошибке валидации — отображает сообщение об ошибке.
	 */
	protected void trySend() {
		reportBuilder.build(context).ifLeft(reportWithId -> {
			CompletableFuture<?> sendFuture = context.getSender()
					.send(reportWithId.id(), reportWithId.reportType(), reportWithId.report());

			client.setScreen(TaskScreen.createRunningScreen(
					SENDING_TITLE_TEXT, ScreenTexts.CANCEL, () -> {
						client.setScreen(this);
						sendFuture.cancel(true);
					}
			));

			sendFuture.handleAsync(
					(result, throwable) -> {
						if (throwable == null) {
							onSent();
							return null;
						}

						if (throwable instanceof CancellationException) {
							return null;
						}

						onSendError(throwable);
						return null;
					}, client
			);
		}).ifRight(validationError -> showError(validationError.message()));
	}

	private void onSent() {
		resetDraft();
		client.setScreen(TaskScreen.createResultScreen(
				SENT_TITLE_TEXT,
				REPORT_SENT_MESSAGE_TEXT,
				ScreenTexts.DONE,
				() -> client.setScreen(null)
		));
	}

	private void onSendError(Throwable error) {
		LOGGER.error("Encountered error while sending abuse report", error);
		Text errorText = error.getCause() instanceof TextifiedException textifiedException
				? textifiedException.getMessageText()
				: GENERIC_ERROR_TEXT;

		showError(errorText);
	}

	private void showError(Text errorMessage) {
		Text formatted = errorMessage.copy().formatted(Formatting.RED);
		client.setScreen(TaskScreen.createResultScreen(
				ERROR_TITLE_TEXT,
				formatted,
				ScreenTexts.BACK,
				() -> client.setScreen(this)
		));
	}

	void saveDraft() {
		if (reportBuilder.hasEnoughInfo()) {
			context.setDraft(reportBuilder.getReport().copy());
		}
	}

	void resetDraft() {
		context.setDraft(null);
	}

	@Override
	public void close() {
		if (reportBuilder.hasEnoughInfo()) {
			client.setScreen(new DiscardWarningScreen());
		}
		else {
			client.setScreen(parent);
		}
	}

	@Override
	public void removed() {
		saveDraft();
		super.removed();
	}

	/**
	 * Экран предупреждения при попытке закрыть форму жалобы с незавершёнными данными.
	 * Предлагает три варианта: вернуться к форме, сохранить черновик или отбросить изменения.
	 */
	@Environment(EnvType.CLIENT)
	class DiscardWarningScreen extends WarningScreen {

		private static final Text TITLE = Text.translatable("gui.abuseReport.discard.title").formatted(Formatting.BOLD);
		private static final Text MESSAGE = Text.translatable("gui.abuseReport.discard.content");
		private static final Text RETURN_BUTTON_TEXT = Text.translatable("gui.abuseReport.discard.return");
		private static final Text DRAFT_BUTTON_TEXT = Text.translatable("gui.abuseReport.discard.draft");
		private static final Text DISCARD_BUTTON_TEXT = Text.translatable("gui.abuseReport.discard.discard");

		protected DiscardWarningScreen() {
			super(TITLE, MESSAGE, MESSAGE);
		}

		@Override
		protected LayoutWidget getLayout() {
			DirectionalLayoutWidget root = DirectionalLayoutWidget.vertical().spacing(8);
			root.getMainPositioner().alignHorizontalCenter();

			DirectionalLayoutWidget topRow = root.add(DirectionalLayoutWidget.horizontal().spacing(8));
			topRow.add(ButtonWidget.builder(RETURN_BUTTON_TEXT, button -> close()).build());
			topRow.add(ButtonWidget.builder(
					DRAFT_BUTTON_TEXT, button -> {
						ReportScreen.this.saveDraft();
						client.setScreen(ReportScreen.this.parent);
					}
			).build());

			root.add(ButtonWidget.builder(
					DISCARD_BUTTON_TEXT, button -> {
						ReportScreen.this.resetDraft();
						client.setScreen(ReportScreen.this.parent);
					}
			).build());

			return root;
		}

		@Override
		public void close() {
			client.setScreen(ReportScreen.this);
		}

		@Override
		public boolean shouldCloseOnEsc() {
			return false;
		}
	}
}
