package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.JigsawBlock;
import net.minecraft.block.entity.JigsawBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.network.packet.c2s.play.JigsawGeneratingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateJigsawC2SPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Экран редактирования блока пазла (Jigsaw). Позволяет настраивать пул структур,
 * имена соединений, финальное состояние и параметры генерации.
 */
@Environment(EnvType.CLIENT)
public class JigsawBlockScreen extends Screen {

	private static final Text JOINT_LABEL_TEXT = Text.translatable("jigsaw_block.joint_label");
	private static final Text POOL_TEXT = Text.translatable("jigsaw_block.pool");
	private static final Text NAME_TEXT = Text.translatable("jigsaw_block.name");
	private static final Text TARGET_TEXT = Text.translatable("jigsaw_block.target");
	private static final Text FINAL_STATE_TEXT = Text.translatable("jigsaw_block.final_state");
	private static final Text PLACEMENT_PRIORITY_TEXT = Text.translatable("jigsaw_block.placement_priority");
	private static final Text PLACEMENT_PRIORITY_TOOLTIP = Text.translatable("jigsaw_block.placement_priority.tooltip");
	private static final Text SELECTION_PRIORITY_TEXT = Text.translatable("jigsaw_block.selection_priority");
	private static final Text SELECTION_PRIORITY_TOOLTIP = Text.translatable("jigsaw_block.selection_priority.tooltip");
	private static final int FIELD_WIDTH = 300;
	private static final int FIELD_HEIGHT = 20;
	private static final int PRIORITY_FIELD_WIDTH = 98;
	private static final int PRIORITY_FIELD_MAX_LENGTH = 3;
	private static final int FIELD_MAX_LENGTH = 128;
	private static final int FINAL_STATE_MAX_LENGTH = 256;
	private static final int MAX_GENERATION_DEPTH = 20;
	private static final int BUTTON_PRESS_PACKET_OFFSET = 100;
	private static final int TEXT_COLOR_LABEL = -6250336;

	private final JigsawBlockEntity jigsaw;
	private TextFieldWidget nameField;
	private TextFieldWidget targetField;
	private TextFieldWidget poolField;
	private TextFieldWidget finalStateField;
	private TextFieldWidget selectionPriorityField;
	private TextFieldWidget placementPriorityField;
	int generationDepth;
	private boolean keepJigsaws = true;
	private CyclingButtonWidget<JigsawBlockEntity.Joint> jointRotationButton;
	private ButtonWidget doneButton;
	private ButtonWidget generateButton;
	private JigsawBlockEntity.Joint joint;

	public JigsawBlockScreen(JigsawBlockEntity jigsaw) {
		super(NarratorManager.EMPTY);
		this.jigsaw = jigsaw;
	}

	private void onDone() {
		updateServer();
		client.setScreen(null);
	}

	private void onCancel() {
		client.setScreen(null);
	}

	private void updateServer() {
		client.getNetworkHandler().sendPacket(new UpdateJigsawC2SPacket(
			jigsaw.getPos(),
			Identifier.of(nameField.getText()),
			Identifier.of(targetField.getText()),
			Identifier.of(poolField.getText()),
			finalStateField.getText(),
			joint,
			parseInt(selectionPriorityField.getText()),
			parseInt(placementPriorityField.getText())
		));
	}

