package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.world.CommandBlockExecutor;

/**
 * Базовый экран редактирования командного блока. Предоставляет поле ввода команды,
 * поле предыдущего вывода и кнопку переключения отслеживания вывода.
 */
@Environment(EnvType.CLIENT)
public abstract class AbstractCommandBlockScreen extends Screen {

	private static final Text SET_COMMAND_TEXT = Text.translatable("advMode.setCommand");
	private static final Text COMMAND_TEXT = Text.translatable("advMode.command");
	private static final Text PREVIOUS_OUTPUT_TEXT = Text.translatable("advMode.previousOutput");
	private static final int COMMAND_FIELD_WIDTH = 300;
	private static final int COMMAND_FIELD_HEIGHT = 20;
	private static final int OUTPUT_FIELD_WIDTH = 276;
	private static final int BUTTON_SIZE = 20;
	private static final int COMMAND_FIELD_Y = 50;
	private static final int COMMAND_LABEL_Y = 40;
	private static final int TITLE_Y = 20;
	private static final int BUTTON_ROW_Y_OFFSET = 120 + 12;
	private static final int SUGGESTIONS_MAX_ROWS = 7;
	private static final int OUTPUT_LABEL_OFFSET = 4;
	private static final int OUTPUT_EXTRA_HEIGHT = 5 * 9 + 1;
	private static final int OUTPUT_BASE_Y = 75;
	private static final int OUTPUT_ADJUST_BASE = 135;
	private static final int TEXT_COLOR_LABEL = -6250336;

	protected TextFieldWidget consoleCommandTextField;
	protected TextFieldWidget previousOutputTextField;
	protected ButtonWidget doneButton;
	protected ButtonWidget cancelButton;
	protected CyclingButtonWidget<Boolean> toggleTrackingOutputButton;
	ChatInputSuggestor commandSuggestor;

	public AbstractCommandBlockScreen() {
		super(NarratorManager.EMPTY);
	}

	@Override
	public void tick() {
		if (!getCommandExecutor().isEditable()) {
			close();
		}
	}

	abstract CommandBlockExecutor getCommandExecutor();

	abstract int getTrackOutputButtonHeight();

	@Override
	protected void init() {
		boolean trackingOutput = getCommandExecutor().isTrackingOutput();

		consoleCommandTextField = new TextFieldWidget(
			textRenderer,
			width / 2 - 150,
			COMMAND_FIELD_Y,
			COMMAND_FIELD_WIDTH,
			COMMAND_FIELD_HEIGHT,
			Text.translatable("advMode.command")
		) {
			@Override
			protected MutableText getNarrationMessage() {
				return super.getNarrationMessage()
					.append(AbstractCommandBlockScreen.this.commandSuggestor.getNarration());
			}
		};
		consoleCommandTextField.setMaxLength(32500);
		consoleCommandTextField.setChangedListener(this::onCommandChanged);
		addSelectableChild(consoleCommandTextField);

		previousOutputTextField = new TextFieldWidget(
			textRenderer,
			width / 2 - 150,
			getTrackOutputButtonHeight(),
			OUTPUT_FIELD_WIDTH,
			COMMAND_FIELD_HEIGHT,
			Text.translatable("advMode.previousOutput")
		);
		previousOutputTextField.setMaxLength(32500);
		previousOutputTextField.setEditable(false);
		previousOutputTextField.setText("-");
		addSelectableChild(previousOutputTextField);

		toggleTrackingOutputButton = addDrawableChild(
			CyclingButtonWidget.onOffBuilder(Text.literal("O"), Text.literal("X"), trackingOutput)
				.omitKeyText()
				.build(
					width / 2 + 150 - BUTTON_SIZE,
					getTrackOutputButtonHeight(),
					BUTTON_SIZE,
					BUTTON_SIZE,
					Text.translatable("advMode.trackOutput"),
					(button, trackOutput) -> {
						CommandBlockExecutor executor = getCommandExecutor();
						executor.setTrackOutput(trackOutput);
						setPreviousOutputText(trackOutput);
					}
				)
		);

		addAdditionalButtons();

		doneButton = addDrawableChild(
			ButtonWidget.builder(ScreenTexts.DONE, button -> commitAndClose())
				.dimensions(width / 2 - 4 - 150, height / 4 + BUTTON_ROW_Y_OFFSET, 150, COMMAND_FIELD_HEIGHT)
				.build()
		);
		cancelButton = addDrawableChild(
			ButtonWidget.builder(ScreenTexts.CANCEL, button -> close())
				.dimensions(width / 2 + 4, height / 4 + BUTTON_ROW_Y_OFFSET, 150, COMMAND_FIELD_HEIGHT)
				.build()
		);

		commandSuggestor = new ChatInputSuggestor(
			client,
			this,
			consoleCommandTextField,
			textRenderer,
			true,
			true,
			0,
			SUGGESTIONS_MAX_ROWS,
			false,
			Integer.MIN_VALUE
		);
		commandSuggestor.setWindowActive(true);
		commandSuggestor.refresh();
		setPreviousOutputText(trackingOutput);
	}

