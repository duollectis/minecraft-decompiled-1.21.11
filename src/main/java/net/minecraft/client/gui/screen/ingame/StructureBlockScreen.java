package net.minecraft.client.gui.screen.ingame;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.StructureBlockBlockEntity;
import net.minecraft.block.enums.StructureBlockMode;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.network.packet.c2s.play.UpdateStructureBlockC2SPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Экран настройки блока структуры. Управляет режимами SAVE/LOAD/CORNER/DATA,
 * полями ввода позиции, размера, целостности и метаданных.
 */
@Environment(EnvType.CLIENT)
public class StructureBlockScreen extends Screen {

	private static final Text STRUCTURE_NAME_TEXT = Text.translatable("structure_block.structure_name");
	private static final Text POSITION_TEXT = Text.translatable("structure_block.position");
	private static final Text SIZE_TEXT = Text.translatable("structure_block.size");
	private static final Text INTEGRITY_TEXT = Text.translatable("structure_block.integrity");
	private static final Text CUSTOM_DATA_TEXT = Text.translatable("structure_block.custom_data");
	private static final Text INCLUDE_ENTITIES_TEXT = Text.translatable("structure_block.include_entities");
	private static final Text STRICT_TEXT = Text.translatable("structure_block.strict");
	private static final Text DETECT_SIZE_TEXT = Text.translatable("structure_block.detect_size");
	private static final Text SHOW_AIR_TEXT = Text.translatable("structure_block.show_air");
	private static final Text SHOW_BOUNDING_BOX_TEXT = Text.translatable("structure_block.show_boundingbox");

	private static final ImmutableList<StructureBlockMode> MODES = ImmutableList.copyOf(StructureBlockMode.values());
	private static final ImmutableList<StructureBlockMode> MODES_EXCEPT_DATA = MODES.stream()
		.filter(mode -> mode != StructureBlockMode.DATA)
		.collect(ImmutableList.toImmutableList());

	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_ROW_Y = 210;
	private static final int SMALL_BUTTON_WIDTH = 50;
	private static final int SMALL_BUTTON_HEIGHT = 20;
	private static final int RIGHT_BUTTON_X_OFFSET = 4 + 100;
	private static final int MODE_BUTTON_Y = 185;
	private static final int DETECT_BUTTON_Y = 120;
	private static final int ENTITIES_BUTTON_Y = 160;
	private static final int SHOW_AIR_BUTTON_Y = 80;
	private static final int ROTATION_BUTTON_WIDTH = 40;
	private static final int NAME_FIELD_WIDTH = 300;
	private static final int COORD_FIELD_WIDTH = 80;
	private static final int NAME_FIELD_MAX_LENGTH = 128;
	private static final int COORD_FIELD_MAX_LENGTH = 15;
	private static final int SEED_FIELD_MAX_LENGTH = 31;
	private static final int METADATA_FIELD_WIDTH = 240;
	private static final int TEXT_COLOR_LABEL = -6250336;
	private static final int TEXT_COLOR_WHITE = -1;
	private static final float DEFAULT_INTEGRITY = 1.0F;

	private final StructureBlockBlockEntity structureBlock;
	private BlockMirror mirror = BlockMirror.NONE;
	private BlockRotation rotation = BlockRotation.NONE;
	private StructureBlockMode mode = StructureBlockMode.DATA;
	private boolean ignoreEntities;
	private boolean strict;
	private boolean showAir;
	private boolean showBoundingBox;

	private TextFieldWidget inputName;
	private TextFieldWidget inputPosX;
	private TextFieldWidget inputPosY;
	private TextFieldWidget inputPosZ;
	private TextFieldWidget inputSizeX;
	private TextFieldWidget inputSizeY;
	private TextFieldWidget inputSizeZ;
	private TextFieldWidget inputIntegrity;
	private TextFieldWidget inputSeed;
	private TextFieldWidget inputMetadata;

	private ButtonWidget buttonSave;
	private ButtonWidget buttonLoad;
	private ButtonWidget buttonRotate0;
	private ButtonWidget buttonRotate90;
	private ButtonWidget buttonRotate180;
	private ButtonWidget buttonRotate270;
	private ButtonWidget buttonDetect;
	private CyclingButtonWidget<Boolean> ignoreEntitiesButton;
	private CyclingButtonWidget<Boolean> strictButton;
	private CyclingButtonWidget<BlockMirror> mirrorButton;
	private CyclingButtonWidget<Boolean> showAirButton;
	private CyclingButtonWidget<Boolean> showBoundingBoxButton;

