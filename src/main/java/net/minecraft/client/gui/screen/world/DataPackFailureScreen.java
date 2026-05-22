package net.minecraft.client.gui.screen.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.Alignment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/**
 * Экран ошибки загрузки датапаков при открытии мира.
 * Предлагает запустить сервер в безопасном режиме или вернуться назад.
 */
@Environment(EnvType.CLIENT)
public class DataPackFailureScreen extends Screen {

	private static final int TEXT_Y = 70;
	private static final int LINE_HEIGHT = 9;

	private MultilineText wrappedText = MultilineText.EMPTY;
	private final Runnable goBack;
	private final Runnable runServerInSafeMode;

	public DataPackFailureScreen(Runnable goBack, Runnable runServerInSafeMode) {
		super(Text.translatable("datapackFailure.title"));
		this.goBack = goBack;
		this.runServerInSafeMode = runServerInSafeMode;
	}

	@Override
	protected void init() {
		super.init();
		wrappedText = MultilineText.create(textRenderer, getTitle(), width - 50);

		addDrawableChild(
				ButtonWidget
						.builder(
								Text.translatable("datapackFailure.safeMode"),
								button -> runServerInSafeMode.run()
						)
						.dimensions(width / 2 - 155, height / 6 + 96, 150, 20)
						.build()
		);
		addDrawableChild(
				ButtonWidget
						.builder(ScreenTexts.BACK, button -> goBack.run())
						.dimensions(width / 2 - 155 + 160, height / 6 + 96, 150, 20)
						.build()
		);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		DrawnTextConsumer textConsumer = context.getTextConsumer();
		wrappedText.draw(Alignment.CENTER, width / 2, TEXT_Y, LINE_HEIGHT, textConsumer);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}
}
