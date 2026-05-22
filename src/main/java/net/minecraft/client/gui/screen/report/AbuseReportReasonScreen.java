package net.minecraft.client.gui.screen.report;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.session.report.AbuseReportReason;
import net.minecraft.client.session.report.AbuseReportType;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Nullables;
import net.minecraft.util.Urls;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Экран выбора причины жалобы на нарушение правил.
 * Отображает список доступных причин и описание выбранной.
 */
@Environment(EnvType.CLIENT)
public class AbuseReportReasonScreen extends Screen {

	private static final Text TITLE_TEXT = Text.translatable("gui.abuseReport.reason.title");
	private static final Text DESCRIPTION_TEXT = Text.translatable("gui.abuseReport.reason.description");
	private static final Text READ_INFO_TEXT = Text.translatable("gui.abuseReport.read_info");
	private static final int CONTENT_WIDTH = 320;
	private static final int REASON_LIST_HEIGHT = 62;
	private static final int DESCRIPTION_PADDING = 4;
	private static final int DESCRIPTION_INDENT = 16;
	private static final int LINE_HEIGHT = 9;

	private final @Nullable Screen parent;
	private AbuseReportReasonScreen.@Nullable ReasonListWidget reasonList;
	@Nullable AbuseReportReason reason;
	private final Consumer<AbuseReportReason> reasonConsumer;
	final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);
	final AbuseReportType reportType;

	public AbuseReportReasonScreen(
			@Nullable Screen parent,
			@Nullable AbuseReportReason reason,
			AbuseReportType reportType,
			Consumer<AbuseReportReason> reasonConsumer
	) {
		super(TITLE_TEXT);
		this.parent = parent;
		this.reason = reason;
		this.reasonConsumer = reasonConsumer;
		this.reportType = reportType;
	}

	@Override
	protected void init() {
		layout.addHeader(TITLE_TEXT, textRenderer);

		DirectionalLayoutWidget bodyLayout = layout.addBody(DirectionalLayoutWidget.vertical().spacing(4));
		reasonList = bodyLayout.add(new AbuseReportReasonScreen.ReasonListWidget(client));

		AbuseReportReasonScreen.ReasonListWidget.ReasonEntry selectedEntry =
				Nullables.map(reason, reasonList::getEntry);
		reasonList.setSelected(selectedEntry);
		bodyLayout.add(EmptyWidget.ofHeight(getHeight()));

		DirectionalLayoutWidget footerLayout = layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
		footerLayout.add(ButtonWidget
				.builder(READ_INFO_TEXT, ConfirmLinkScreen.opening(this, Urls.ABOUT_JAVA_REPORTING))
				.build());
		footerLayout.add(ButtonWidget.builder(
				ScreenTexts.DONE, button -> {
					AbuseReportReasonScreen.ReasonListWidget.ReasonEntry entry = reasonList.getSelectedOrNull();

					if (entry != null) {
						reasonConsumer.accept(entry.getReason());
					}

					client.setScreen(parent);
				}
		).build());

		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		layout.refreshPositions();

		if (reasonList != null) {
			reasonList.position(width, getReasonListHeight(), layout.getHeaderHeight());
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);

		int left = getContentLeft();
		int top = getContentTop();
		int right = getContentRight();
		int bottom = getContentBottom();

		context.fill(left, top, right, bottom, -16777216);
		context.drawStrokedRectangle(left, top, getWidth(), getHeight(), -1);
		context.drawTextWithShadow(textRenderer, DESCRIPTION_TEXT, left + DESCRIPTION_PADDING, top + DESCRIPTION_PADDING, -1);

		AbuseReportReasonScreen.ReasonListWidget.ReasonEntry selectedEntry = reasonList.getSelectedOrNull();

		if (selectedEntry == null) {
			return;
		}

		int textLeft = left + DESCRIPTION_PADDING + DESCRIPTION_INDENT;
		int textRight = right - DESCRIPTION_PADDING;
		int textTop = top + DESCRIPTION_PADDING + LINE_HEIGHT + 2;
		int textBottom = bottom - DESCRIPTION_PADDING;
		int textWidth = textRight - textLeft;
		int textHeight = textBottom - textTop;
		int wrappedHeight = textRenderer.getWrappedLinesHeight(selectedEntry.reason.getDescription(), textWidth);

		context.drawWrappedTextWithShadow(
				textRenderer,
				selectedEntry.reason.getDescription(),
				textLeft,
				textTop + (textHeight - wrappedHeight) / 2,
				textWidth,
				-1
		);
	}

	private int getContentLeft() {
		return (width - CONTENT_WIDTH) / 2;
	}

	private int getContentRight() {
		return (width + CONTENT_WIDTH) / 2;
	}

	private int getContentTop() {
		return getContentBottom() - getHeight();
	}

	private int getContentBottom() {
		return height - layout.getFooterHeight() - DESCRIPTION_PADDING;
	}

	private int getWidth() {
		return CONTENT_WIDTH;
	}

	private int getHeight() {
		return REASON_LIST_HEIGHT;
	}

	int getReasonListHeight() {
		return layout.getContentHeight() - getHeight() - 8;
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	@Environment(EnvType.CLIENT)
	public class ReasonListWidget extends AlwaysSelectedEntryListWidget<AbuseReportReasonScreen.ReasonListWidget.ReasonEntry> {

		public ReasonListWidget(final MinecraftClient client) {
			super(
					client,
					AbuseReportReasonScreen.this.width,
					AbuseReportReasonScreen.this.getReasonListHeight(),
					AbuseReportReasonScreen.this.layout.getHeaderHeight(),
					18
			);

			for (AbuseReportReason abuseReportReason : AbuseReportReason.values()) {
				if (!AbuseReportReason.getExcludedReasonsForType(AbuseReportReasonScreen.this.reportType)
						.contains(abuseReportReason)) {
					addEntry(new AbuseReportReasonScreen.ReasonListWidget.ReasonEntry(abuseReportReason));
				}
			}
		}

		public AbuseReportReasonScreen.ReasonListWidget.@Nullable ReasonEntry getEntry(AbuseReportReason reason) {
			return children().stream().filter(entry -> entry.reason == reason).findFirst().orElse(null);
		}

		@Override
		public int getRowWidth() {
			return CONTENT_WIDTH;
		}

		public void setSelected(AbuseReportReasonScreen.ReasonListWidget.@Nullable ReasonEntry reasonEntry) {
			super.setSelected(reasonEntry);
			AbuseReportReasonScreen.this.reason = reasonEntry != null ? reasonEntry.getReason() : null;
		}

		@Environment(EnvType.CLIENT)
		public class ReasonEntry extends AlwaysSelectedEntryListWidget.Entry<AbuseReportReasonScreen.ReasonListWidget.ReasonEntry> {

			final AbuseReportReason reason;

			public ReasonEntry(final AbuseReportReason reason) {
				this.reason = reason;
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
				int textX = getContentX() + 1;
				int textY = getContentY() + (getContentHeight() - LINE_HEIGHT) / 2 + 1;
				context.drawTextWithShadow(AbuseReportReasonScreen.this.textRenderer, reason.getText(), textX, textY, -1);
			}

			@Override
			public Text getNarration() {
				return Text.translatable(
						"gui.abuseReport.reason.narration",
						reason.getText(),
						reason.getDescription()
				);
			}

			@Override
			public boolean mouseClicked(Click click, boolean doubled) {
				ReasonListWidget.this.setSelected(this);
				return super.mouseClicked(click, doubled);
			}

			public AbuseReportReason getReason() {
				return reason;
			}
		}
	}
}
