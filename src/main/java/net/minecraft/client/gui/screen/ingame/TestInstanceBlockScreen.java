package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.block.entity.TestInstanceBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.ScrollableTextWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.packet.c2s.play.TestInstanceBlockActionC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.test.TestInstance;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Экран настройки блока тестового экземпляра. Управляет идентификатором теста,
 * размером, поворотом, сущностями и отображает статус выполнения теста.
 */
@Environment(EnvType.CLIENT)
public class TestInstanceBlockScreen extends Screen {

	private static final Text TEST_ID_TEXT = Text.translatable("test_instance_block.test_id");
	private static final Text SIZE_TEXT = Text.translatable("test_instance_block.size");
	private static final Text ENTITIES_TEXT = Text.translatable("test_instance_block.entities");
	private static final Text ROTATION_TEXT = Text.translatable("test_instance_block.rotation");

	private static final int PADDING = 8;
	private static final int CONTENT_WIDTH = 316;
	private static final int FIELD_HEIGHT = 20;
	private static final int FIELD_MAX_LENGTH = 128;
	private static final int COORD_FIELD_MAX_LENGTH = 15;
	private static final int STATUS_LINES = 8;
	private static final int LINE_HEIGHT = 9;
	private static final int TEST_ID_FIELD_Y = 40;
	private static final int STATUS_WIDGET_Y = 70;
	private static final int SIZE_ROW_Y = 160;
	private static final int ACTION_BUTTON_ROW_Y = 185;
	private static final int DONE_CANCEL_ROW_Y = 210;
	private static final int LABEL_Y_OFFSET = 150;
	private static final int TITLE_Y = 10;
	private static final int TEST_ID_LABEL_Y = 30;
	private static final int MIN_SIZE = 1;
	private static final int MAX_SIZE = 48;
	private static final int TEXT_COLOR_LABEL = -6250336;
	private static final int TEXT_COLOR_WHITE = -1;

	private final TestInstanceBlockEntity testInstanceBlockEntity;

	private @Nullable TextFieldWidget testIdTextField;
	private @Nullable TextFieldWidget sizeXField;
	private @Nullable TextFieldWidget sizeYField;
	private @Nullable TextFieldWidget sizeZField;
	private @Nullable ScrollableTextWidget statusWidget;
	private @Nullable ButtonWidget saveButton;
	private @Nullable ButtonWidget exportButton;
	private @Nullable CyclingButtonWidget<Boolean> entitiesButton;
	private @Nullable CyclingButtonWidget<BlockRotation> rotationButton;

	public TestInstanceBlockScreen(TestInstanceBlockEntity testInstanceBlockEntity) {
		super(testInstanceBlockEntity.getCachedState().getBlock().getName());
		this.testInstanceBlockEntity = testInstanceBlockEntity;
	}

