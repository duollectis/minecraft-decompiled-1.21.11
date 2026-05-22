package net.minecraft.client.gui.screen;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ChangeGameModeC2SPacket;
import net.minecraft.server.command.GameModeCommand;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;

import java.util.List;

/**
 * Экран быстрого переключения режима игры (F3+F4).
 * Отображается поверх игры и закрывается при отпускании клавиши-модификатора.
 */
@Environment(EnvType.CLIENT)
public class GameModeSwitcherScreen extends Screen {

	static final Identifier SLOT_TEXTURE = Identifier.ofVanilla("gamemode_switcher/slot");
	static final Identifier SELECTION_TEXTURE = Identifier.ofVanilla("gamemode_switcher/selection");
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/gamemode_switcher.png");
	private static final int TEXTURE_WIDTH = 128;
	private static final int TEXTURE_HEIGHT = 128;
	private static final int BUTTON_SIZE = 26;
	private static final int BUTTON_STRIDE = 31;
	private static final int BUTTON_GAP = 5;
	private static final int ICON_OFFSET = 5;
	private static final int UI_WIDTH = GameModeSelection.values().length * BUTTON_STRIDE - BUTTON_GAP;
	private static final int LABEL_Y_OFFSET = 20;
	private static final int HINT_Y_OFFSET = 5;
	private static final int BG_X_OFFSET = 62;
	private static final int BG_Y_OFFSET = 27;
	private static final int BG_WIDTH = 125;
	private static final int BG_HEIGHT = 75;

	private final GameModeSelection currentGameMode;
	private GameModeSelection gameMode;
	private int lastMouseX;
	private int lastMouseY;
	private boolean mouseUsedForSelection;
	private final List<ButtonWidget> gameModeButtons = Lists.newArrayList();

	public GameModeSwitcherScreen() {
		super(NarratorManager.EMPTY);
		currentGameMode = GameModeSelection.of(getPreviousGameMode());
		gameMode = currentGameMode;
	}

	private GameMode getPreviousGameMode() {
		ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
		GameMode previous = interactionManager.getPreviousGameMode();
		return previous != null
			? previous
			: interactionManager.getCurrentGameMode() == GameMode.CREATIVE ? GameMode.SURVIVAL : GameMode.CREATIVE;
	}

