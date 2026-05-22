package net.minecraft.client.gui.screen.option;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

/**
 * Базовый класс для всех экранов настроек игры. Предоставляет стандартную трёхчастную
 * компоновку (заголовок / тело / подвал) и управление виджетом списка опций.
 */
@Environment(EnvType.CLIENT)
public abstract class GameOptionsScreen extends Screen {

	protected final Screen parent;
	protected final GameOptions gameOptions;
	protected @Nullable OptionListWidget body;
	public final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);

	public GameOptionsScreen(Screen parent, GameOptions gameOptions, Text title) {
		super(title);
		this.parent = parent;
		this.gameOptions = gameOptions;
	}

	@Override
	protected void init() {
		initHeader();
		initBody();
		initFooter();
		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	protected void initHeader() {
		layout.addHeader(title, textRenderer);
	}

	protected void initBody() {
		body = layout.addBody(new OptionListWidget(client, width, this));
		addOptions();
		if (body.getWidgetFor(gameOptions.getNarrator()) instanceof CyclingButtonWidget cyclingButtonWidget) {
			narratorToggleButton = cyclingButtonWidget;
			narratorToggleButton.active = client.getNarratorManager().isActive();
		}
	}

	protected abstract void addOptions();

	protected void initFooter() {
		layout.addFooter(ButtonWidget.builder(ScreenTexts.DONE, button -> close()).width(200).build());
	}

	@Override
	protected void refreshWidgetPositions() {
		layout.refreshPositions();
		if (body != null) {
			body.position(width, layout);
		}
	}

	@Override
	public void removed() {
		client.options.write();
	}

	@Override
	public void close() {
		if (body != null) {
			body.applyAllPendingValues();
		}

		client.setScreen(parent);
	}

	public void update(SimpleOption<?> option) {
		if (body != null) {
			body.update(option);
		}
	}
}