	protected void addAdditionalButtons() {
	}

	@Override
	protected void setInitialFocus() {
		setInitialFocus(consoleCommandTextField);
	}

	@Override
	protected Text getUsageNarrationText() {
		return commandSuggestor.isOpen()
			? commandSuggestor.getSuggestionUsageNarrationText()
			: super.getUsageNarrationText();
	}

	@Override
	public void resize(int width, int height) {
		String currentCommand = consoleCommandTextField.getText();
		init(width, height);
		consoleCommandTextField.setText(currentCommand);
		commandSuggestor.refresh();
	}

	protected void setPreviousOutputText(boolean trackOutput) {
		previousOutputTextField.setText(
			trackOutput ? getCommandExecutor().getLastOutput().getString() : "-"
		);
	}

	/**
	 * Сохраняет настройки на сервер и закрывает экран.
	 * Если отслеживание вывода отключено — очищает последний вывод.
	 */
	protected void commitAndClose() {
		syncSettingsToServer();
		CommandBlockExecutor executor = getCommandExecutor();

		if (!executor.isTrackingOutput()) {
			executor.setLastOutput(null);
		}

		client.setScreen(null);
	}

	protected abstract void syncSettingsToServer();

	private void onCommandChanged(String text) {
		commandSuggestor.refresh();
	}

	@Override
	public boolean deferSubtitles() {
		return true;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (commandSuggestor.keyPressed(input)) {
			return true;
		}

		if (super.keyPressed(input)) {
			return true;
		}

		if (input.isEnter()) {
			commitAndClose();
			return true;
		}

		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		return commandSuggestor.mouseScrolled(verticalAmount)
			? true
			: super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		return commandSuggestor.mouseClicked(click) ? true : super.mouseClicked(click, doubled);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		context.drawCenteredTextWithShadow(textRenderer, SET_COMMAND_TEXT, width / 2, TITLE_Y, -1);
		context.drawTextWithShadow(textRenderer, COMMAND_TEXT, width / 2 - 150 + 1, COMMAND_LABEL_Y, TEXT_COLOR_LABEL);
		consoleCommandTextField.render(context, mouseX, mouseY, deltaTicks);

		if (!previousOutputTextField.getText().isEmpty()) {
			int outputLabelY = OUTPUT_BASE_Y + OUTPUT_EXTRA_HEIGHT + getTrackOutputButtonHeight() - OUTPUT_ADJUST_BASE;
			context.drawTextWithShadow(
				textRenderer,
				PREVIOUS_OUTPUT_TEXT,
				width / 2 - 150 + 1,
				outputLabelY + OUTPUT_LABEL_OFFSET,
				TEXT_COLOR_LABEL
			);
			previousOutputTextField.render(context, mouseX, mouseY, deltaTicks);
		}

		commandSuggestor.render(context, mouseX, mouseY);
	}
}