	@Override
	protected void init() {
		super.init();
		gameModeButtons.clear();
		gameMode = currentGameMode;

		for (int index = 0; index < GameModeSelection.VALUES.length; index++) {
			GameModeSelection selection = GameModeSelection.VALUES[index];
			gameModeButtons.add(new ButtonWidget(
				selection,
				width / 2 - UI_WIDTH / 2 + index * BUTTON_STRIDE,
				height / 2 - BUTTON_STRIDE
			));
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		context.drawCenteredTextWithShadow(textRenderer, gameMode.text, width / 2, height / 2 - BUTTON_STRIDE - LABEL_Y_OFFSET, -1);

		MutableText hint = Text.translatable(
			"debug.gamemodes.select_next",
			client.options.debugSwitchGameModeKey.getBoundKeyLocalizedText().copy().formatted(Formatting.AQUA)
		);
		context.drawCenteredTextWithShadow(textRenderer, hint, width / 2, height / 2 + HINT_Y_OFFSET, -1);

		if (!mouseUsedForSelection) {
			lastMouseX = mouseX;
			lastMouseY = mouseY;
			mouseUsedForSelection = true;
		}

		boolean mouseSteady = lastMouseX == mouseX && lastMouseY == mouseY;

		for (ButtonWidget button : gameModeButtons) {
			button.render(context, mouseX, mouseY, deltaTicks);
			button.setSelected(gameMode == button.gameMode);

			if (!mouseSteady && button.isSelected()) {
				gameMode = button.gameMode;
			}
		}
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		int bgX = width / 2 - BG_X_OFFSET;
		int bgY = height / 2 - BUTTON_STRIDE - BG_Y_OFFSET;
		context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, bgX, bgY, 0.0F, 0.0F, BG_WIDTH, BG_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
	}

	private void apply() {
		apply(client, gameMode);
	}

	private static void apply(MinecraftClient client, GameModeSelection selection) {
		if (!client.canSwitchGameMode()) {
			return;
		}

		GameModeSelection current = GameModeSelection.of(client.interactionManager.getCurrentGameMode());

		if (selection != current && GameModeCommand.PERMISSION_CHECK.allows(client.player.getPermissions())) {
			client.player.networkHandler.sendPacket(new ChangeGameModeC2SPacket(selection.gameMode));
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (client.options.debugSwitchGameModeKey.matchesKey(input)) {
			mouseUsedForSelection = false;
			gameMode = gameMode.next();
			return true;
		}

		return super.keyPressed(input);
	}

	@Override
	public boolean keyReleased(KeyInput input) {
		if (client.options.debugModifierKey.matchesKey(input)) {
			apply();
			client.setScreen(null);
			return true;
		}

		return super.keyReleased(input);
	}

	@Override
	public boolean mouseReleased(Click click) {
		if (client.options.debugModifierKey.matchesMouse(click)) {
			apply();
			client.setScreen(null);
			return true;
		}

		return super.mouseReleased(click);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Environment(EnvType.CLIENT)
	public static class ButtonWidget extends ClickableWidget {

		final GameModeSelection gameMode;
		private boolean selected;

		public ButtonWidget(GameModeSelection gameMode, int x, int y) {
			super(x, y, BUTTON_SIZE, BUTTON_SIZE, gameMode.text);
			this.gameMode = gameMode;
		}

		@Override
		public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
			drawBackground(context);

			if (selected) {
				drawSelectionBox(context);
			}

			gameMode.renderIcon(context, getX() + ICON_OFFSET, getY() + ICON_OFFSET);
		}

		@Override
		public void appendClickableNarrations(NarrationMessageBuilder builder) {
			appendDefaultNarrations(builder);
		}

		@Override
		public boolean isSelected() {
			return super.isSelected() || selected;
		}

		public void setSelected(boolean selected) {
			this.selected = selected;
		}

		private void drawBackground(DrawContext context) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, GameModeSwitcherScreen.SLOT_TEXTURE, getX(), getY(), BUTTON_SIZE, BUTTON_SIZE);
		}

		private void drawSelectionBox(DrawContext context) {
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, GameModeSwitcherScreen.SELECTION_TEXTURE, getX(), getY(), BUTTON_SIZE, BUTTON_SIZE);
		}
	}

	@Environment(EnvType.CLIENT)
	enum GameModeSelection {
		CREATIVE(Text.translatable("gameMode.creative"), GameMode.CREATIVE, new ItemStack(Blocks.GRASS_BLOCK)),
		SURVIVAL(Text.translatable("gameMode.survival"), GameMode.SURVIVAL, new ItemStack(Items.IRON_SWORD)),
		ADVENTURE(Text.translatable("gameMode.adventure"), GameMode.ADVENTURE, new ItemStack(Items.MAP)),
		SPECTATOR(Text.translatable("gameMode.spectator"), GameMode.SPECTATOR, new ItemStack(Items.ENDER_EYE));

		static final GameModeSelection[] VALUES = values();
		private static final int ICON_SIZE = 16;

		final Text text;
		final GameMode gameMode;
		private final ItemStack icon;

		GameModeSelection(Text text, GameMode gameMode, ItemStack icon) {
			this.text = text;
			this.gameMode = gameMode;
			this.icon = icon;
		}

		void renderIcon(DrawContext context, int x, int y) {
			context.drawItem(icon, x, y);
		}

		GameModeSelection next() {
			return switch (this) {
				case CREATIVE -> SURVIVAL;
				case SURVIVAL -> ADVENTURE;
				case ADVENTURE -> SPECTATOR;
				case SPECTATOR -> CREATIVE;
			};
		}

		static GameModeSelection of(GameMode gameMode) {
			return switch (gameMode) {
				case SPECTATOR -> SPECTATOR;
				case SURVIVAL -> SURVIVAL;
				case CREATIVE -> CREATIVE;
				case ADVENTURE -> ADVENTURE;
			};
		}
	}
}
