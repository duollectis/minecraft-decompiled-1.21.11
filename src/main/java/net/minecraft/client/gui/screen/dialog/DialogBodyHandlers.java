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

/**
 * Реестр обработчиков тел диалогов. Каждый обработчик создаёт виджет для конкретного типа {@link DialogBody}.
 */
@Environment(EnvType.CLIENT)
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
		DialogBodyHandler<B> handler = getHandler(dialogBody);

		if (handler == null) {
			LOGGER.warn("Unrecognized dialog body {}", dialogBody);
			return null;
		}

		return handler.createWidget(dialogScreen, dialogBody);
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

	/**
	 * Обработчик тела диалога с предметом инвентаря.
	 * Если есть описание — отображает предмет рядом с текстом, иначе только предмет.
	 */
	@Environment(EnvType.CLIENT)
	static class ItemDialogBodyHandler implements DialogBodyHandler<ItemDialogBody> {

		public Widget createWidget(DialogScreen<?> dialogScreen, ItemDialogBody body) {
			if (body.description().isEmpty()) {
				return new ItemStackWidget(
						MinecraftClient.getInstance(),
						0,
						0,
						body.width(),
						body.height(),
						body.item().getName(),
						body.item(),
						body.showDecorations(),
						body.showTooltip()
				);
			}

			PlainMessageDialogBody description = body.description().get();
			DirectionalLayoutWidget row = DirectionalLayoutWidget.horizontal().spacing(2);
			row.getMainPositioner().alignVerticalCenter();
			row.add(new ItemStackWidget(
					MinecraftClient.getInstance(),
					0,
					0,
					body.width(),
					body.height(),
					ScreenTexts.EMPTY,
					body.item(),
					body.showDecorations(),
					body.showTooltip()
			));
			row.add(
					NarratedMultilineTextWidget
							.builder(description.contents(), dialogScreen.getTextRenderer())
							.width(description.width())
							.alwaysShowBorders(false)
							.backgroundRendering(NarratedMultilineTextWidget.BackgroundRendering.NEVER)
							.build()
							.onClick(style -> DialogBodyHandlers.runActionFromStyle(dialogScreen, style))
			);
			return row;
		}
	}

	/**
	 * Обработчик тела диалога с простым текстовым сообщением.
	 */
	@Environment(EnvType.CLIENT)
	static class PlainMessageDialogBodyHandler implements DialogBodyHandler<PlainMessageDialogBody> {

		public Widget createWidget(DialogScreen<?> dialogScreen, PlainMessageDialogBody body) {
			return NarratedMultilineTextWidget
					.builder(body.contents(), dialogScreen.getTextRenderer())
					.width(body.width())
					.alwaysShowBorders(false)
					.backgroundRendering(NarratedMultilineTextWidget.BackgroundRendering.NEVER)
					.build()
					.setCentered(true)
					.onClick(style -> DialogBodyHandlers.runActionFromStyle(dialogScreen, style));
		}
	}
}