	@Override
	protected void init() {
		int leftX = width / 2 - 158;
		boolean isDevelopment = SharedConstants.isDevelopment;
		int actionButtonCount = isDevelopment ? 3 : 2;
		int actionButtonWidth = getRoundedWidth(actionButtonCount);

		testIdTextField = new TextFieldWidget(textRenderer, leftX, TEST_ID_FIELD_Y, CONTENT_WIDTH, FIELD_HEIGHT, TEST_ID_TEXT);
		testIdTextField.setMaxLength(FIELD_MAX_LENGTH);

		Optional<RegistryKey<TestInstance>> testKey = testInstanceBlockEntity.getTestKey();
		if (testKey.isPresent()) {
			testIdTextField.setText(testKey.get().getValue().toString());
		}

		testIdTextField.setChangedListener(value -> refresh(false));
		addDrawableChild(testIdTextField);

		statusWidget = new ScrollableTextWidget(leftX, STATUS_WIDGET_Y, CONTENT_WIDTH, STATUS_LINES * LINE_HEIGHT, Text.literal(""), textRenderer);
		addDrawableChild(statusWidget);

		Vec3i currentSize = testInstanceBlockEntity.getSize();
		int columnIndex = 0;

		sizeXField = new TextFieldWidget(textRenderer, getX(columnIndex++, 5), SIZE_ROW_Y, getRoundedWidth(5), FIELD_HEIGHT, Text.translatable("structure_block.size.x"));
		sizeXField.setMaxLength(COORD_FIELD_MAX_LENGTH);
		addDrawableChild(sizeXField);

		sizeYField = new TextFieldWidget(textRenderer, getX(columnIndex++, 5), SIZE_ROW_Y, getRoundedWidth(5), FIELD_HEIGHT, Text.translatable("structure_block.size.y"));
		sizeYField.setMaxLength(COORD_FIELD_MAX_LENGTH);
		addDrawableChild(sizeYField);

		sizeZField = new TextFieldWidget(textRenderer, getX(columnIndex++, 5), SIZE_ROW_Y, getRoundedWidth(5), FIELD_HEIGHT, Text.translatable("structure_block.size.z"));
		sizeZField.setMaxLength(COORD_FIELD_MAX_LENGTH);
		addDrawableChild(sizeZField);

		setSize(currentSize);

		rotationButton = addDrawableChild(
			CyclingButtonWidget.builder(TestInstanceBlockScreen::rotationAsText, testInstanceBlockEntity.getRotation())
				.values(BlockRotation.values())
				.omitKeyText()
				.build(
					getX(columnIndex++, 5),
					SIZE_ROW_Y,
					getRoundedWidth(5),
					FIELD_HEIGHT,
					ROTATION_TEXT,
					(button, rotation) -> refresh()
				)
		);

		entitiesButton = addDrawableChild(
			CyclingButtonWidget.onOffBuilder(!testInstanceBlockEntity.shouldIgnoreEntities())
				.omitKeyText()
				.build(getX(columnIndex, 5), SIZE_ROW_Y, getRoundedWidth(5), FIELD_HEIGHT, ENTITIES_TEXT)
		);

		int actionIndex = 0;
		addDrawableChild(ButtonWidget.builder(
			Text.translatable("test_instance.action.reset"),
			button -> {
				executeAction(TestInstanceBlockActionC2SPacket.Action.RESET);
				client.setScreen(null);
			}
		).dimensions(getX(actionIndex++, actionButtonCount), ACTION_BUTTON_ROW_Y, actionButtonWidth, FIELD_HEIGHT).build());

		saveButton = addDrawableChild(ButtonWidget.builder(
			Text.translatable("test_instance.action.save"),
			button -> {
				executeAction(TestInstanceBlockActionC2SPacket.Action.SAVE);
				client.setScreen(null);
			}
		).dimensions(getX(actionIndex++, actionButtonCount), ACTION_BUTTON_ROW_Y, actionButtonWidth, FIELD_HEIGHT).build());

		if (isDevelopment) {
			exportButton = addDrawableChild(ButtonWidget.builder(
				Text.literal("Export Structure"),
				button -> {
					executeAction(TestInstanceBlockActionC2SPacket.Action.EXPORT);
					client.setScreen(null);
				}
			).dimensions(getX(actionIndex, actionButtonCount), ACTION_BUTTON_ROW_Y, actionButtonWidth, FIELD_HEIGHT).build());
		}

		addDrawableChild(ButtonWidget.builder(
			Text.translatable("test_instance.action.run"),
			button -> {
				executeAction(TestInstanceBlockActionC2SPacket.Action.RUN);
				client.setScreen(null);
			}
		).dimensions(getX(0, 3), DONE_CANCEL_ROW_Y, getRoundedWidth(3), FIELD_HEIGHT).build());

		addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> onDone())
			.dimensions(getX(1, 3), DONE_CANCEL_ROW_Y, getRoundedWidth(3), FIELD_HEIGHT)
			.build());

		addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, button -> onCancel())
			.dimensions(getX(2, 3), DONE_CANCEL_ROW_Y, getRoundedWidth(3), FIELD_HEIGHT)
			.build());

		refresh(true);
	}

	private void refresh() {
		boolean canSave = rotationButton.getValue() == BlockRotation.NONE
			&& Identifier.tryParse(testIdTextField.getText()) != null;

		saveButton.active = canSave;

		if (exportButton != null) {
			exportButton.active = canSave;
		}
	}

	private static Text rotationAsText(BlockRotation rotation) {
		return Text.literal(switch (rotation) {
			case NONE -> "0";
			case CLOCKWISE_90 -> "90";
			case CLOCKWISE_180 -> "180";
			case COUNTERCLOCKWISE_90 -> "270";
		});
	}

	private void setSize(Vec3i vec) {
		sizeXField.setText(Integer.toString(vec.getX()));
		sizeYField.setText(Integer.toString(vec.getY()));
		sizeZField.setText(Integer.toString(vec.getZ()));
	}

	private int getX(int index, int total) {
		int leftX = width / 2 - 158;
		float columnWidth = getWidth(total);
		return (int) (leftX + index * (PADDING + columnWidth));
	}

	private static int getRoundedWidth(int total) {
		return (int) getWidth(total);
	}

	private static float getWidth(int total) {
		return (float) (CONTENT_WIDTH - (total - 1) * PADDING) / total;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		int leftX = width / 2 - 158;

		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, TITLE_Y, TEXT_COLOR_WHITE);
		context.drawTextWithShadow(textRenderer, TEST_ID_TEXT, leftX, TEST_ID_LABEL_Y, TEXT_COLOR_LABEL);
		context.drawTextWithShadow(textRenderer, SIZE_TEXT, leftX, LABEL_Y_OFFSET, TEXT_COLOR_LABEL);
		context.drawTextWithShadow(textRenderer, ROTATION_TEXT, rotationButton.getX(), LABEL_Y_OFFSET, TEXT_COLOR_LABEL);
		context.drawTextWithShadow(textRenderer, ENTITIES_TEXT, entitiesButton.getX(), LABEL_Y_OFFSET, TEXT_COLOR_LABEL);
	}

	private void refresh(boolean initial) {
		TestInstanceBlockActionC2SPacket.Action action = initial
			? TestInstanceBlockActionC2SPacket.Action.INIT
			: TestInstanceBlockActionC2SPacket.Action.QUERY;

		boolean success = executeAction(action);
		if (!success) {
			statusWidget.setMessage(Text.translatable("test_instance.description.invalid_id").formatted(Formatting.RED));
		}

		refresh();
	}

	private void onDone() {
		executeAction(TestInstanceBlockActionC2SPacket.Action.SET);
		close();
	}

	/**
	 * Формирует и отправляет пакет действия с тестовым блоком.
	 * Возвращает {@code true}, если идентификатор теста корректен.
	 */
	private boolean executeAction(TestInstanceBlockActionC2SPacket.Action action) {
		Optional<Identifier> testId = Optional.ofNullable(Identifier.tryParse(testIdTextField.getText()));
		Optional<RegistryKey<TestInstance>> testKey = testId.map(id -> RegistryKey.of(RegistryKeys.TEST_INSTANCE, id));
		Vec3i size = new Vec3i(
			parse(sizeXField.getText()),
			parse(sizeYField.getText()),
			parse(sizeZField.getText())
		);
		boolean ignoreEntities = !entitiesButton.getValue();

		client.getNetworkHandler().sendPacket(new TestInstanceBlockActionC2SPacket(
			testInstanceBlockEntity.getPos(),
			action,
			testKey,
			size,
			rotationButton.getValue(),
			ignoreEntities
		));

		return testId.isPresent();
	}

	/**
	 * Обновляет виджет статуса сообщением об ошибке (если есть) и текущим статусом.
	 * При наличии размера в ответе обновляет поля ввода размера.
	 */
	public void handleStatus(Text status, Optional<Vec3i> size) {
		MutableText fullMessage = Text.empty();

		testInstanceBlockEntity.getErrorMessage().ifPresent(errorMessage ->
			fullMessage
				.append(Text.translatable("test_instance.description.failed", Text.empty().formatted(Formatting.RED).append(errorMessage)))
				.append("\n\n")
		);

		fullMessage.append(status);
		statusWidget.setMessage(fullMessage);
		size.ifPresent(this::setSize);
	}

	private void onCancel() {
		close();
	}

	private static int parse(String value) {
		try {
			return MathHelper.clamp(Integer.parseInt(value), MIN_SIZE, MAX_SIZE);
		} catch (NumberFormatException e) {
			return MIN_SIZE;
		}
	}

	@Override
	public boolean deferSubtitles() {
		return true;
	}
}