	private int parseInt(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private void generate() {
		client.getNetworkHandler().sendPacket(new JigsawGeneratingC2SPacket(
			jigsaw.getPos(),
			generationDepth,
			keepJigsaws
		));
	}

	@Override
	public void close() {
		onCancel();
	}

	@Override
	protected void init() {
		int centerX = width / 2;

		poolField = new TextFieldWidget(textRenderer, centerX - 153, 20, FIELD_WIDTH, FIELD_HEIGHT, POOL_TEXT);
		poolField.setMaxLength(FIELD_MAX_LENGTH);
		poolField.setText(jigsaw.getPool().getValue().toString());
		poolField.setChangedListener(pool -> updateDoneButtonState());
		addSelectableChild(poolField);

		nameField = new TextFieldWidget(textRenderer, centerX - 153, 55, FIELD_WIDTH, FIELD_HEIGHT, NAME_TEXT);
		nameField.setMaxLength(FIELD_MAX_LENGTH);
		nameField.setText(jigsaw.getName().toString());
		nameField.setChangedListener(name -> updateDoneButtonState());
		addSelectableChild(nameField);

		targetField = new TextFieldWidget(textRenderer, centerX - 153, 90, FIELD_WIDTH, FIELD_HEIGHT, TARGET_TEXT);
		targetField.setMaxLength(FIELD_MAX_LENGTH);
		targetField.setText(jigsaw.getTarget().toString());
		targetField.setChangedListener(target -> updateDoneButtonState());
		addSelectableChild(targetField);

		finalStateField = new TextFieldWidget(textRenderer, centerX - 153, 125, FIELD_WIDTH, FIELD_HEIGHT, FINAL_STATE_TEXT);
		finalStateField.setMaxLength(FINAL_STATE_MAX_LENGTH);
		finalStateField.setText(jigsaw.getFinalState());
		addSelectableChild(finalStateField);

		selectionPriorityField = new TextFieldWidget(textRenderer, centerX - 153, 160, PRIORITY_FIELD_WIDTH, FIELD_HEIGHT, SELECTION_PRIORITY_TEXT);
		selectionPriorityField.setMaxLength(PRIORITY_FIELD_MAX_LENGTH);
		selectionPriorityField.setText(Integer.toString(jigsaw.getSelectionPriority()));
		selectionPriorityField.setTooltip(Tooltip.of(SELECTION_PRIORITY_TOOLTIP));
		addSelectableChild(selectionPriorityField);

		placementPriorityField = new TextFieldWidget(textRenderer, centerX - 50, 160, PRIORITY_FIELD_WIDTH, FIELD_HEIGHT, PLACEMENT_PRIORITY_TEXT);
		placementPriorityField.setMaxLength(PRIORITY_FIELD_MAX_LENGTH);
		placementPriorityField.setText(Integer.toString(jigsaw.getPlacementPriority()));
		placementPriorityField.setTooltip(Tooltip.of(PLACEMENT_PRIORITY_TOOLTIP));
		addSelectableChild(placementPriorityField);

		joint = jigsaw.getJoint();
		jointRotationButton = addDrawableChild(
			CyclingButtonWidget.builder(JigsawBlockEntity.Joint::asText, joint)
				.values(JigsawBlockEntity.Joint.values())
				.omitKeyText()
				.build(
					centerX + 54,
					160,
					100,
					FIELD_HEIGHT,
					JOINT_LABEL_TEXT,
					(button, newJoint) -> joint = newJoint
				)
		);

		boolean isVertical = JigsawBlock.getFacing(jigsaw.getCachedState()).getAxis().isVertical();
		jointRotationButton.active = isVertical;
		jointRotationButton.visible = isVertical;

		addDrawableChild(new SliderWidget(centerX - 154, 185, 100, FIELD_HEIGHT, ScreenTexts.EMPTY, 0.0) {
			{
				updateMessage();
			}

			@Override
			protected void updateMessage() {
				setMessage(Text.translatable("jigsaw_block.levels", JigsawBlockScreen.this.generationDepth));
			}

			@Override
			protected void applyValue() {
				JigsawBlockScreen.this.generationDepth = MathHelper.floor(MathHelper.clampedLerp(value, 0.0, MAX_GENERATION_DEPTH));
			}
		});

		addDrawableChild(
			CyclingButtonWidget.onOffBuilder(keepJigsaws)
				.build(
					centerX - 50,
					185,
					100,
					FIELD_HEIGHT,
					Text.translatable("jigsaw_block.keep_jigsaws"),
					(button, keep) -> keepJigsaws = keep
				)
		);

		generateButton = addDrawableChild(
			ButtonWidget.builder(Text.translatable("jigsaw_block.generate"), button -> {
				onDone();
				generate();
			}).dimensions(centerX + 54, 185, 100, FIELD_HEIGHT).build()
		);
		doneButton = addDrawableChild(
			ButtonWidget.builder(ScreenTexts.DONE, button -> onDone())
				.dimensions(centerX - 4 - 150, 210, 150, FIELD_HEIGHT)
				.build()
		);
		addDrawableChild(
			ButtonWidget.builder(ScreenTexts.CANCEL, button -> onCancel())
				.dimensions(centerX + 4, 210, 150, FIELD_HEIGHT)
				.build()
		);
		updateDoneButtonState();
	}

	@Override
	protected void setInitialFocus() {
		setInitialFocus(poolField);
	}

	public static boolean isValidId(String id) {
		return Identifier.tryParse(id) != null;
	}

	private void updateDoneButtonState() {
		boolean allValid = isValidId(nameField.getText())
			&& isValidId(targetField.getText())
			&& isValidId(poolField.getText());
		doneButton.active = allValid;
		generateButton.active = allValid;
	}

	@Override
	public boolean deferSubtitles() {
		return true;
	}

	@Override
	public void resize(int width, int height) {
		String savedName = nameField.getText();
		String savedTarget = targetField.getText();
		String savedPool = poolField.getText();
		String savedFinalState = finalStateField.getText();
		String savedSelectionPriority = selectionPriorityField.getText();
		String savedPlacementPriority = placementPriorityField.getText();
		int savedDepth = generationDepth;
		JigsawBlockEntity.Joint savedJoint = joint;

		init(width, height);

		nameField.setText(savedName);
		targetField.setText(savedTarget);
		poolField.setText(savedPool);
		finalStateField.setText(savedFinalState);
		generationDepth = savedDepth;
		joint = savedJoint;
		jointRotationButton.setValue(savedJoint);
		selectionPriorityField.setText(savedSelectionPriority);
		placementPriorityField.setText(savedPlacementPriority);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (super.keyPressed(input)) {
			return true;
		}

		if (doneButton.active && input.isEnter()) {
			onDone();
			return true;
		}

		return false;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		int centerX = width / 2;

		context.drawTextWithShadow(textRenderer, POOL_TEXT, centerX - 153, 10, TEXT_COLOR_LABEL);
		poolField.render(context, mouseX, mouseY, deltaTicks);
		context.drawTextWithShadow(textRenderer, NAME_TEXT, centerX - 153, 45, TEXT_COLOR_LABEL);
		nameField.render(context, mouseX, mouseY, deltaTicks);
		context.drawTextWithShadow(textRenderer, TARGET_TEXT, centerX - 153, 80, TEXT_COLOR_LABEL);
		targetField.render(context, mouseX, mouseY, deltaTicks);
		context.drawTextWithShadow(textRenderer, FINAL_STATE_TEXT, centerX - 153, 115, TEXT_COLOR_LABEL);
		finalStateField.render(context, mouseX, mouseY, deltaTicks);
		context.drawTextWithShadow(textRenderer, SELECTION_PRIORITY_TEXT, centerX - 153, 150, TEXT_COLOR_LABEL);
		placementPriorityField.render(context, mouseX, mouseY, deltaTicks);
		context.drawTextWithShadow(textRenderer, PLACEMENT_PRIORITY_TEXT, centerX - 50, 150, TEXT_COLOR_LABEL);
		selectionPriorityField.render(context, mouseX, mouseY, deltaTicks);

		if (JigsawBlock.getFacing(jigsaw.getCachedState()).getAxis().isVertical()) {
			context.drawTextWithShadow(textRenderer, JOINT_LABEL_TEXT, centerX + 53, 150, TEXT_COLOR_LABEL);
		}
	}
}
