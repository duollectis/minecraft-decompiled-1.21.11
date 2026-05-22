package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.TestBlockEntity;
import net.minecraft.block.enums.TestBlockMode;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.packet.c2s.play.SetTestBlockC2SPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Экран настройки тестового блока. Позволяет задать режим и сообщение.
 */
@Environment(EnvType.CLIENT)
public class TestBlockScreen extends Screen {

	private static final List<TestBlockMode> MODES = List.of(TestBlockMode.values());
	private static final Text TITLE_TEXT = Text.translatable(Blocks.TEST_BLOCK.getTranslationKey());
	private static final Text MESSAGE_TEXT = Text.translatable("test_block.message");

	private static final int FIELD_WIDTH = 240;
	private static final int FIELD_HEIGHT = 20;
	private static final int FIELD_MAX_LENGTH = 128;
	private static final int FIELD_Y = 80;
	private static final int MODE_BUTTON_Y = 185;
	private static final int BUTTON_ROW_Y = 210;
	private static final int BUTTON_WIDTH = 150;
	private static final int SMALL_BUTTON_WIDTH = 50;
	private static final int TEXT_COLOR_LABEL = -6250336;
	private static final int TEXT_COLOR_WHITE = -1;

	private final BlockPos pos;
	private TestBlockMode mode;
	private String message;
	private @Nullable TextFieldWidget textField;

	public TestBlockScreen(TestBlockEntity blockEntity) {
		super(TITLE_TEXT);
		pos = blockEntity.getPos();
		mode = blockEntity.getMode();
		message = blockEntity.getMessage();
	}

	@Override
	public void init() {
		textField = new TextFieldWidget(textRenderer, width / 2 - 152, FIELD_Y, FIELD_WIDTH, FIELD_HEIGHT, MESSAGE_TEXT);
		textField.setMaxLength(FIELD_MAX_LENGTH);
		textField.setText(message);
		addDrawableChild(textField);

		setMode(mode);

		addDrawableChild(
			CyclingButtonWidget.builder(TestBlockMode::getName, mode)
				.values(MODES)
				.omitKeyText()
				.build(
					width / 2 - 4 - BUTTON_WIDTH,
					MODE_BUTTON_Y,
					SMALL_BUTTON_WIDTH,
					FIELD_HEIGHT,
					TITLE_TEXT,
					(button, newMode) -> setMode(newMode)
				)
		);

		addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> onDone())
			.dimensions(width / 2 - 4 - BUTTON_WIDTH, BUTTON_ROW_Y, BUTTON_WIDTH, FIELD_HEIGHT)
			.build());

		addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, button -> onCancel())
			.dimensions(width / 2 + 4, BUTTON_ROW_Y, BUTTON_WIDTH, FIELD_HEIGHT)
			.build());
	}

	@Override
	protected void setInitialFocus() {
		if (textField != null) {
			setInitialFocus(textField);
		} else {
			super.setInitialFocus();
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, TEXT_COLOR_WHITE);

		if (mode != TestBlockMode.START) {
			context.drawTextWithShadow(textRenderer, MESSAGE_TEXT, width / 2 - 153, 70, TEXT_COLOR_LABEL);
		}

		context.drawTextWithShadow(textRenderer, mode.getInfo(), width / 2 - 153, 174, TEXT_COLOR_LABEL);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public boolean deferSubtitles() {
		return true;
	}

	private void onDone() {
		message = textField.getText();
		client.getNetworkHandler().sendPacket(new SetTestBlockC2SPacket(pos, mode, message));
		close();
	}

	@Override
	public void close() {
		onCancel();
	}

	private void onCancel() {
		client.setScreen(null);
	}

	private void setMode(TestBlockMode newMode) {
		mode = newMode;
		textField.visible = newMode != TestBlockMode.START;
	}
}
