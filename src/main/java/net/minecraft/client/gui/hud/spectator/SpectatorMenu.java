package net.minecraft.client.gui.hud.spectator;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

import java.util.List;

/**
 * Спектаторское меню быстрых действий (телепорт, смена страницы, закрытие).
 * Отображается в виде 9 слотов хотбара при нажатии соответствующей клавиши.
 */
@Environment(EnvType.CLIENT)
public class SpectatorMenu {

	static final Identifier CLOSE_TEXTURE = Identifier.ofVanilla("spectator/close");
	static final Identifier SCROLL_LEFT_TEXTURE = Identifier.ofVanilla("spectator/scroll_left");
	static final Identifier SCROLL_RIGHT_TEXTURE = Identifier.ofVanilla("spectator/scroll_right");

	private static final SpectatorMenuCommand CLOSE_COMMAND = new CloseSpectatorMenuCommand();
	private static final SpectatorMenuCommand PREVIOUS_PAGE_COMMAND = new ChangePageSpectatorMenuCommand(-1, true);
	private static final SpectatorMenuCommand NEXT_PAGE_COMMAND = new ChangePageSpectatorMenuCommand(1, true);
	private static final SpectatorMenuCommand DISABLED_NEXT_PAGE_COMMAND = new ChangePageSpectatorMenuCommand(1, false);

	private static final int CLOSE_SLOT = 8;
	private static final int NEXT_PAGE_SLOT = 7;
	private static final int COMMANDS_PER_PAGE = 6;

	static final Text CLOSE_TEXT = Text.translatable("spectatorMenu.close");
	static final Text PREVIOUS_PAGE_TEXT = Text.translatable("spectatorMenu.previous_page");
	static final Text NEXT_PAGE_TEXT = Text.translatable("spectatorMenu.next_page");

	public static final SpectatorMenuCommand BLANK_COMMAND = new SpectatorMenuCommand() {
		@Override
		public void use(SpectatorMenu menu) {
		}

		@Override
		public Text getName() {
			return ScreenTexts.EMPTY;
		}

		@Override
		public void renderIcon(DrawContext context, float brightness, float alpha) {
		}

		@Override
		public boolean isEnabled() {
			return false;
		}
	};

	private final SpectatorMenuCloseCallback closeCallback;
	private SpectatorMenuCommandGroup currentGroup;
	private int selectedSlot = -1;
	int page;

	public SpectatorMenu(SpectatorMenuCloseCallback closeCallback) {
		currentGroup = new RootSpectatorCommandGroup();
		this.closeCallback = closeCallback;
	}

	public SpectatorMenuCommand getCommand(int slot) {
		int absoluteIndex = slot + page * COMMANDS_PER_PAGE;

		if (page > 0 && slot == 0) {
			return PREVIOUS_PAGE_COMMAND;
		}

		if (slot == NEXT_PAGE_SLOT) {
			return absoluteIndex < currentGroup.getCommands().size() ? NEXT_PAGE_COMMAND : DISABLED_NEXT_PAGE_COMMAND;
		}

		if (slot == CLOSE_SLOT) {
			return CLOSE_COMMAND;
		}

		return absoluteIndex >= 0 && absoluteIndex < currentGroup.getCommands().size()
			? (SpectatorMenuCommand) MoreObjects.firstNonNull(currentGroup.getCommands().get(absoluteIndex), BLANK_COMMAND)
			: BLANK_COMMAND;
	}

	public List<SpectatorMenuCommand> getCommands() {
		List<SpectatorMenuCommand> commands = Lists.newArrayList();

		for (int slot = 0; slot <= CLOSE_SLOT; slot++) {
			commands.add(getCommand(slot));
		}

		return commands;
	}

	public SpectatorMenuCommand getSelectedCommand() {
		return getCommand(selectedSlot);
	}

	public SpectatorMenuCommandGroup getCurrentGroup() {
		return currentGroup;
	}

	public void useCommand(int slot) {
		SpectatorMenuCommand command = getCommand(slot);

		if (command == BLANK_COMMAND) {
			return;
		}

		if (selectedSlot == slot && command.isEnabled()) {
			command.use(this);
		} else {
			selectedSlot = slot;
		}
	}

	public void close() {
		closeCallback.close(this);
	}

	public int getSelectedSlot() {
		return selectedSlot;
	}

	public void selectElement(SpectatorMenuCommandGroup group) {
		currentGroup = group;
		selectedSlot = -1;
		page = 0;
	}

	public SpectatorMenuState getCurrentState() {
		return new SpectatorMenuState(getCommands(), selectedSlot);
	}

	@Environment(EnvType.CLIENT)
	static class ChangePageSpectatorMenuCommand implements SpectatorMenuCommand {

		private final int direction;
		private final boolean enabled;

		ChangePageSpectatorMenuCommand(int direction, boolean enabled) {
			this.direction = direction;
			this.enabled = enabled;
		}

		@Override
		public void use(SpectatorMenu menu) {
			menu.page += direction;
		}

		@Override
		public Text getName() {
			return direction < 0 ? SpectatorMenu.PREVIOUS_PAGE_TEXT : SpectatorMenu.NEXT_PAGE_TEXT;
		}

		@Override
		public void renderIcon(DrawContext context, float brightness, float alpha) {
			int color = ColorHelper.fromFloats(alpha, brightness, brightness, brightness);
			Identifier texture = direction < 0 ? SpectatorMenu.SCROLL_LEFT_TEXTURE : SpectatorMenu.SCROLL_RIGHT_TEXTURE;
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 16, 16, color);
		}

		@Override
		public boolean isEnabled() {
			return enabled;
		}
	}

	@Environment(EnvType.CLIENT)
	static class CloseSpectatorMenuCommand implements SpectatorMenuCommand {

		@Override
		public void use(SpectatorMenu menu) {
			menu.close();
		}

		@Override
		public Text getName() {
			return SpectatorMenu.CLOSE_TEXT;
		}

		@Override
		public void renderIcon(DrawContext context, float brightness, float alpha) {
			context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				SpectatorMenu.CLOSE_TEXTURE,
				0,
				0,
				16,
				16,
				ColorHelper.fromFloats(alpha, brightness, brightness, brightness)
			);
		}

		@Override
		public boolean isEnabled() {
			return true;
		}
	}
}
