package net.minecraft.client.gui.screen.dialog;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.ItemStackWidget;
import net.minecraft.client.gui.widget.NarratedMultilineTextWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.dialog.body.DialogBody;
import net.minecraft.dialog.body.ItemDialogBody;
import net.minecraft.dialog.body.PlainMessageDialogBody;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Environment(EnvType.CLIENT)
/**
 * {@code DialogBodyHandlers}.
 */
public class DialogBodyHandlers {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Map<MapCodec<? extends DialogBody>, DialogBodyHandler<?>>
			DIALOG_BODY_HANDLERS =
			new HashMap<>();

	private static <B extends DialogBody> void register(
			MapCodec<B> dialogBodyCodec,
			DialogBodyHandler<? super B> dialogBodyHandler
	) {
		DIALOG_BODY_HANDLERS.put(dialogBodyCodec, dialogBodyHandler);
	}

	private static <B extends DialogBody> @Nullable DialogBodyHandler<B> getHandler(B dialogBody) {
		return (DialogBodyHandler<B>) DIALOG_BODY_HANDLERS.get(dialogBody.getTypeCodec());
	}

	/**
	 * Создаёт widget.
	 *
	 * @param dialogScreen dialog screen
	 * @param dialogBody dialog body
	 *
	 * @return @Nullable Widget — результат операции
	 */
	public static <B extends DialogBody> @Nullable Widget createWidget(DialogScreen<?> dialogScreen, B dialogBody) {
		DialogBodyHandler<B> dialogBodyHandler = getHandler(dialogBody);
		if (dialogBodyHandler == null) {
			LOGGER.warn("Unrecognized dialog body {}", dialogBody);
			return null;
		}
		else {
			return dialogBodyHandler.createWidget(dialogScreen, dialogBody);
		}
	}

	/**
	 * Bootstrap.
	 */
	public static void bootstrap() {
		register(PlainMessageDialogBody.CODEC, new DialogBodyHandlers.PlainMessageDialogBodyHandler());
		register(ItemDialogBody.CODEC, new DialogBodyHandlers.ItemDialogBodyHandler());
	}

	static void runActionFromStyle(DialogScreen<?> dialogScreen, @Nullable Style style) {
		if (style != null) {
			ClickEvent clickEvent = style.getClickEvent();
			if (clickEvent != null) {
				dialogScreen.runAction(Optional.of(clickEvent));
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ItemDialogBodyHandler}.
	 */
	static class ItemDialogBodyHandler implements DialogBodyHandler<ItemDialogBody> {

		/**
		 * Создаёт widget.
		 *
		 * @param dialogScreen dialog screen
		 * @param itemDialogBody item dialog body
		 *
		 * @return Widget — результат операции
		 */
		public Widget createWidget(DialogScreen<?> dialogScreen, ItemDialogBody itemDialogBody) {
			if (itemDialogBody.description().isPresent()) {
				PlainMessageDialogBody plainMessageDialogBody = itemDialogBody.description().get();
				DirectionalLayoutWidget directionalLayoutWidget = DirectionalLayoutWidget.horizontal().spacing(2);
				directionalLayoutWidget.getMainPositioner().alignVerticalCenter();
				ItemStackWidget itemStackWidget = new ItemStackWidget(
						MinecraftClient.getInstance(),
						0,
						0,
						itemDialogBody.width(),
						itemDialogBody.height(),
						ScreenTexts.EMPTY,
						itemDialogBody.item(),
						itemDialogBody.showDecorations(),
						itemDialogBody.showTooltip()
				);
				directionalLayoutWidget.add(itemStackWidget);
				directionalLayoutWidget.add(
						NarratedMultilineTextWidget
								.builder(plainMessageDialogBody.contents(), dialogScreen.getTextRenderer())
								.width(plainMessageDialogBody.width())
								.alwaysShowBorders(false)
								.backgroundRendering(NarratedMultilineTextWidget.BackgroundRendering.NEVER)
								.build()
								.onClick(style -> DialogBodyHandlers.runActionFromStyle(dialogScreen, style))
				);
				return directionalLayoutWidget;
			}
			else {
				return new ItemStackWidget(
						MinecraftClient.getInstance(),
						0,
						0,
						itemDialogBody.width(),
						itemDialogBody.height(),
						itemDialogBody.item().getName(),
						itemDialogBody.item(),
						itemDialogBody.showDecorations(),
						itemDialogBody.showTooltip()
				);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code PlainMessageDialogBodyHandler}.
	 */
	static class PlainMessageDialogBodyHandler implements DialogBodyHandler<PlainMessageDialogBody> {

		/**
		 * Создаёт widget.
		 *
		 * @param dialogScreen dialog screen
		 * @param plainMessageDialogBody plain message dialog body
		 *
		 * @return Widget — результат операции
		 */
		public Widget createWidget(DialogScreen<?> dialogScreen, PlainMessageDialogBody plainMessageDialogBody) {
			return NarratedMultilineTextWidget
					.builder(plainMessageDialogBody.contents(), dialogScreen.getTextRenderer())
					.width(plainMessageDialogBody.width())
					.alwaysShowBorders(false)
					.backgroundRendering(NarratedMultilineTextWidget.BackgroundRendering.NEVER)
					.build()
					.setCentered(true)
					.onClick(style -> DialogBodyHandlers.runActionFromStyle(dialogScreen, style));
		}
	}
}
