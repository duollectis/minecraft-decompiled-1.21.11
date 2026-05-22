package net.minecraft.client.gui.screen.dialog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.dialog.AfterAction;
import net.minecraft.dialog.body.DialogBody;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.dialog.type.DialogInput;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Базовый экран диалога, отображающий тело, входные поля и кнопки действий из {@link Dialog}.
 * Содержит кнопку предупреждения о кастомных экранах сервера.
 */
@Environment(EnvType.CLIENT)
public abstract class DialogScreen<T extends Dialog> extends Screen {

	public static final Text
			CUSTOM_SCREEN_REJECTED_DISCONNECT_TEXT =
			Text.translatable("menu.custom_screen_info.disconnect");
	private static final int BUTTON_MARGIN = 20;
	private static final ButtonTextures WARNING_BUTTON_TEXTURES = new ButtonTextures(
			Identifier.ofVanilla("dialog/warning_button"),
			Identifier.ofVanilla("dialog/warning_button_disabled"),
			Identifier.ofVanilla("dialog/warning_button_highlighted")
	);
	private final T dialog;
	private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);
	private final @Nullable Screen parent;
	private @Nullable ScrollableLayoutWidget contents;
	private ButtonWidget warningButton;
	private final DialogNetworkAccess networkAccess;
	private Supplier<Optional<ClickEvent>> cancelAction = DialogControls.EMPTY_ACTION_CLICK_EVENT;

	public DialogScreen(@Nullable Screen parent, T dialog, DialogNetworkAccess networkAccess) {
		super(dialog.common().title());
		this.dialog = dialog;
		this.parent = parent;
		this.networkAccess = networkAccess;
	}

	@Override
	protected final void init() {
		super.init();
		warningButton = createWarningButton();
		warningButton.setNavigationOrder(-10);
		DialogControls dialogControls = new DialogControls(this);
		DirectionalLayoutWidget bodyLayout = DirectionalLayoutWidget.vertical().spacing(10);
		bodyLayout.getMainPositioner().alignHorizontalCenter();
		layout.addHeader(createHeader());

		for (DialogBody dialogBody : dialog.common().body()) {
			Widget widget = DialogBodyHandlers.createWidget(this, dialogBody);
			if (widget != null) {
				bodyLayout.add(widget);
			}
		}

		for (DialogInput dialogInput : dialog.common().inputs()) {
			dialogControls.addInput(dialogInput, bodyLayout::add);
		}

		initBody(bodyLayout, dialogControls, dialog, networkAccess);
		contents = new ScrollableLayoutWidget(client, bodyLayout, layout.getContentHeight());
		layout.addBody(contents);
		initHeaderAndFooter(layout, dialogControls, dialog, networkAccess);
		cancelAction = dialogControls.createClickEvent(dialog.getCancelAction());
		layout.forEachChild(child -> {
			if (child != warningButton) {
				addDrawableChild(child);
			}
		});
		addDrawableChild(warningButton);
		refreshWidgetPositions();
	}

	protected void initBody(
			DirectionalLayoutWidget bodyLayout,
			DialogControls controls,
			T dialog,
			DialogNetworkAccess networkAccess
	) {
	}

	protected void initHeaderAndFooter(
			ThreePartsLayoutWidget layout,
			DialogControls controls,
			T dialog,
			DialogNetworkAccess networkAccess
	) {
	}

	@Override
	protected void refreshWidgetPositions() {
		contents.setHeight(layout.getContentHeight());
		layout.refreshPositions();
		resetWarningButtonPosition();
	}

	protected Widget createHeader() {
		DirectionalLayoutWidget headerRow = DirectionalLayoutWidget.horizontal().spacing(10);
		headerRow.getMainPositioner().alignHorizontalCenter().alignVerticalCenter();
		headerRow.add(new TextWidget(title, textRenderer));
		headerRow.add(warningButton);
		return headerRow;
	}

	/**
	 * Сбрасывает позицию кнопки предупреждения, если она вышла за пределы экрана.
	 */
	protected void resetWarningButtonPosition() {
		int buttonX = warningButton.getX();
		int buttonY = warningButton.getY();

		if (buttonX < 0 || buttonY < 0 || buttonX > width - BUTTON_MARGIN || buttonY > height - BUTTON_MARGIN) {
			warningButton.setX(Math.max(0, width - 40));
			warningButton.setY(Math.min(5, height));
		}
	}

	private ButtonWidget createWarningButton() {
		TexturedButtonWidget button = new TexturedButtonWidget(
				0,
				0,
				BUTTON_MARGIN,
				BUTTON_MARGIN,
				WARNING_BUTTON_TEXTURES,
				pressed -> client.setScreen(DialogScreen.WarningScreen.create(client, networkAccess, this)),
				Text.translatable("menu.custom_screen_info.button_narration")
		);
		button.setTooltip(Tooltip.of(Text.translatable("menu.custom_screen_info.tooltip")));
		return button;
	}

	@Override
	public boolean shouldPause() {
		return dialog.common().pause();
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return dialog.common().canCloseWithEscape();
	}

	@Override
	public void close() {
		runAction(cancelAction.get(), AfterAction.CLOSE);
	}

	public void runAction(Optional<ClickEvent> clickEvent) {
		runAction(clickEvent, dialog.common().afterAction());
	}

	/**
	 * Выполняет действие диалога и переключает экран согласно {@link AfterAction}.
	 *
	 * @param clickEvent событие клика, которое нужно обработать
	 * @param afterAction поведение после выполнения действия
	 */
	public void runAction(Optional<ClickEvent> clickEvent, AfterAction afterAction) {
		Screen nextScreen = switch (afterAction) {
			case NONE -> this;
			case CLOSE -> parent;
			case WAIT_FOR_RESPONSE -> new WaitingForResponseScreen(parent);
		};

		if (clickEvent.isPresent()) {
			handleClickEvent(clickEvent.get(), nextScreen);
		} else {
			client.setScreen(nextScreen);
		}
	}

	private void handleClickEvent(ClickEvent clickEvent, @Nullable Screen afterActionScreen) {
		switch (clickEvent) {
			case ClickEvent.RunCommand(String command):
				networkAccess.runClickEventCommand(CommandManager.stripLeadingSlash(command), afterActionScreen);
				break;
			case ClickEvent.ShowDialog showDialog:
				networkAccess.showDialog(showDialog.dialog(), afterActionScreen);
				break;
			case ClickEvent.Custom custom:
				networkAccess.sendCustomClickActionPacket(custom.id(), custom.payload());
				client.setScreen(afterActionScreen);
				break;
			default:
				handleBasicClickEvent(clickEvent, client, afterActionScreen);
		}
	}

	public @Nullable Screen getParentScreen() {
		return parent;
	}

	/**
	 * Размещает виджеты в сетку с заданным числом колонок.
	 * Остаток виджетов, не заполняющих полную строку, центрируется в отдельной строке.
	 *
	 * @param widgets список виджетов для размещения
	 * @param columns количество колонок в сетке
	 * @return виджет-сетка с размещёнными элементами
	 */
	protected static Widget createGridWidget(List<? extends Widget> widgets, int columns) {
		GridWidget gridWidget = new GridWidget();
		gridWidget.getMainPositioner().alignHorizontalCenter();
		gridWidget.setColumnSpacing(2).setRowSpacing(2);

		int totalCount = widgets.size();
		int fullRows = totalCount / columns;
		int fullRowsEnd = fullRows * columns;

		for (int index = 0; index < fullRowsEnd; index++) {
			gridWidget.add(widgets.get(index), index / columns, index % columns);
		}

		if (totalCount != fullRowsEnd) {
			DirectionalLayoutWidget remainderRow = DirectionalLayoutWidget.horizontal().spacing(2);
			remainderRow.getMainPositioner().alignHorizontalCenter();

			for (int index = fullRowsEnd; index < totalCount; index++) {
				remainderRow.add(widgets.get(index));
			}

			gridWidget.add(remainderRow, fullRows, 0, 1, columns);
		}

		return gridWidget;
	}

	/**
	 * Экран предупреждения о кастомном диалоге сервера.
	 * Позволяет отключиться или вернуться к диалогу.
	 */
	@Environment(EnvType.CLIENT)
	public static class WarningScreen extends ConfirmScreen {

		private final MutableObject<@Nullable Screen> dialogScreen;

		public static Screen create(
				MinecraftClient client,
				DialogNetworkAccess networkAccess,
				Screen dialogScreen
		) {
			return new DialogScreen.WarningScreen(client, networkAccess, new MutableObject<>(dialogScreen));
		}

		private WarningScreen(
				MinecraftClient client,
				DialogNetworkAccess networkAccess,
				MutableObject<Screen> dialogScreen
		) {
			super(
					disconnect -> {
						if (disconnect) {
							networkAccess.disconnect(DialogScreen.CUSTOM_SCREEN_REJECTED_DISCONNECT_TEXT);
						} else {
							client.setScreen(dialogScreen.get());
						}
					},
					Text.translatable("menu.custom_screen_info.title"),
					Text.translatable("menu.custom_screen_info.contents"),
					ScreenTexts.returnToMenuOrDisconnect(client.isInSingleplayer()),
					ScreenTexts.BACK
			);
			this.dialogScreen = dialogScreen;
		}

		public @Nullable Screen getDialogScreen() {
			return dialogScreen.get();
		}

		public void setDialogScreen(@Nullable Screen screen) {
			dialogScreen.setValue(screen);
		}
	}
}
