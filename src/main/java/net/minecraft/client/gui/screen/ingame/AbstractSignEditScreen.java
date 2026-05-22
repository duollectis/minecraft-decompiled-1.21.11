package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.WoodType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.block.entity.AbstractSignBlockEntityRenderer;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.stream.IntStream;

/**
 * Базовый экран редактирования таблички. Управляет вводом текста по строкам,
 * отображением курсора/выделения и отправкой пакета обновления на сервер.
 */
@Environment(EnvType.CLIENT)
public abstract class AbstractSignEditScreen extends Screen {

	private static final int SIGN_LINES = 4;
	private static final int DONE_BUTTON_Y_OFFSET = 144;
	private static final int DONE_BUTTON_WIDTH = 200;
	private static final int DONE_BUTTON_HEIGHT = 20;
	private static final int TITLE_Y = 40;
	private static final int CURSOR_BLINK_PERIOD = 6;

	protected final SignBlockEntity blockEntity;
	private SignText text;
	private final String[] messages;
	private final boolean front;
	protected final WoodType signType;
	private int ticksSinceOpened;
	private int currentRow;
	private @Nullable SelectionManager selectionManager;

	public AbstractSignEditScreen(SignBlockEntity blockEntity, boolean front, boolean filtered) {
		this(blockEntity, front, filtered, Text.translatable("sign.edit"));
	}

	public AbstractSignEditScreen(SignBlockEntity blockEntity, boolean front, boolean filtered, Text title) {
		super(title);
		this.blockEntity = blockEntity;
		this.text = blockEntity.getText(front);
		this.front = front;
		signType = AbstractSignBlock.getWoodType(blockEntity.getCachedState().getBlock());
		messages = IntStream.range(0, SIGN_LINES)
			.mapToObj(line -> this.text.getMessage(line, filtered))
			.map(Text::getString)
			.toArray(String[]::new);
	}

	@Override
	protected void init() {
		addDrawableChild(
			ButtonWidget.builder(ScreenTexts.DONE, button -> finishEditing())
				.dimensions(width / 2 - 100, height / 4 + DONE_BUTTON_Y_OFFSET, DONE_BUTTON_WIDTH, DONE_BUTTON_HEIGHT)
				.build()
		);
		selectionManager = new SelectionManager(
			() -> messages[currentRow],
			this::setCurrentRowMessage,
			SelectionManager.makeClipboardGetter(client),
			SelectionManager.makeClipboardSetter(client),
			textLine -> client.textRenderer.getWidth(textLine) <= blockEntity.getMaxTextWidth()
		);
	}

	@Override
	public void tick() {
		ticksSinceOpened++;

		if (!canEdit()) {
			finishEditing();
		}
	}

	private boolean canEdit() {
		return client.player != null
			&& !blockEntity.isRemoved()
			&& !blockEntity.isPlayerTooFarToEdit(client.player.getUuid());
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.isUp()) {
			currentRow = currentRow - 1 & 3;
			selectionManager.putCursorAtEnd();
			return true;
		}

		if (input.isDown() || input.isEnter()) {
			currentRow = currentRow + 1 & 3;
			selectionManager.putCursorAtEnd();
			return true;
		}

		return selectionManager.handleSpecialKey(input) ? true : super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharInput input) {
		selectionManager.insert(input);
		return true;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, TITLE_Y, -1);
		renderSign(context);
	}

	@Override
	public void close() {
		finishEditing();
	}

	@Override
	public void removed() {
		ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();

		if (networkHandler == null) {
			return;
		}

		networkHandler.sendPacket(new UpdateSignC2SPacket(
			blockEntity.getPos(),
			front,
			messages[0],
			messages[1],
			messages[2],
			messages[3]
		));
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public boolean deferSubtitles() {
		return true;
	}

	protected abstract void renderSignBackground(DrawContext context);

	protected abstract Vector3f getTextScale();

	protected abstract float getYOffset();

	private void renderSign(DrawContext context) {
		context.getMatrices().pushMatrix();
		context.getMatrices().translate(width / 2.0F, getYOffset());
		context.getMatrices().pushMatrix();
		renderSignBackground(context);
		context.getMatrices().popMatrix();
		renderSignText(context);
		context.getMatrices().popMatrix();
	}

	private void renderSignText(DrawContext context) {
		Vector3f scale = getTextScale();
		context.getMatrices().scale(scale.x(), scale.y());

		int textColor = text.isGlowing()
			? text.getColor().getSignColor()
			: AbstractSignBlockEntityRenderer.getTextColor(text);

		boolean cursorVisible = ticksSinceOpened / CURSOR_BLINK_PERIOD % 2 == 0;
		int selectionStart = selectionManager.getSelectionStart();
		int selectionEnd = selectionManager.getSelectionEnd();
		int halfLinesHeight = SIGN_LINES * blockEntity.getTextLineHeight() / 2;
		int currentRowY = currentRow * blockEntity.getTextLineHeight() - halfLinesHeight;

		for (int lineIndex = 0; lineIndex < messages.length; lineIndex++) {
			String line = messages[lineIndex];

			if (line == null) {
				continue;
			}

			if (textRenderer.isRightToLeft()) {
				line = textRenderer.mirror(line);
			}

			int lineX = -textRenderer.getWidth(line) / 2;
			int lineY = lineIndex * blockEntity.getTextLineHeight() - halfLinesHeight;
			context.drawText(textRenderer, line, lineX, lineY, textColor, false);

			if (lineIndex == currentRow && selectionStart >= 0 && cursorVisible) {
				int cursorOffset = textRenderer.getWidth(line.substring(0, Math.max(Math.min(selectionStart, line.length()), 0)));
				int cursorX = cursorOffset - textRenderer.getWidth(line) / 2;

				if (selectionStart >= line.length()) {
					context.drawText(textRenderer, "_", cursorX, currentRowY, textColor, false);
				}
			}
		}

		for (int lineIndex = 0; lineIndex < messages.length; lineIndex++) {
			String line = messages[lineIndex];

			if (line == null || lineIndex != currentRow || selectionStart < 0) {
				continue;
			}

			int cursorOffset = textRenderer.getWidth(line.substring(0, Math.max(Math.min(selectionStart, line.length()), 0)));
			int cursorX = cursorOffset - textRenderer.getWidth(line) / 2;

			if (cursorVisible && selectionStart < line.length()) {
				context.fill(cursorX, currentRowY - 1, cursorX + 1, currentRowY + blockEntity.getTextLineHeight(), ColorHelper.fullAlpha(textColor));
			}

			if (selectionEnd != selectionStart) {
				int selStart = Math.min(selectionStart, selectionEnd);
				int selEnd = Math.max(selectionStart, selectionEnd);
				int selStartX = textRenderer.getWidth(line.substring(0, selStart)) - textRenderer.getWidth(line) / 2;
				int selEndX = textRenderer.getWidth(line.substring(0, selEnd)) - textRenderer.getWidth(line) / 2;
				int selLeft = Math.min(selStartX, selEndX);
				int selRight = Math.max(selStartX, selEndX);
				context.drawSelection(selLeft, currentRowY, selRight, currentRowY + blockEntity.getTextLineHeight(), true);
			}
		}
	}

	private void setCurrentRowMessage(String message) {
		messages[currentRow] = message;
		text = text.withMessage(currentRow, Text.literal(message));
		blockEntity.setText(text, front);
	}

	private void finishEditing() {
		client.setScreen(null);
	}
}
