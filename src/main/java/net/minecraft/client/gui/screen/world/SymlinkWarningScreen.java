package net.minecraft.client.gui.screen.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Urls;
import net.minecraft.util.Util;

import java.net.URI;

/**
 * Экран предупреждения о символических ссылках в директории мира или пакета ресурсов.
 * Информирует пользователя о потенциальных рисках безопасности и предлагает ссылку на документацию.
 */
@Environment(EnvType.CLIENT)
public class SymlinkWarningScreen extends Screen {

	private static final int BUTTON_WIDTH = 120;
	private static final Text WORLD_TITLE = Text.translatable("symlink_warning.title.world").formatted(Formatting.BOLD);
	private static final Text WORLD_MESSAGE = Text.translatable("symlink_warning.message.world", Text.of(Urls.MINECRAFT_SYMLINKS));
	private static final Text PACK_TITLE = Text.translatable("symlink_warning.title.pack").formatted(Formatting.BOLD);
	private static final Text PACK_MESSAGE = Text.translatable("symlink_warning.message.pack", Text.of(Urls.MINECRAFT_SYMLINKS));

	private final Text message;
	private final URI link;
	private final Runnable onClose;
	private final GridWidget grid = new GridWidget().setRowSpacing(10);

	public SymlinkWarningScreen(Text title, Text message, URI link, Runnable onClose) {
		super(title);
		this.message = message;
		this.link = link;
		this.onClose = onClose;
	}

	public static Screen world(Runnable onClose) {
		return new SymlinkWarningScreen(WORLD_TITLE, WORLD_MESSAGE, Urls.MINECRAFT_SYMLINKS, onClose);
	}

	public static Screen pack(Runnable onClose) {
		return new SymlinkWarningScreen(PACK_TITLE, PACK_MESSAGE, Urls.MINECRAFT_SYMLINKS, onClose);
	}

	@Override
	protected void init() {
		super.init();
		grid.getMainPositioner().alignHorizontalCenter();

		GridWidget.Adder adder = grid.createAdder(1);
		adder.add(new TextWidget(title, textRenderer));
		adder.add(new MultilineTextWidget(message, textRenderer)
				.setMaxWidth(width - 50)
				.setCentered(true));

		GridWidget buttonGrid = new GridWidget().setColumnSpacing(5);
		GridWidget.Adder buttonAdder = buttonGrid.createAdder(3);
		buttonAdder.add(ButtonWidget
				.builder(ScreenTexts.OPEN_LINK, button -> Util.getOperatingSystem().open(link))
				.size(BUTTON_WIDTH, 20)
				.build());
		buttonAdder.add(
				ButtonWidget
						.builder(
								ScreenTexts.COPY_LINK_TO_CLIPBOARD,
								button -> client.keyboard.setClipboard(link.toString())
						)
						.size(BUTTON_WIDTH, 20)
						.build()
		);
		buttonAdder.add(ButtonWidget.builder(ScreenTexts.BACK, button -> close()).size(BUTTON_WIDTH, 20).build());
		adder.add(buttonGrid);

		refreshWidgetPositions();
		grid.forEachChild(this::addDrawableChild);
	}

	@Override
	protected void refreshWidgetPositions() {
		grid.refreshPositions();
		SimplePositioningWidget.setPos(grid, getNavigationFocus());
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinSentences(super.getNarratedTitle(), message);
	}

	@Override
	public void close() {
		onClose.run();
	}
}
