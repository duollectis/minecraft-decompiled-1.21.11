package net.minecraft.client.gui.screen.option;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.option.GameOptions;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Urls;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * Экран информации о телеметрии — отображает список собираемых событий,
 * позволяет включить опциональную телеметрию и перейти к политике конфиденциальности.
 */
@Environment(EnvType.CLIENT)
public class TelemetryInfoScreen extends Screen {

	private static final Text TITLE_TEXT = Text.translatable("telemetry_info.screen.title");
	private static final Text DESCRIPTION_TEXT =
			Text.translatable("telemetry_info.screen.description").withColor(-4539718);
	private static final Text PRIVACY_STATEMENT_TEXT = Text.translatable("telemetry_info.button.privacy_statement");
	private static final Text GIVE_FEEDBACK_TEXT = Text.translatable("telemetry_info.button.give_feedback");
	private static final Text SHOW_DATA_TEXT = Text.translatable("telemetry_info.button.show_data");
	private static final Text OPT_IN_DESCRIPTION_TEXT =
			Text.translatable("telemetry_info.opt_in.description").withColor(-2039584);
	private static final int MARGIN = 8;
	private static final boolean OPTIONAL_TELEMETRY_ENABLED_BY_API =
			MinecraftClient.getInstance().isOptionalTelemetryEnabledByApi();

	private final Screen parent;
	private final GameOptions options;
	private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(
			this,
			16 + 9 * 5 + 20,
			OPTIONAL_TELEMETRY_ENABLED_BY_API
					? 33 + CheckboxWidget.getCheckboxSize(MinecraftClient.getInstance().textRenderer)
					: 33
	);
	private @Nullable TelemetryEventWidget telemetryEventWidget;
	private @Nullable MultilineTextWidget textWidget;
	private @Nullable CheckboxWidget optInCheckbox;
	private double scroll;

	public TelemetryInfoScreen(Screen parent, GameOptions options) {
		super(TITLE_TEXT);
		this.parent = parent;
		this.options = options;
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinSentences(super.getNarratedTitle(), DESCRIPTION_TEXT);
	}

	@Override
	protected void init() {
		DirectionalLayoutWidget headerLayout = layout.addHeader(DirectionalLayoutWidget.vertical().spacing(4));
		headerLayout.getMainPositioner().alignHorizontalCenter();
		headerLayout.add(new TextWidget(TITLE_TEXT, textRenderer));
		textWidget = headerLayout.add(new MultilineTextWidget(DESCRIPTION_TEXT, textRenderer).setCentered(true));
		DirectionalLayoutWidget linkRow = headerLayout.add(DirectionalLayoutWidget.horizontal().spacing(MARGIN));
		linkRow.add(ButtonWidget.builder(PRIVACY_STATEMENT_TEXT, this::openPrivacyStatementPage).build());
		linkRow.add(ButtonWidget.builder(GIVE_FEEDBACK_TEXT, this::openFeedbackPage).build());

		DirectionalLayoutWidget footerLayout = layout.addFooter(DirectionalLayoutWidget.vertical().spacing(4));
		footerLayout.getMainPositioner().alignHorizontalCenter();
		if (OPTIONAL_TELEMETRY_ENABLED_BY_API) {
			optInCheckbox = footerLayout.add(
					CheckboxWidget.builder(OPT_IN_DESCRIPTION_TEXT, textRenderer)
							.maxWidth(width - 40)
							.option(options.getTelemetryOptInExtra())
							.callback(this::updateOptIn)
							.build()
			);
		}

		DirectionalLayoutWidget actionRow = footerLayout.add(DirectionalLayoutWidget.horizontal().spacing(MARGIN));
		actionRow.add(ButtonWidget.builder(SHOW_DATA_TEXT, this::openLogDirectory).build());
		actionRow.add(ButtonWidget.builder(ScreenTexts.DONE, button -> close()).build());

		DirectionalLayoutWidget bodyLayout = layout.addBody(DirectionalLayoutWidget.vertical().spacing(MARGIN));
		telemetryEventWidget = bodyLayout.add(
				new TelemetryEventWidget(0, 0, width - 40, layout.getContentHeight(), textRenderer)
		);
		telemetryEventWidget.setScrollConsumer(newScroll -> scroll = newScroll);
		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		if (telemetryEventWidget != null) {
			telemetryEventWidget.setScrollY(scroll);
			telemetryEventWidget.setWidth(width - 40);
			telemetryEventWidget.setHeight(layout.getContentHeight());
			telemetryEventWidget.initContents();
		}

		if (textWidget != null) {
			textWidget.setMaxWidth(width - 16);
		}

		if (optInCheckbox != null) {
			optInCheckbox.setMaxWidth(width - 40, textRenderer);
		}

		layout.refreshPositions();
	}

	@Override
	protected void setInitialFocus() {
		if (telemetryEventWidget != null) {
			setInitialFocus(telemetryEventWidget);
		}
	}

	private void updateOptIn(ClickableWidget checkbox, boolean checked) {
		if (telemetryEventWidget != null) {
			telemetryEventWidget.refresh(checked);
		}
	}

	private void openPrivacyStatementPage(ButtonWidget button) {
		ConfirmLinkScreen.open(this, Urls.PRIVACY_STATEMENT);
	}

	private void openFeedbackPage(ButtonWidget button) {
		ConfirmLinkScreen.open(this, Urls.JAVA_FEEDBACK);
	}

	private void openLogDirectory(ButtonWidget button) {
		Util.getOperatingSystem().open(client.getTelemetryManager().getLogManager());
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}
}
