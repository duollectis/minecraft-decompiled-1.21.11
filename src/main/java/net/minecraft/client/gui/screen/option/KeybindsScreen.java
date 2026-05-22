package net.minecraft.client.gui.screen.option;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * Экран привязки клавиш — позволяет переназначать клавиши управления
 * и сбрасывать все привязки к значениям по умолчанию.
 */
@Environment(EnvType.CLIENT)
public class KeybindsScreen extends GameOptionsScreen {

	private static final Text TITLE_TEXT = Text.translatable("controls.keybinds.title");

	public @Nullable KeyBinding selectedKeyBinding;
	public long lastKeyCodeUpdateTime;
	private ControlsListWidget controlsList;
	private ButtonWidget resetAllButton;

	public KeybindsScreen(Screen parent, GameOptions gameOptions) {
		super(parent, gameOptions, TITLE_TEXT);
	}

	@Override
	protected void initBody() {
		controlsList = layout.addBody(new ControlsListWidget(this, client));
	}

	@Override
	protected void addOptions() {
	}

	@Override
	protected void initFooter() {
		resetAllButton = ButtonWidget.builder(
				Text.translatable("controls.resetAll"), button -> {
					for (KeyBinding keyBinding : gameOptions.allKeys) {
						keyBinding.setBoundKey(keyBinding.getDefaultKey());
					}

					controlsList.update();
				}
		).build();
		DirectionalLayoutWidget footerLayout = layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
		footerLayout.add(resetAllButton);
		footerLayout.add(ButtonWidget.builder(ScreenTexts.DONE, button -> close()).build());
	}

	@Override
	protected void refreshWidgetPositions() {
		layout.refreshPositions();
		controlsList.position(width, layout);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (selectedKeyBinding == null) {
			return super.mouseClicked(click, doubled);
		}

		selectedKeyBinding.setBoundKey(InputUtil.Type.MOUSE.createFromCode(click.button()));
		selectedKeyBinding = null;
		controlsList.update();
		return true;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (selectedKeyBinding == null) {
			return super.keyPressed(input);
		}

		if (input.isEscape()) {
			selectedKeyBinding.setBoundKey(InputUtil.UNKNOWN_KEY);
		} else {
			selectedKeyBinding.setBoundKey(InputUtil.fromKeyCode(input));
		}

		selectedKeyBinding = null;
		lastKeyCodeUpdateTime = Util.getMeasuringTimeMs();
		controlsList.update();
		return true;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		boolean hasNonDefault = false;

		for (KeyBinding keyBinding : gameOptions.allKeys) {
			if (!keyBinding.isDefault()) {
				hasNonDefault = true;
				break;
			}
		}

		resetAllButton.active = hasNonDefault;
	}
}