	private final DecimalFormat decimalFormat = new DecimalFormat("0.0###", DecimalFormatSymbols.getInstance(Locale.ROOT));

	public StructureBlockScreen(StructureBlockBlockEntity structureBlock) {
		super(Text.translatable(Blocks.STRUCTURE_BLOCK.getTranslationKey()));
		this.structureBlock = structureBlock;
	}

	private void done() {
		if (updateStructureBlock(StructureBlockBlockEntity.Action.UPDATE_DATA)) {
			client.setScreen(null);
		}
	}

	private void cancel() {
		structureBlock.setMirror(mirror);
		structureBlock.setRotation(rotation);
		structureBlock.setMode(mode);
		structureBlock.setIgnoreEntities(ignoreEntities);
		structureBlock.setStrict(strict);
		structureBlock.setShowAir(showAir);
		structureBlock.setShowBoundingBox(showBoundingBox);
		client.setScreen(null);
	}

	@Override
	protected void init() {
		addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> done())
			.dimensions(width / 2 - 4 - BUTTON_WIDTH, BUTTON_ROW_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());
		addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, button -> cancel())
			.dimensions(width / 2 + 4, BUTTON_ROW_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
			.build());

		mirror = structureBlock.getMirror();
		rotation = structureBlock.getRotation();
		mode = structureBlock.getMode();
		ignoreEntities = structureBlock.shouldIgnoreEntities();
		strict = structureBlock.isStrict();
		showAir = structureBlock.shouldShowAir();
		showBoundingBox = structureBlock.shouldShowBoundingBox();

		buttonSave = addDrawableChild(ButtonWidget.builder(
			Text.translatable("structure_block.button.save"),
			button -> {
				if (structureBlock.getMode() == StructureBlockMode.SAVE) {
					updateStructureBlock(StructureBlockBlockEntity.Action.SAVE_AREA);
					client.setScreen(null);
				}
			}
		).dimensions(width / 2 + 4 + RIGHT_BUTTON_X_OFFSET, MODE_BUTTON_Y, SMALL_BUTTON_WIDTH, SMALL_BUTTON_HEIGHT).build());

		buttonLoad = addDrawableChild(ButtonWidget.builder(
			Text.translatable("structure_block.button.load"),
			button -> {
				if (structureBlock.getMode() == StructureBlockMode.LOAD) {
					updateStructureBlock(StructureBlockBlockEntity.Action.LOAD_AREA);
					client.setScreen(null);
				}
			}
		).dimensions(width / 2 + 4 + RIGHT_BUTTON_X_OFFSET, MODE_BUTTON_Y, SMALL_BUTTON_WIDTH, SMALL_BUTTON_HEIGHT).build());

		addDrawableChild(
			CyclingButtonWidget.<StructureBlockMode>builder(
				value -> Text.translatable("structure_block.mode." + value.asString()), mode
			)
				.values(MODES_EXCEPT_DATA, MODES)
				.omitKeyText()
				.build(
					width / 2 - 4 - BUTTON_WIDTH,
					MODE_BUTTON_Y,
					SMALL_BUTTON_WIDTH,
					SMALL_BUTTON_HEIGHT,
					Text.literal("MODE"),
					(button, newMode) -> {
						structureBlock.setMode(newMode);
						updateWidgets(newMode);
					}
				)
		);

		buttonDetect = addDrawableChild(ButtonWidget.builder(
			Text.translatable("structure_block.button.detect_size"),
			button -> {
				if (structureBlock.getMode() == StructureBlockMode.SAVE) {
					updateStructureBlock(StructureBlockBlockEntity.Action.SCAN_AREA);
					client.setScreen(null);
				}
			}
		).dimensions(width / 2 + 4 + RIGHT_BUTTON_X_OFFSET, DETECT_BUTTON_Y, SMALL_BUTTON_WIDTH, SMALL_BUTTON_HEIGHT).build());

		ignoreEntitiesButton = addDrawableChild(
			CyclingButtonWidget.onOffBuilder(!structureBlock.shouldIgnoreEntities())
				.omitKeyText()
				.build(
					width / 2 + 4 + RIGHT_BUTTON_X_OFFSET,
					ENTITIES_BUTTON_Y,
					SMALL_BUTTON_WIDTH,
					SMALL_BUTTON_HEIGHT,
					INCLUDE_ENTITIES_TEXT,
					(button, includeEntities) -> structureBlock.setIgnoreEntities(!includeEntities)
				)
		);

		strictButton = addDrawableChild(
			CyclingButtonWidget.onOffBuilder(structureBlock.isStrict())
				.omitKeyText()
				.build(
					width / 2 + 4 + RIGHT_BUTTON_X_OFFSET,
					DETECT_BUTTON_Y,
					SMALL_BUTTON_WIDTH,
					SMALL_BUTTON_HEIGHT,
					STRICT_TEXT,
					(button, isStrict) -> structureBlock.setStrict(isStrict)
				)
		);

		mirrorButton = addDrawableChild(
			CyclingButtonWidget.builder(BlockMirror::getName, mirror)
				.values(BlockMirror.values())
				.omitKeyText()
				.build(
					width / 2 - 20,
					MODE_BUTTON_Y,
					40,
					SMALL_BUTTON_HEIGHT,
					Text.literal("MIRROR"),
					(button, newMirror) -> structureBlock.setMirror(newMirror)
				)
		);

		showAirButton = addDrawableChild(
			CyclingButtonWidget.onOffBuilder(structureBlock.shouldShowAir())
				.omitKeyText()
				.build(
					width / 2 + 4 + RIGHT_BUTTON_X_OFFSET,
					SHOW_AIR_BUTTON_Y,
					SMALL_BUTTON_WIDTH,
					SMALL_BUTTON_HEIGHT,
					SHOW_AIR_TEXT,
					(button, isShowAir) -> structureBlock.setShowAir(isShowAir)
				)
		);

		showBoundingBoxButton = addDrawableChild(
			CyclingButtonWidget.onOffBuilder(structureBlock.shouldShowBoundingBox())
				.omitKeyText()
				.build(
					width / 2 + 4 + RIGHT_BUTTON_X_OFFSET,
					SHOW_AIR_BUTTON_Y,
					SMALL_BUTTON_WIDTH,
					SMALL_BUTTON_HEIGHT,
					SHOW_BOUNDING_BOX_TEXT,
					(button, isShowBoundingBox) -> structureBlock.setShowBoundingBox(isShowBoundingBox)
				)
		);

		buttonRotate0 = addDrawableChild(ButtonWidget.builder(
			Text.literal("0"),
			button -> {
				structureBlock.setRotation(BlockRotation.NONE);
				updateRotationButton();
			}
		).dimensions(width / 2 - 1 - ROTATION_BUTTON_WIDTH - 1 - ROTATION_BUTTON_WIDTH - 20, MODE_BUTTON_Y, ROTATION_BUTTON_WIDTH, SMALL_BUTTON_HEIGHT).build());

		buttonRotate90 = addDrawableChild(ButtonWidget.builder(
			Text.literal("90"),
			button -> {
				structureBlock.setRotation(BlockRotation.CLOCKWISE_90);
				updateRotationButton();
			}
		).dimensions(width / 2 - 1 - ROTATION_BUTTON_WIDTH - 20, MODE_BUTTON_Y, ROTATION_BUTTON_WIDTH, SMALL_BUTTON_HEIGHT).build());

		buttonRotate180 = addDrawableChild(ButtonWidget.builder(
			Text.literal("180"),
			button -> {
				structureBlock.setRotation(BlockRotation.CLOCKWISE_180);
				updateRotationButton();
			}
		).dimensions(width / 2 + 1 + 20, MODE_BUTTON_Y, ROTATION_BUTTON_WIDTH, SMALL_BUTTON_HEIGHT).build());

		buttonRotate270 = addDrawableChild(ButtonWidget.builder(
			Text.literal("270"),
			button -> {
				structureBlock.setRotation(BlockRotation.COUNTERCLOCKWISE_90);
				updateRotationButton();
			}
		).dimensions(width / 2 + 1 + ROTATION_BUTTON_WIDTH + 1 + 20, MODE_BUTTON_Y, ROTATION_BUTTON_WIDTH, SMALL_BUTTON_HEIGHT).build());

		inputName = new TextFieldWidget(textRenderer, width / 2 - 152, 40, NAME_FIELD_WIDTH, SMALL_BUTTON_HEIGHT, STRUCTURE_NAME_TEXT) {
			@Override
			public boolean charTyped(CharInput input) {
				return !StructureBlockScreen.this.isValidCharacterForName(getText(), input.codepoint(), getCursor())
					? false
					: super.charTyped(input);
			}
		};
		inputName.setMaxLength(NAME_FIELD_MAX_LENGTH);
		inputName.setText(structureBlock.getTemplateName());
		addSelectableChild(inputName);

		BlockPos offset = structureBlock.getOffset();
		inputPosX = new TextFieldWidget(textRenderer, width / 2 - 152, 80, COORD_FIELD_WIDTH, SMALL_BUTTON_HEIGHT, Text.translatable("structure_block.position.x"));
		inputPosX.setMaxLength(COORD_FIELD_MAX_LENGTH);
		inputPosX.setText(Integer.toString(offset.getX()));
		addSelectableChild(inputPosX);

		inputPosY = new TextFieldWidget(textRenderer, width / 2 - 72, 80, COORD_FIELD_WIDTH, SMALL_BUTTON_HEIGHT, Text.translatable("structure_block.position.y"));
		inputPosY.setMaxLength(COORD_FIELD_MAX_LENGTH);
		inputPosY.setText(Integer.toString(offset.getY()));
		addSelectableChild(inputPosY);

		inputPosZ = new TextFieldWidget(textRenderer, width / 2 + 8, 80, COORD_FIELD_WIDTH, SMALL_BUTTON_HEIGHT, Text.translatable("structure_block.position.z"));
		inputPosZ.setMaxLength(COORD_FIELD_MAX_LENGTH);
		inputPosZ.setText(Integer.toString(offset.getZ()));
		addSelectableChild(inputPosZ);

		Vec3i size = structureBlock.getSize();
		inputSizeX = new TextFieldWidget(textRenderer, width / 2 - 152, 120, COORD_FIELD_WIDTH, SMALL_BUTTON_HEIGHT, Text.translatable("structure_block.size.x"));
		inputSizeX.setMaxLength(COORD_FIELD_MAX_LENGTH);
		inputSizeX.setText(Integer.toString(size.getX()));
		addSelectableChild(inputSizeX);

		inputSizeY = new TextFieldWidget(textRenderer, width / 2 - 72, 120, COORD_FIELD_WIDTH, SMALL_BUTTON_HEIGHT, Text.translatable("structure_block.size.y"));
		inputSizeY.setMaxLength(COORD_FIELD_MAX_LENGTH);
		inputSizeY.setText(Integer.toString(size.getY()));
		addSelectableChild(inputSizeY);

		inputSizeZ = new TextFieldWidget(textRenderer, width / 2 + 8, 120, COORD_FIELD_WIDTH, SMALL_BUTTON_HEIGHT, Text.translatable("structure_block.size.z"));
		inputSizeZ.setMaxLength(COORD_FIELD_MAX_LENGTH);
		inputSizeZ.setText(Integer.toString(size.getZ()));
		addSelectableChild(inputSizeZ);

		inputIntegrity = new TextFieldWidget(textRenderer, width / 2 - 152, 120, COORD_FIELD_WIDTH, SMALL_BUTTON_HEIGHT, Text.translatable("structure_block.integrity.integrity"));
		inputIntegrity.setMaxLength(COORD_FIELD_MAX_LENGTH);
		inputIntegrity.setText(decimalFormat.format(structureBlock.getIntegrity()));
		addSelectableChild(inputIntegrity);

		inputSeed = new TextFieldWidget(textRenderer, width / 2 - 72, 120, COORD_FIELD_WIDTH, SMALL_BUTTON_HEIGHT, Text.translatable("structure_block.integrity.seed"));
		inputSeed.setMaxLength(SEED_FIELD_MAX_LENGTH);
		inputSeed.setText(Long.toString(structureBlock.getSeed()));
		addSelectableChild(inputSeed);

		inputMetadata = new TextFieldWidget(textRenderer, width / 2 - 152, 120, METADATA_FIELD_WIDTH, SMALL_BUTTON_HEIGHT, Text.translatable("structure_block.custom_data"));
		inputMetadata.setMaxLength(NAME_FIELD_MAX_LENGTH);
		inputMetadata.setText(structureBlock.getMetadata());
		addSelectableChild(inputMetadata);

		updateRotationButton();
		updateWidgets(mode);
	}

	@Override
	protected void setInitialFocus() {
		setInitialFocus(inputName);
	}

	@Override
	public void resize(int width, int height) {
		String savedName = inputName.getText();
		String savedPosX = inputPosX.getText();
		String savedPosY = inputPosY.getText();
		String savedPosZ = inputPosZ.getText();
		String savedSizeX = inputSizeX.getText();
		String savedSizeY = inputSizeY.getText();
		String savedSizeZ = inputSizeZ.getText();
		String savedIntegrity = inputIntegrity.getText();
		String savedSeed = inputSeed.getText();
		String savedMetadata = inputMetadata.getText();

		init(width, height);

		inputName.setText(savedName);
		inputPosX.setText(savedPosX);
		inputPosY.setText(savedPosY);
		inputPosZ.setText(savedPosZ);
		inputSizeX.setText(savedSizeX);
		inputSizeY.setText(savedSizeY);
		inputSizeZ.setText(savedSizeZ);
		inputIntegrity.setText(savedIntegrity);
		inputSeed.setText(savedSeed);
		inputMetadata.setText(savedMetadata);
	}

	private void updateRotationButton() {
		buttonRotate0.active = true;
		buttonRotate90.active = true;
		buttonRotate180.active = true;
		buttonRotate270.active = true;

		switch (structureBlock.getRotation()) {
			case NONE -> buttonRotate0.active = false;
			case CLOCKWISE_180 -> buttonRotate180.active = false;
			case COUNTERCLOCKWISE_90 -> buttonRotate270.active = false;
			case CLOCKWISE_90 -> buttonRotate90.active = false;
		}
	}

	private void updateWidgets(StructureBlockMode currentMode) {
		inputName.setVisible(false);
		inputPosX.setVisible(false);
		inputPosY.setVisible(false);
		inputPosZ.setVisible(false);
		inputSizeX.setVisible(false);
		inputSizeY.setVisible(false);
		inputSizeZ.setVisible(false);
		inputIntegrity.setVisible(false);
		inputSeed.setVisible(false);
		inputMetadata.setVisible(false);
		buttonSave.visible = false;
		buttonLoad.visible = false;
		buttonDetect.visible = false;
		ignoreEntitiesButton.visible = false;
		strictButton.visible = false;
		mirrorButton.visible = false;
		buttonRotate0.visible = false;
		buttonRotate90.visible = false;
		buttonRotate180.visible = false;
		buttonRotate270.visible = false;
		showAirButton.visible = false;
		showBoundingBoxButton.visible = false;

		switch (currentMode) {
			case SAVE -> {
				inputName.setVisible(true);
				inputPosX.setVisible(true);
				inputPosY.setVisible(true);
				inputPosZ.setVisible(true);
				inputSizeX.setVisible(true);
				inputSizeY.setVisible(true);
				inputSizeZ.setVisible(true);
				buttonSave.visible = true;
				buttonDetect.visible = true;
				ignoreEntitiesButton.visible = true;
				showAirButton.visible = true;
			}
			case LOAD -> {
				inputName.setVisible(true);
				inputPosX.setVisible(true);
				inputPosY.setVisible(true);
				inputPosZ.setVisible(true);
				inputIntegrity.setVisible(true);
				inputSeed.setVisible(true);
				buttonLoad.visible = true;
				ignoreEntitiesButton.visible = true;
				strictButton.visible = true;
				mirrorButton.visible = true;
				buttonRotate0.visible = true;
				buttonRotate90.visible = true;
				buttonRotate180.visible = true;
				buttonRotate270.visible = true;
				showBoundingBoxButton.visible = true;
				updateRotationButton();
			}
			case CORNER -> inputName.setVisible(true);
			case DATA -> inputMetadata.setVisible(true);
		}
	}

	/**
	 * Формирует и отправляет пакет обновления блока структуры на сервер.
	 * Парсит все поля ввода с защитой от некорректных значений.
	 */
	private boolean updateStructureBlock(StructureBlockBlockEntity.Action action) {
		BlockPos offset = new BlockPos(
			parseInt(inputPosX.getText()),
			parseInt(inputPosY.getText()),
			parseInt(inputPosZ.getText())
		);
		Vec3i size = new Vec3i(
			parseInt(inputSizeX.getText()),
			parseInt(inputSizeY.getText()),
			parseInt(inputSizeZ.getText())
		);
		float integrity = parseFloat(inputIntegrity.getText());
		long seed = parseLong(inputSeed.getText());

		client.getNetworkHandler().sendPacket(new UpdateStructureBlockC2SPacket(
			structureBlock.getPos(),
			action,
			structureBlock.getMode(),
			inputName.getText(),
			offset,
			size,
			structureBlock.getMirror(),
			structureBlock.getRotation(),
			inputMetadata.getText(),
			structureBlock.shouldIgnoreEntities(),
			structureBlock.isStrict(),
			structureBlock.shouldShowAir(),
			structureBlock.shouldShowBoundingBox(),
			integrity,
			seed
		));

		return true;
	}

	private long parseLong(String value) {
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private float parseFloat(String value) {
		try {
			return Float.valueOf(value);
		} catch (NumberFormatException e) {
			return DEFAULT_INTEGRITY;
		}
	}

	private int parseInt(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	@Override
	public void close() {
		cancel();
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (super.keyPressed(input)) {
			return true;
		}

		if (input.isEnter()) {
			done();
			return true;
		}

		return false;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		StructureBlockMode currentMode = structureBlock.getMode();
		int centerX = width / 2;

		context.drawCenteredTextWithShadow(textRenderer, title, centerX, 10, TEXT_COLOR_WHITE);

		if (currentMode != StructureBlockMode.DATA) {
			context.drawTextWithShadow(textRenderer, STRUCTURE_NAME_TEXT, centerX - 153, 30, TEXT_COLOR_LABEL);
			inputName.render(context, mouseX, mouseY, deltaTicks);
		}

		if (currentMode == StructureBlockMode.LOAD || currentMode == StructureBlockMode.SAVE) {
			context.drawTextWithShadow(textRenderer, POSITION_TEXT, centerX - 153, 70, TEXT_COLOR_LABEL);
			inputPosX.render(context, mouseX, mouseY, deltaTicks);
			inputPosY.render(context, mouseX, mouseY, deltaTicks);
			inputPosZ.render(context, mouseX, mouseY, deltaTicks);
			context.drawTextWithShadow(
				textRenderer,
				INCLUDE_ENTITIES_TEXT,
				centerX + 154 - textRenderer.getWidth(INCLUDE_ENTITIES_TEXT),
				150,
				TEXT_COLOR_LABEL
			);
		}

		if (currentMode == StructureBlockMode.SAVE) {
			context.drawTextWithShadow(textRenderer, SIZE_TEXT, centerX - 153, 110, TEXT_COLOR_LABEL);
			inputSizeX.render(context, mouseX, mouseY, deltaTicks);
			inputSizeY.render(context, mouseX, mouseY, deltaTicks);
			inputSizeZ.render(context, mouseX, mouseY, deltaTicks);
			context.drawTextWithShadow(
				textRenderer,
				DETECT_SIZE_TEXT,
				centerX + 154 - textRenderer.getWidth(DETECT_SIZE_TEXT),
				110,
				TEXT_COLOR_LABEL
			);
			context.drawTextWithShadow(
				textRenderer,
				SHOW_AIR_TEXT,
				centerX + 154 - textRenderer.getWidth(SHOW_AIR_TEXT),
				70,
				TEXT_COLOR_LABEL
			);
		}

		if (currentMode == StructureBlockMode.LOAD) {
			context.drawTextWithShadow(textRenderer, INTEGRITY_TEXT, centerX - 153, 110, TEXT_COLOR_LABEL);
			inputIntegrity.render(context, mouseX, mouseY, deltaTicks);
			inputSeed.render(context, mouseX, mouseY, deltaTicks);
			context.drawTextWithShadow(
				textRenderer,
				STRICT_TEXT,
				centerX + 154 - textRenderer.getWidth(STRICT_TEXT),
				110,
				TEXT_COLOR_LABEL
			);
			context.drawTextWithShadow(
				textRenderer,
				SHOW_BOUNDING_BOX_TEXT,
				centerX + 154 - textRenderer.getWidth(SHOW_BOUNDING_BOX_TEXT),
				70,
				TEXT_COLOR_LABEL
			);
		}

		if (currentMode == StructureBlockMode.DATA) {
			context.drawTextWithShadow(textRenderer, CUSTOM_DATA_TEXT, centerX - 153, 110, TEXT_COLOR_LABEL);
			inputMetadata.render(context, mouseX, mouseY, deltaTicks);
		}

		context.drawTextWithShadow(textRenderer, currentMode.asText(), centerX - 153, 174, TEXT_COLOR_LABEL);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public boolean deferSubtitles() {
		return true;
	}
}
