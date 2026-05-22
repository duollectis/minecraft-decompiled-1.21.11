package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.network.packet.c2s.play.UpdateCommandBlockC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.world.CommandBlockExecutor;

/**
 * Экран редактирования командного блока. Добавляет кнопки выбора режима,
 * условного выполнения и триггера по редстоуну.
 */
@Environment(EnvType.CLIENT)
public class CommandBlockScreen extends AbstractCommandBlockScreen {

	private static final int TRACK_OUTPUT_BUTTON_HEIGHT = 135;
	private static final int MODE_BUTTON_WIDTH = 100;
	private static final int MODE_BUTTON_HEIGHT = 20;
	private static final int MODE_BUTTON_Y = 165;
	private static final int MODE_BUTTON_SPACING = 4;

	private final CommandBlockBlockEntity blockEntity;
	private CyclingButtonWidget<CommandBlockBlockEntity.Type> modeButton;
	private CyclingButtonWidget<Boolean> conditionalModeButton;
	private CyclingButtonWidget<Boolean> redstoneTriggerButton;
	private CommandBlockBlockEntity.Type mode = CommandBlockBlockEntity.Type.REDSTONE;
	private boolean conditional;
	private boolean autoActivate;

	public CommandBlockScreen(CommandBlockBlockEntity blockEntity) {
		this.blockEntity = blockEntity;
	}

	@Override
	CommandBlockExecutor getCommandExecutor() {
		return blockEntity.getCommandExecutor();
	}

	@Override
	int getTrackOutputButtonHeight() {
		return TRACK_OUTPUT_BUTTON_HEIGHT;
	}

	@Override
	protected void init() {
		super.init();
		setButtonsActive(false);
	}

	@Override
	protected void addAdditionalButtons() {
		modeButton = addDrawableChild(
			CyclingButtonWidget.<CommandBlockBlockEntity.Type>builder(
				type -> switch (type) {
					case SEQUENCE -> Text.translatable("advMode.mode.sequence");
					case AUTO -> Text.translatable("advMode.mode.auto");
					case REDSTONE -> Text.translatable("advMode.mode.redstone");
				},
				mode
			)
				.values(CommandBlockBlockEntity.Type.values())
				.omitKeyText()
				.build(
					width / 2 - 50 - MODE_BUTTON_WIDTH - MODE_BUTTON_SPACING,
					MODE_BUTTON_Y,
					MODE_BUTTON_WIDTH,
					MODE_BUTTON_HEIGHT,
					Text.translatable("advMode.mode"),
					(button, newMode) -> mode = newMode
				)
		);
		conditionalModeButton = addDrawableChild(
			CyclingButtonWidget.onOffBuilder(
				Text.translatable("advMode.mode.conditional"),
				Text.translatable("advMode.mode.unconditional"),
				conditional
			)
				.omitKeyText()
				.build(
					width / 2 - 50,
					MODE_BUTTON_Y,
					MODE_BUTTON_WIDTH,
					MODE_BUTTON_HEIGHT,
					Text.translatable("advMode.type"),
					(button, isConditional) -> conditional = isConditional
				)
		);
		redstoneTriggerButton = addDrawableChild(
			CyclingButtonWidget.onOffBuilder(
				Text.translatable("advMode.mode.autoexec.bat"),
				Text.translatable("advMode.mode.redstoneTriggered"),
				autoActivate
			)
				.omitKeyText()
				.build(
					width / 2 + 50 + MODE_BUTTON_SPACING,
					MODE_BUTTON_Y,
					MODE_BUTTON_WIDTH,
					MODE_BUTTON_HEIGHT,
					Text.translatable("advMode.triggering"),
					(button, isAutoActivate) -> autoActivate = isAutoActivate
				)
		);
	}

	private void setButtonsActive(boolean active) {
		doneButton.active = active;
		toggleTrackingOutputButton.active = active;
		modeButton.active = active;
		conditionalModeButton.active = active;
		redstoneTriggerButton.active = active;
	}

	/**
	 * Синхронизирует состояние блока с UI после получения данных от сервера.
	 * Вызывается при открытии экрана или обновлении блока.
	 */
	public void updateCommandBlock() {
		CommandBlockExecutor executor = blockEntity.getCommandExecutor();
		boolean trackingOutput = executor.isTrackingOutput();

		consoleCommandTextField.setText(executor.getCommand());
		mode = blockEntity.getCommandBlockType();
		conditional = blockEntity.isConditionalCommandBlock();
		autoActivate = blockEntity.isAuto();

		toggleTrackingOutputButton.setValue(trackingOutput);
		modeButton.setValue(mode);
		conditionalModeButton.setValue(conditional);
		redstoneTriggerButton.setValue(autoActivate);
		setPreviousOutputText(trackingOutput);
		setButtonsActive(true);
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		setButtonsActive(true);
	}

	@Override
	protected void syncSettingsToServer() {
		client.getNetworkHandler().sendPacket(new UpdateCommandBlockC2SPacket(
			blockEntity.getPos(),
			consoleCommandTextField.getText(),
			mode,
			blockEntity.getCommandExecutor().isTrackingOutput(),
			conditional,
			autoActivate
		));
	}
}
